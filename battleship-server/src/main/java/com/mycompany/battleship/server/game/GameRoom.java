package com.mycompany.battleship.server.game;

import com.mycompany.battleship.common.model.ChatMessage;
import com.mycompany.battleship.common.model.Emote;
import com.mycompany.battleship.common.model.EmoteEvent;
import com.mycompany.battleship.common.model.GameConfig;
import com.mycompany.battleship.common.model.GameException;
import com.mycompany.battleship.common.model.GamePhase;
import com.mycompany.battleship.common.model.GameResult;
import com.mycompany.battleship.common.model.GameView;
import com.mycompany.battleship.common.model.MissileType;
import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.Ship;
import com.mycompany.battleship.common.model.ShotResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Phòng chơi + trọng tài của một trận giữa 2 người. Server là nguồn sự thật duy nhất:
 * client chỉ gửi "bắn loại đạn X vào ô Y", mọi kiểm tra và tính toán đều làm ở đây.
 *
 * Vòng đời: CREATED -> COUNTDOWN (3-2-1) -> PLAYING -> FINISHED -> (Tái đấu: COUNTDOWN ...) -> CLOSED.
 * Mọi hàm public đều thread-safe (synchronized); bộ đếm 45s chạy trên ScheduledExecutorService dùng chung.
 */
public class GameRoom {
    private static final Logger LOG = Logger.getLogger(GameRoom.class.getName());

    private final String roomId;
    private final PlayerState playerA;   // người mời: đi trước
    private final PlayerState playerB;
    private final GameConfig config;
    private final ScheduledExecutorService scheduler;
    private final GameEventListener listener;
    private final Random rng = new Random();
    private final Set<String> rematchRequests = new HashSet<>();
    private final ChatLog chat;

    private GamePhase phase = GamePhase.CREATED;
    private long epoch;                       // tăng mỗi khi đổi lượt/giai đoạn -> vô hiệu hoá timer cũ
    private ScheduledFuture<?> pendingTask;
    private String currentTurnId;
    private long turnDeadlineMillis;
    private long matchStartMillis;
    private Runnable onClosed = () -> { };

    public GameRoom(String roomId, String inviterId, String inviteeId, GameConfig config,
                    ScheduledExecutorService scheduler, GameEventListener listener) {
        if (inviterId == null || inviteeId == null || inviterId.equals(inviteeId)) {
            throw new IllegalArgumentException("Cần 2 người chơi khác nhau");
        }
        this.roomId = roomId;
        this.config = config;
        this.scheduler = scheduler;
        this.listener = listener;
        this.playerA = new PlayerState(inviterId, config);
        this.playerB = new PlayerState(inviteeId, config);
        this.chat = new ChatLog(config);
    }

    // ------------------------------------------------------------------ getters

    public String getRoomId() { return roomId; }
    public String getInviterId() { return playerA.id(); }
    public String getInviteeId() { return playerB.id(); }
    public synchronized GamePhase getPhase() { return phase; }
    public synchronized String getCurrentTurnPlayerId() { return currentTurnId; }
    public synchronized boolean isMember(String playerId) {
        return playerA.id().equals(playerId) || playerB.id().equals(playerId);
    }

    /** GameManager đăng ký để gỡ phòng khỏi danh sách khi phòng đóng. */
    void setOnClosed(Runnable r) { this.onClosed = r; }

    // ------------------------------------------------------------------ vòng đời

    /** Sinh bàn, chạy đếm ngược 3-2-1, sau đó người mời đi trước. Gọi khi B đã đồng ý lời thách đấu. */
    public synchronized void start() {
        if (phase != GamePhase.CREATED) throw new GameException(GameException.Code.INVALID_STATE, "Phòng đã bắt đầu");
        beginCountdown();
    }

    /** Người chơi bấm "Tái đấu". Khi cả hai đã bấm thì bắt đầu ván mới. */
    public synchronized void requestRematch(String playerId) {
        stateOf(playerId);
        if (phase == GamePhase.CLOSED) throw new GameException(GameException.Code.ROOM_CLOSED, "Đối thủ đã rời phòng");
        if (phase != GamePhase.FINISHED) throw new GameException(GameException.Code.INVALID_STATE, "Trận chưa kết thúc");
        if (!rematchRequests.add(playerId)) return;
        if (rematchRequests.size() == 2) {
            beginCountdown();
        } else {
            notifyListener(l -> l.onRematchRequested(this, playerId));
        }
    }

    /**
     * Rời phòng: bấm "Về sảnh" hoặc mất kết nối. Đang đánh => bị xử thua; sau khi rời phòng đóng
     * (không thể tái đấu). Gọi nhiều lần / sau khi đã đóng thì bỏ qua.
     */
    public synchronized void leave(String playerId) {
        if (phase == GamePhase.CLOSED || !isMember(playerId)) return;
        PlayerState leaver = stateOf(playerId);
        if (phase == GamePhase.PLAYING) {
            finish(opponentOf(leaver).id(), leaver.id(), GameResult.Reason.LEFT);
        }
        notifyListener(l -> l.onPlayerLeft(this, playerId));
        close();
    }

