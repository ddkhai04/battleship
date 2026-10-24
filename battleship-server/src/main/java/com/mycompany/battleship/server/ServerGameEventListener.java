package com.mycompany.battleship.server;

import com.google.gson.Gson;
import com.mycompany.battleship.common.model.ChatMessage;
import com.mycompany.battleship.common.model.EmoteEvent;
import com.mycompany.battleship.common.model.GameResult;
import com.mycompany.battleship.common.model.MessageDTO;
import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.ShotResult;
import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.server.dao.MatchDAO;
import com.mycompany.battleship.server.dao.UserDAO;
import com.mycompany.battleship.server.game.GameEventListener;
import com.mycompany.battleship.server.game.GameRoom;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGameEventListener implements GameEventListener {

    private final Map<String, Integer> roomMatchIdMap = new ConcurrentHashMap<>();
    private final Map<String, Long> roomStartTimeMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> roomTurnCountMap = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final MatchDAO matchDAO = new MatchDAO();
    private final UserDAO userDAO = new UserDAO();

    private ClientHandler getHandler(String username) {
        if (username == null) return null;
        return BattleshipServer.onlineUsers.get(username);
    }

    private void sendToBoth(GameRoom room, MessageDTO message) {
        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());
        if (hA != null) hA.sendMessage(message);
        if (hB != null) hB.sendMessage(message);
    }

    private String formatCoordinate(Point p) {
        if (p == null) return "A1";
        char rowChar = (char) ('A' + p.getRow());
        return "" + rowChar + (p.getCol() + 1);
    }

    @Override
    public void onMatchPrepared(GameRoom room) {
        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());

        User p1 = userDAO.checkLogin(room.getInviterId());
        User p2 = userDAO.checkLogin(room.getInviteeId());

        if (p1 != null && p2 != null) {
            // 1. Tạo match ở trạng thái ONGOING trong DB
            int matchId = matchDAO.createMatch(p1.getId(), p2.getId());
            if (matchId > 0) {
                roomMatchIdMap.put(room.getRoomId(), matchId);
                roomStartTimeMap.put(room.getRoomId(), System.currentTimeMillis());
                roomTurnCountMap.put(room.getRoomId(), 1);

                // 2. Lưu sơ đồ xếp tàu (ship_data) của 2 người chơi vào match_boards
                try {
                    matchDAO.saveMatchBoard(matchId, p1.getId(), gson.toJson(room.viewFor(room.getInviterId())));
                    matchDAO.saveMatchBoard(matchId, p2.getId(), gson.toJson(room.viewFor(room.getInviteeId())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (hA != null) {
            Map<String, Object> payloadA = new HashMap<>();
            payloadA.put("roomId", room.getRoomId());
            payloadA.put("opponent", room.getInviteeId());
            hA.sendMessage(new MessageDTO("MATCH_START", payloadA));
            hA.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviterId())));
        }
        if (hB != null) {
            Map<String, Object> payloadB = new HashMap<>();
            payloadB.put("roomId", room.getRoomId());
            payloadB.put("opponent", room.getInviterId());
            hB.sendMessage(new MessageDTO("MATCH_START", payloadB));
            hB.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviteeId())));
        }
    }

    @Override
    public void onCountdown(GameRoom room, int secondsLeft) {
        sendToBoth(room, new MessageDTO("COUNTDOWN", secondsLeft));
    }

    @Override
    public void onTurnStarted(GameRoom room, String playerId, int turnSeconds, long deadlineMillis) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("turnPlayer", playerId);
        payload.put("seconds", turnSeconds);
        payload.put("deadlineMillis", deadlineMillis);
        sendToBoth(room, new MessageDTO("TURN_START", payload));
    }

    @Override
    public void onShotResolved(GameRoom room, ShotResult result) {
        sendToBoth(room, new MessageDTO("SHOT_RESULT", result));

        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());
        if (hA != null) hA.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviterId())));
        if (hB != null) hB.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviteeId())));

        // Lưu thông tin lượt bắn vào match_turns và cập nhật thống kê tọa độ/độ chính xác
        Integer matchId = roomMatchIdMap.get(room.getRoomId());
        if (matchId != null) {
            User shooter = userDAO.checkLogin(result.getShooterId());
            if (shooter != null) {
                int turnNumber = roomTurnCountMap.getOrDefault(room.getRoomId(), 1);
                roomTurnCountMap.put(room.getRoomId(), turnNumber + 1);

                // 1. Toạ độ bắn (với đạn mưa RAIN thì target là null)
                String coordinate = (result.getTarget() != null) ? formatCoordinate(result.getTarget()) : "RAIN";
                // 2. Kiểm tra bắn trúng (trúng trực tiếp hoặc có ít nhất 1 ô trong vùng nổ trúng tàu)
                boolean isHit = result.isDirectHit() || result.getCells().stream().anyMatch(ShotResult.CellResult::isHit);
                // 3. Kiểm tra tàu chìm thông qua danh sách getSunkShips()
                java.util.List<com.mycompany.battleship.common.model.Ship> sunkShips = result.getSunkShips();
                boolean isSunk = !sunkShips.isEmpty();
                String resultStr = isSunk ? "SUNK" : (isHit ? "HIT" : "MISS");
                // 4. Lấy loại tàu bị chìm nếu có
                String sunkType = null;
                if (isSunk) {
                    com.mycompany.battleship.common.model.Ship ship = sunkShips.get(0);
                    // Dùng ship.toString() để an toàn với mọi class/enum Ship, 
                    // hoặc ship.getType().name() / ship.getName() nếu Ship có getter tương ứng
                    sunkType = ship.toString();
                }
                // 5. Lưu vào database
                matchDAO.saveMatchTurn(matchId, turnNumber, shooter.getId(), coordinate, resultStr, sunkType);
                matchDAO.recordShotStats(shooter.getId(), coordinate, isHit);
            }
        }
    }

    @Override
    public void onGameOver(GameRoom room, GameResult result) {
        ClientHandler winnerHandler = getHandler(result.getWinnerId());
        ClientHandler loserHandler = getHandler(result.getLoserId());

        if (winnerHandler != null) {
            Map<String, Object> winPayload = new HashMap<>();
            winPayload.put("outcome", "WIN");
            winPayload.put("reason", result.getReason());
            winPayload.put("result", result);
            winnerHandler.sendMessage(new MessageDTO("GAME_OVER", winPayload));
        }
        
        if (loserHandler != null) {
            Map<String, Object> losePayload = new HashMap<>();
            losePayload.put("outcome", "LOSE");
            losePayload.put("reason", result.getReason());
            losePayload.put("result", result);
            loserHandler.sendMessage(new MessageDTO("GAME_OVER", losePayload));
        }

        // Tính thời lượng trận đấu (duration_seconds) và lưu vào database
        Integer matchId = roomMatchIdMap.remove(room.getRoomId());
        Long startTime = roomStartTimeMap.remove(room.getRoomId());
        roomTurnCountMap.remove(room.getRoomId());

        int durationSeconds = 0;
        if (startTime != null) {
            durationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
        }

        User winnerUser = userDAO.checkLogin(result.getWinnerId());
        User loserUser = userDAO.checkLogin(result.getLoserId());

        if (winnerUser != null && loserUser != null) {
            if (matchId != null) {
                matchDAO.finishMatch(matchId, winnerUser.getId(), loserUser.getId(), GameResult.WIN_POINTS, durationSeconds);
            } else {
                matchDAO.saveMatchResult(winnerUser.getId(), loserUser.getId(), winnerUser.getId(), loserUser.getId(), GameResult.WIN_POINTS, durationSeconds);
            }

            userDAO.updateStatus(winnerUser.getId(), "ONLINE");
            userDAO.updateStatus(loserUser.getId(), "ONLINE");
        }

        if (winnerHandler != null && winnerHandler.getCurrentUser() != null) {
            winnerHandler.getCurrentUser().setStatus("ONLINE");
            winnerHandler.getCurrentUser().setScore(winnerHandler.getCurrentUser().getScore() + GameResult.WIN_POINTS);
            winnerHandler.getCurrentUser().setWins(winnerHandler.getCurrentUser().getWins() + 1);
        }
        if (loserHandler != null && loserHandler.getCurrentUser() != null) {
            loserHandler.getCurrentUser().setStatus("ONLINE");
            loserHandler.getCurrentUser().setScore(Math.max(0, loserHandler.getCurrentUser().getScore() - GameResult.WIN_POINTS));
            loserHandler.getCurrentUser().setLosses(loserHandler.getCurrentUser().getLosses() + 1);
        }

        BattleshipServer.broadcastLobbyList();
    }

    @Override
    public void onRematchRequested(GameRoom room, String fromPlayerId) {
        String otherId = fromPlayerId.equals(room.getInviterId()) ? room.getInviteeId() : room.getInviterId();
        ClientHandler otherHandler = getHandler(otherId);
        if (otherHandler != null) {
            otherHandler.sendMessage(new MessageDTO("REMATCH_REQUESTED", fromPlayerId));
        }
    }

    @Override
    public void onPlayerLeft(GameRoom room, String playerId) {
        Integer matchId = roomMatchIdMap.get(room.getRoomId());
        Long startTime = roomStartTimeMap.get(room.getRoomId());
        if (matchId != null && startTime != null) {
            int durationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
            matchDAO.abortMatch(matchId, durationSeconds);
        }
        sendToBoth(room, new MessageDTO("PLAYER_LEFT", playerId));
    }

    @Override
    public void onChatMessage(GameRoom room, ChatMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", message.getSenderId());
        payload.put("text", message.getText());
        payload.put("timestamp", message.getTimestampMillis());
        sendToBoth(room, new MessageDTO("CHAT", payload));
    }

    @Override
    public void onEmote(GameRoom room, EmoteEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", event.getSenderId());
        payload.put("emoteName", event.getEmote().name());
        payload.put("timestamp", event.getTimestampMillis());
        sendToBoth(room, new MessageDTO("EMOTE", payload));
    }

    @Override
    public void onRoomClosed(GameRoom room) {
        roomMatchIdMap.remove(room.getRoomId());
        roomStartTimeMap.remove(room.getRoomId());
        roomTurnCountMap.remove(room.getRoomId());
        sendToBoth(room, new MessageDTO("ROOM_CLOSED", null));
    }
}