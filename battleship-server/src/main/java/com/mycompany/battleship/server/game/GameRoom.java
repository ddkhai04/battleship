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
import com.mycompany.battleship.server.ClientHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameRoom {
    private static final Logger LOG = Logger.getLogger(GameRoom.class.getName());

    private final String roomId;
    private final PlayerState playerA;
    private final PlayerState playerB;
    private final GameConfig config;
    private final ScheduledExecutorService scheduler;
    private final GameEventListener listener;
    private final Random rng = new Random();
    private final Set<String> rematchRequests = new HashSet<>();
    private final ChatLog chat;
    private GameResult lastResult;

    private GamePhase phase = GamePhase.CREATED;
    private long epoch;
    private ScheduledFuture<?> pendingTask;
    private String currentTurnId;
    private long turnDeadlineMillis;
    private long matchStartMillis;
    private Runnable onClosed = () -> { };

    private int dbMatchId = -1;
    private int playerAId = -1;
    private int playerBId = -1;
    private int turnCounter = 0;
    private final java.util.List<com.mycompany.battleship.server.ClientHandler> spectators = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public GameRoom(String roomId, int dbMatchId, int playerAId, String inviterId, int playerBId, String inviteeId,
                    GameConfig config, ScheduledExecutorService scheduler, GameEventListener listener) {
        this.roomId = roomId;
        this.dbMatchId = dbMatchId;
        this.playerAId = playerAId;
        this.playerBId = playerBId;
        this.config = config;
        this.scheduler = scheduler;
        this.listener = listener;
        this.playerA = new PlayerState(inviterId, config);
        this.playerB = new PlayerState(inviteeId, config);
        this.chat = new ChatLog(config);
    }

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

    public void addSpectator(com.mycompany.battleship.server.ClientHandler adminHandler) {
        spectators.add(adminHandler);
        broadcastGodViewToSpectators();
    }

    public void removeSpectator(com.mycompany.battleship.server.ClientHandler adminHandler) {
        spectators.remove(adminHandler);
    }

    public void broadcastGodViewToSpectators() {
        if (spectators.isEmpty()) return;
        Board b1 = playerA.board();
        Board b2 = playerB.board();

        char[][] grid1 = new char[10][10];
        char[][] grid2 = new char[10][10];

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Point p = new Point(r, c);
                if (b1 != null && b1.hasShip(p)) grid1[r][c] = b1.isMarked(p) ? 'X' : 'S';
                else grid1[r][c] = (b1 != null && b1.isMarked(p)) ? 'o' : '.';

                if (b2 != null && b2.hasShip(p)) grid2[r][c] = b2.isMarked(p) ? 'X' : 'S';
                else grid2[r][c] = (b2 != null && b2.isMarked(p)) ? 'o' : '.';
            }
        }

        Map<String, Object> godData = new HashMap<>();
        godData.put("roomId", roomId);
        godData.put("player1", playerA.id());
        godData.put("player2", playerB.id());
        godData.put("currentTurn", currentTurnId);
        godData.put("phase", phase.name());
        godData.put("grid1", grid1);
        godData.put("grid2", grid2);

        if (lastResult != null && phase == GamePhase.FINISHED) {
            godData.put("winner", lastResult.getWinnerId());
            godData.put("loser", lastResult.getLoserId());
            godData.put("reason", lastResult.getReason().name());
        }
        com.mycompany.battleship.common.model.MessageDTO msg =
            new com.mycompany.battleship.common.model.MessageDTO("SPECTATE_UPDATE", godData);
        for (ClientHandler sp : spectators) {
            sp.sendMessage(msg);
        }
    }

    public String getRoomId() { return roomId; }
    public String getInviterId() { return playerA.id(); }
    public String getInviteeId() { return playerB.id(); }
    public synchronized GamePhase getPhase() { return phase; }
    public synchronized String getCurrentTurnPlayerId() { return currentTurnId; }
    public synchronized boolean isMember(String playerId) {
        return playerA.id().equals(playerId) || playerB.id().equals(playerId);
    }

    void setOnClosed(Runnable r) { this.onClosed = r; }

    /** Mở giai đoạn đặt tàu 45s. */
    public synchronized void start() {
        if (phase != GamePhase.CREATED) throw new GameException(GameException.Code.INVALID_STATE, "Phòng đã bắt đầu");
        beginSetup();
    }

    /**
     * Nhận vị trí tàu từ Client trong giai đoạn SETUP (hỗ trợ cả khi mới đặt 1..4 tàu để Admin xem real-time).
     * @param shipLists danh sách các tàu đang đặt trên bàn; nếu rỗng/null thì bàn trống.
     * @param ready true nếu người chơi bấm nút "Sẵn sàng" khóa bàn cờ.
     */
    public synchronized void placeShips(String playerId, List<List<Point>> shipLists, boolean ready) {
        if (phase != GamePhase.SETUP) return;
        PlayerState ps = stateOf(playerId);
        if (ps.isReady()) return; // Đã khóa bàn thì không sửa nữa

        if (shipLists != null && !shipLists.isEmpty()) {
            // Nếu người chơi đã đặt đủ 5 tàu, kiểm tra chuẩn kích thước bằng Board.fromShips
            if (shipLists.size() == config.getShipLengths().length) {
                Board fullBoard = Board.fromShips(shipLists, config.getShipLengths());
                if (fullBoard == null) {
                    throw new GameException(GameException.Code.OUT_OF_BOUNDS, "Bố trí tàu không hợp lệ (các tàu phải cách nhau ít nhất 1 ô)!");
                }
                ps.setBoard(fullBoard);
            } else {
                // Nếu đang đặt dở (1..4 tàu), dựng bàn nháp để phát trực tiếp cho Admin xem
                Board draftBoard = buildPartialOrFullBoard(shipLists);
                if (draftBoard != null) {
                    ps.setBoard(draftBoard);
                }
            }
        } else if (!ready) {
            ps.setBoard(new Board());
        }

        if (ready) {
            // Khi bấm Sẵn sàng, đảm bảo phải đủ 5 tàu chuẩn kích thước
            if (ps.board() == null || ps.board().getShips().size() != config.getShipLengths().length) {
                ps.setBoard(BoardGenerator.generate(config.getShipLengths(), rng));
            }
            ps.setReady(true);
            notifyListener(l -> l.onPlayerReady(this, playerId));

            // Nếu cả 2 người chơi đều đã bấm Sẵn sàng thì vào đếm ngược 3-2-1 luôn
            if (playerA.isReady() && playerB.isReady()) {
                cancelPending();
                beginCountdown();
            }
        }
        broadcastGodViewToSpectators();
    }

    /** Cho phép dựng bàn cờ từ 1..4 tàu đang đặt dở để hiển thị lên God View cho Admin. */
    private Board buildPartialOrFullBoard(List<List<Point>> shipCellsList) {
        Board board = new Board();
        if (shipCellsList == null || shipCellsList.isEmpty()) {
            return board;
        }

        for (List<Point> rawCells : shipCellsList) {
            if (rawCells == null || rawCells.isEmpty()) continue;
            List<Point> sorted = new ArrayList<>(rawCells);
            sorted.sort((a, b) -> (a.getRow() == b.getRow())
                    ? Integer.compare(a.getCol(), b.getCol())
                    : Integer.compare(a.getRow(), b.getRow()));

            boolean sameRow = true;
            boolean sameCol = true;
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).getRow() != sorted.get(0).getRow()) sameRow = false;
                if (sorted.get(i).getCol() != sorted.get(0).getCol()) sameCol = false;
            }
            if (!sameRow && !sameCol) return null;

            for (int i = 1; i < sorted.size(); i++) {
                int dr = sorted.get(i).getRow() - sorted.get(i - 1).getRow();
                int dc = sorted.get(i).getCol() - sorted.get(i - 1).getCol();
                if (dr + dc != 1) return null;
            }

            if (!board.canPlace(sorted)) {
                return null;
            }
            board.addShip(new Ship(board.getShips().size(), sorted));
        }
        return board;
    }

    /** Người chơi bấm "Tái đấu". Khi cả hai đã bấm thì bắt đầu giai đoạn đặt tàu 45s cho ván mới. */
    public synchronized void requestRematch(String playerId) {
        stateOf(playerId);
        if (phase == GamePhase.CLOSED) {
            throw new GameException(GameException.Code.ROOM_CLOSED, "Đối thủ đã rời phòng hoặc offline");
        }
        if (phase != GamePhase.FINISHED) throw new GameException(GameException.Code.INVALID_STATE, "Trận chưa kết thúc");

        if (!rematchRequests.add(playerId)) return;
        if (rematchRequests.size() == 2) {
            beginSetup();
        } else {
            notifyListener(l -> l.onRematchRequested(this, playerId));
        }
    }

    public synchronized void leave(String playerId) {
        if (phase == GamePhase.CLOSED || !isMember(playerId)) return;
        PlayerState leaver = stateOf(playerId);
        if (phase == GamePhase.PLAYING || phase == GamePhase.SETUP || phase == GamePhase.COUNTDOWN) {
            finish(opponentOf(leaver).id(), leaver.id(), GameResult.Reason.LEFT);
            notifyListener(l -> l.onPlayerLeft(this, playerId));
            close();
        } else if (phase == GamePhase.FINISHED) {
            rematchRequests.remove(playerId);
        } else {
            notifyListener(l -> l.onPlayerLeft(this, playerId));
            close();
        }
    }

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
            if (shooter.getMysteriesCollected() < 4) {
                result.setNewMysteryBox(board.spawnMystery(rng));
            }
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
        turnCounter++;
        if (dbMatchId > 0) {
            com.mycompany.battleship.server.dao.MatchDAO dao = new com.mycompany.battleship.server.dao.MatchDAO();
            int shooterId = playerId.equals(playerA.id()) ? playerAId : playerBId;

            String coordStr;
            if (target != null) {
                char rowChar = (char) ('A' + target.getRow());
                coordStr = rowChar + String.valueOf(target.getCol() + 1);
            } else {
                coordStr = "RAIN";
            }

            com.google.gson.JsonArray affectedArr = new com.google.gson.JsonArray();
            String sunkShipType = null;
            boolean anyHit = false;

            for (ShotResult.CellResult cell : result.getCells()) {
                Point p = cell.getPoint();
                char rChar = (char) ('A' + p.getRow());
                String cellCoord = rChar + String.valueOf(p.getCol() + 1);

                String cellRes = "MISS";
                if (cell.getSunkShip() != null) {
                    cellRes = "SUNK";
                    sunkShipType = "SHIP_" + cell.getSunkShip().getLength();
                    anyHit = true;
                } else if (cell.isHit()) {
                    cellRes = "HIT";
                    anyHit = true;
                }

                com.google.gson.JsonObject cellObj = new com.google.gson.JsonObject();
                cellObj.addProperty("coord", cellCoord);
                cellObj.addProperty("row", p.getRow());
                cellObj.addProperty("col", p.getCol());
                cellObj.addProperty("result", cellRes);
                affectedArr.add(cellObj);

                dao.recordShotStats(shooterId, cellCoord, cell.isHit());
            }

            String resType = (sunkShipType != null) ? "SUNK" : ((directHit || anyHit) ? "HIT" : "MISS");
            String affectedJson = gson.toJson(affectedArr);

            dao.saveMatchTurn(dbMatchId, turnCounter, shooterId, coordStr, resType, sunkShipType, type.name(), affectedJson);
        }

        broadcastGodViewToSpectators();
        return result;
    }

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

    public synchronized GameView viewFor(String playerId) {
        PlayerState me = stateOf(playerId);
        PlayerState opp = opponentOf(me);
        if (me.board() == null || opp.board() == null) {
            throw new GameException(GameException.Code.INVALID_STATE, "Trận chưa được khởi tạo");
        }

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
        if (phase == GamePhase.PLAYING || phase == GamePhase.SETUP) {
            secondsLeft = (int) Math.max(0, (turnDeadlineMillis - System.currentTimeMillis() + 999) / 1000);
        }
        return new GameView(phase, me.id(), opp.id(), currentTurnId, secondsLeft,
                turnDeadlineMillis, own, enemy, sunk, me.inventorySnapshot());
    }

    // ------------------------------------------------------------------ nội bộ: đặt tàu 45s, đếm ngược & lượt

    private void beginSetup() {
        cancelPending();
        rematchRequests.clear();
        // Khởi tạo bàn cờ trống để Admin vào xem thấy bàn trống và cập nhật từng tàu khi người chơi đặt
        playerA.resetForNewMatch(new Board());
        playerB.resetForNewMatch(new Board());
        currentTurnId = null;
        phase = GamePhase.SETUP;

        long myEpoch = ++epoch;
        long millis = config.getSetupSeconds() * 1000L;
        turnDeadlineMillis = System.currentTimeMillis() + millis;
        long deadline = turnDeadlineMillis;

        notifyListener(l -> l.onSetupStarted(this, config.getSetupSeconds(), deadline));
        broadcastGodViewToSpectators();
        schedule(() -> onSetupTimeout(myEpoch), millis);
    }

    private synchronized void onSetupTimeout(long myEpoch) {
        if (phase != GamePhase.SETUP || epoch != myEpoch) return;
        beginCountdown();
    }

    private void beginCountdown() {
        cancelPending();
        rematchRequests.clear();

        // Nếu hết 45s mà người chơi chưa đặt đủ 5 tàu, hệ thống tự động sinh ngẫu nhiên
        int requiredShips = config.getShipLengths().length;
        if (playerA.board() == null || playerA.board().getShips().size() != requiredShips) {
            playerA.setBoard(BoardGenerator.generate(config.getShipLengths(), rng));
        }
        if (playerB.board() == null || playerB.board().getShips().size() != requiredShips) {
            playerB.setBoard(BoardGenerator.generate(config.getShipLengths(), rng));
        }

        currentTurnId = null;
        phase = GamePhase.COUNTDOWN;
        notifyListener(l -> l.onMatchPrepared(this));
        broadcastGodViewToSpectators();
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

        if (dbMatchId > 0) {
            com.mycompany.battleship.server.dao.MatchDAO dao = new com.mycompany.battleship.server.dao.MatchDAO();
            dao.saveMatchBoard(dbMatchId, playerAId, gson.toJson(playerA.board().getShips()));
            dao.saveMatchBoard(dbMatchId, playerBId, gson.toJson(playerB.board().getShips()));
        }

        startTurn(playerA.id());
        broadcastGodViewToSpectators();
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
        this.lastResult = result;
        broadcastGodViewToSpectators();
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
        if (shooter.getMysteriesCollected() >= 4) return;

        if (!shooter.hasBonusSpace()) {
            result.incrementRewardsLost();
            return;
        }

        List<MissileType> pool = new ArrayList<>(Arrays.asList(MissileType.BIG, MissileType.NUCLEAR, MissileType.RAIN));
        if (shooter.nuclearReceived() >= config.getMaxNuclearPerMatch()) pool.remove(MissileType.NUCLEAR);
        MissileType reward = pool.get(rng.nextInt(pool.size()));

        shooter.addBonus(reward);
        result.addReward(reward);

        shooter.incrementMysteriesCollected();

        if (shooter.getMysteriesCollected() == 4) {
            PlayerState victim = opponentOf(shooter);
            victim.board().clearAllMysteries();
        }
    }

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