    // ------------------------------------------------------------------ bắn

    /**
     * Xử lý một lượt bắn. Ném {@link GameException} nếu nước đi không hợp lệ (sai lượt, ô đã bắn, hết đạn...).
     *
     * @param target ô được chọn; bỏ qua (có thể null) với đạn rải rác
     */
    public synchronized ShotResult fire(String playerId, MissileType type, Point target) {
        if (phase == GamePhase.CLOSED) throw new GameException(GameException.Code.ROOM_CLOSED, "Phòng đã đóng");
        if (phase != GamePhase.PLAYING) throw new GameException(GameException.Code.INVALID_STATE, "Trận không diễn ra");
        PlayerState shooter = stateOf(playerId);
        if (!playerId.equals(currentTurnId)) throw new GameException(GameException.Code.NOT_YOUR_TURN, "Chưa tới lượt");
        if (type == null) throw new GameException(GameException.Code.INVALID_MISSILE, "Thiếu loại đạn");
        if (!shooter.has(type)) throw new GameException(GameException.Code.MISSILE_UNAVAILABLE, "Hết đạn " + type);

        PlayerState victim = opponentOf(shooter);
        Board board = victim.board();
        if (type.needsTarget()) {
            if (target == null || !Board.inBounds(target)) {
                throw new GameException(GameException.Code.OUT_OF_BOUNDS, "Toạ độ không hợp lệ");
            }
            if (board.isMarked(target)) {
                throw new GameException(GameException.Code.CELL_ALREADY_MARKED, "Ô " + target + " đã bắn rồi");
            }
        } else {
            target = null;
        }

        cancelPending();
        shooter.consume(type);
        ShotResult result = new ShotResult(shooter.id(), victim.id(), type, target);

        List<Point> area = (type == MissileType.RAIN) ? rainCells(board) : blastCells(type, target);
        for (Point p : area) {
            if (board.isMarked(p)) continue;
            ShotResult.CellResult cell = board.mark(p);
            result.addCell(cell);
            if (cell.isMysteryCollected()) grantReward(shooter, result);
        }

        boolean directHit = target != null && board.hasShip(target);
        result.setDirectHit(directHit);

        shooter.incrementShots();
        if (shooter.shotsFired() % config.getMysteryEveryNShots() == 0) {
            result.setNewMysteryBox(board.spawnMystery(rng));
        }
        result.setShooterInventory(shooter.inventorySnapshot());

        if (board.allShipsRevealed()) {
            result.setGameOver(true);
            notifyListener(l -> l.onShotResolved(this, result));
            finish(shooter.id(), victim.id(), GameResult.Reason.ALL_SHIPS_SUNK);
        } else {
            String next = directHit ? shooter.id() : victim.id();
            result.setNextTurnPlayerId(next);
            notifyListener(l -> l.onShotResolved(this, result));
            startTurn(next);
        }
        return result;
    }

    // ------------------------------------------------------------------ chat & emote

    public synchronized ChatMessage sendChat(String playerId, String text) {
        requireChatOpen(playerId);
        ChatMessage msg = chat.addMessage(roomId, playerId, text);
        notifyListener(l -> l.onChatMessage(this, msg));
        return msg;
    }

    public synchronized EmoteEvent sendEmote(String playerId, Emote emote) {
        requireChatOpen(playerId);
        EmoteEvent event = chat.addEmote(roomId, playerId, emote);
        notifyListener(l -> l.onEmote(this, event));
        return event;
    }

    public synchronized List<ChatMessage> getChatHistory() { return chat.history(); }

    private void requireChatOpen(String playerId) {
        stateOf(playerId);
        if (phase == GamePhase.CLOSED) throw new GameException(GameException.Code.ROOM_CLOSED, "Phòng đã đóng");
        if (phase == GamePhase.CREATED) throw new GameException(GameException.Code.INVALID_STATE, "Phòng chưa bắt đầu");
    }

    // ------------------------------------------------------------------ ảnh chụp trạng thái

    public synchronized GameView viewFor(String playerId) {
        PlayerState me = stateOf(playerId);
        PlayerState opp = opponentOf(me);
        if (me.board() == null) throw new GameException(GameException.Code.INVALID_STATE, "Trận chưa được khởi tạo");

        Board mine = me.board();
        Board theirs = opp.board();
        char[][] own = new char[Board.SIZE][Board.SIZE];
        char[][] enemy = new char[Board.SIZE][Board.SIZE];
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Point p = new Point(r, c);
                if (mine.hasShip(p)) own[r][c] = mine.isMarked(p) ? 'X' : 'S';
                else own[r][c] = mine.isMarked(p) ? 'o' : '.';

                if (theirs.isMarked(p)) enemy[r][c] = theirs.hasShip(p) ? 'X' : 'o';
                else enemy[r][c] = theirs.hasMystery(p) ? '?' : '.';
            }
        }
        List<List<Point>> sunk = new ArrayList<>();
        for (Ship s : theirs.getShips()) if (s.isSunk()) sunk.add(s.getCells());

        int secondsLeft = 0;
        if (phase == GamePhase.PLAYING) {
            secondsLeft = (int) Math.max(0, (turnDeadlineMillis - System.currentTimeMillis() + 999) / 1000);
        }
        return new GameView(phase, me.id(), opp.id(), currentTurnId, secondsLeft,
                turnDeadlineMillis, own, enemy, sunk, me.inventorySnapshot());
    }

    // ------------------------------------------------------------------ nội bộ: đếm ngược & lượt

    private void beginCountdown() {
        rematchRequests.clear();
        playerA.resetForNewMatch(BoardGenerator.generate(config.getShipLengths(), rng));
        playerB.resetForNewMatch(BoardGenerator.generate(config.getShipLengths(), rng));
        currentTurnId = null;
        phase = GamePhase.COUNTDOWN;
        notifyListener(l -> l.onMatchPrepared(this));
        countdownStep(config.getCountdownSeconds(), ++epoch);
    }

    private void countdownStep(int remaining, long myEpoch) {
        if (remaining <= 0) {
            beginPlaying();
            return;
        }
        notifyListener(l -> l.onCountdown(this, remaining));
        schedule(() -> {
            synchronized (GameRoom.this) {
                if (epoch == myEpoch && phase == GamePhase.COUNTDOWN) countdownStep(remaining - 1, myEpoch);
            }
        }, 1000);
    }

    private void beginPlaying() {
        phase = GamePhase.PLAYING;
        matchStartMillis = System.currentTimeMillis();
        startTurn(playerA.id());
    }

    private void startTurn(String playerId) {
        cancelPending();
        currentTurnId = playerId;
        long myEpoch = ++epoch;
        long millis = config.getTurnSeconds() * 1000L;
        turnDeadlineMillis = System.currentTimeMillis() + millis;
        schedule(() -> onTurnTimeout(myEpoch), millis);
        long deadline = turnDeadlineMillis;
        notifyListener(l -> l.onTurnStarted(this, playerId, config.getTurnSeconds(), deadline));
    }

    private synchronized void onTurnTimeout(long myEpoch) {
        if (phase != GamePhase.PLAYING || epoch != myEpoch) return;
        String loser = currentTurnId;
        finish(opponentOf(stateOf(loser)).id(), loser, GameResult.Reason.TIMEOUT);
    }

    private void finish(String winnerId, String loserId, GameResult.Reason reason) {
        cancelPending();
        epoch++;
        phase = GamePhase.FINISHED;
        currentTurnId = null;
        GameResult result = new GameResult(roomId, winnerId, loserId, reason, matchStartMillis, System.currentTimeMillis());
        notifyListener(l -> l.onGameOver(this, result));
    }

    private void close() {
        if (phase == GamePhase.CLOSED) return;
        cancelPending();
        epoch++;
        phase = GamePhase.CLOSED;
        notifyListener(l -> l.onRoomClosed(this));
        try {
            onClosed.run();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "onClosed lỗi", ex);
        }
    }

    // ------------------------------------------------------------------ nội bộ: đạn & thưởng

    private List<Point> blastCells(MissileType type, Point center) {
        List<Point> cells = new ArrayList<>();
        for (int[] d : type.shape()) {
            int r = center.getRow() + d[0], c = center.getCol() + d[1];
            if (Board.inBounds(r, c)) cells.add(new Point(r, c));
        }
        return cells;
    }

    private List<Point> rainCells(Board board) {
        List<Point> free = board.unmarkedCells();
        Collections.shuffle(free, rng);
        int count = config.getRainMin() + rng.nextInt(config.getRainMax() - config.getRainMin() + 1);
        return new ArrayList<>(free.subList(0, Math.min(count, free.size())));
    }

    private void grantReward(PlayerState shooter, ShotResult result) {
        if (!shooter.hasBonusSpace()) {
            result.incrementRewardsLost();
            return;
        }
        List<MissileType> pool = new ArrayList<>(Arrays.asList(MissileType.BIG, MissileType.NUCLEAR, MissileType.RAIN));
        if (shooter.nuclearReceived() >= config.getMaxNuclearPerMatch()) pool.remove(MissileType.NUCLEAR);
        MissileType reward = pool.get(rng.nextInt(pool.size()));
        shooter.addBonus(reward);
        result.addReward(reward);
    }

    // ------------------------------------------------------------------ nội bộ: tiện ích

    private PlayerState stateOf(String playerId) {
        if (playerA.id().equals(playerId)) return playerA;
        if (playerB.id().equals(playerId)) return playerB;
        throw new GameException(GameException.Code.NOT_IN_ROOM, "Người chơi không thuộc phòng " + roomId);
    }

    private PlayerState opponentOf(PlayerState p) { return p == playerA ? playerB : playerA; }

    private void schedule(Runnable task, long delayMillis) {
        pendingTask = scheduler.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelPending() {
        if (pendingTask != null) {
            pendingTask.cancel(false);
            pendingTask = null;
        }
    }

    private void notifyListener(Consumer<GameEventListener> action) {
        try {
            action.accept(listener);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "GameEventListener ném lỗi", ex);
        }
    }

    PlayerState playerState(String playerId) { return stateOf(playerId); }
}