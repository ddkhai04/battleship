package com.mycompany.battleship.server;

import com.google.gson.Gson;
import com.mycompany.battleship.common.model.ChatMessage;
import com.mycompany.battleship.common.model.EmoteEvent;
import com.mycompany.battleship.common.model.GameResult;
import com.mycompany.battleship.common.model.GameView;
import com.mycompany.battleship.common.model.MessageDTO;
import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.ShotResult;
import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.server.dao.MatchDAO;
import com.mycompany.battleship.server.dao.UserDAO;
import com.mycompany.battleship.server.game.GameEventListener;
import com.mycompany.battleship.server.game.GameRoom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private List<String> layToaDoOTau(GameView view) {
        List<String> danhSach = new ArrayList<>();
        for (int r = 0; r < view.ownGrid.length; r++) {
            for (int c = 0; c < view.ownGrid[r].length; c++) {
                char o = view.ownGrid[r][c];
                if (o == 'S' || o == 'X') {
                    danhSach.add(formatCoordinate(new Point(r, c)));
                }
            }
        }
        return danhSach;
    }

    @Override
    public void onSetupStarted(GameRoom room, int setupSeconds, long deadlineMillis) {
        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());

        User p1 = userDAO.checkLogin(room.getInviterId());
        User p2 = userDAO.checkLogin(room.getInviteeId());

        if (p1 != null && p2 != null) {
            userDAO.updateStatus(p1.getId(), "IN_GAME");
            userDAO.updateStatus(p2.getId(), "IN_GAME");
            if (hA != null && hA.getCurrentUser() != null) {
                hA.getCurrentUser().setStatus("IN_GAME");
            }
            if (hB != null && hB.getCurrentUser() != null) {
                hB.getCurrentUser().setStatus("IN_GAME");
            }
            BattleshipServer.broadcastLobbyList();
        }

        if (hA != null) {
            Map<String, Object> payloadA = new HashMap<>();
            payloadA.put("roomId", room.getRoomId());
            payloadA.put("opponent", room.getInviteeId());
            payloadA.put("setupSeconds", setupSeconds);
            hA.sendMessage(new MessageDTO("MATCH_START", payloadA));
        }
        if (hB != null) {
            Map<String, Object> payloadB = new HashMap<>();
            payloadB.put("roomId", room.getRoomId());
            payloadB.put("opponent", room.getInviterId());
            payloadB.put("setupSeconds", setupSeconds);
            hB.sendMessage(new MessageDTO("MATCH_START", payloadB));
        }
    }

    @Override
    public void onPlayerReady(GameRoom room, String playerId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("player", playerId);
        payload.put("ready", true);
        sendToBoth(room, new MessageDTO("SETUP_STATUS", payload));
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

                    // 3. Ghi các ô đặt tàu của 2 người chơi vào user_ship_placements
                    matchDAO.recordShipPlacements(p1.getId(), layToaDoOTau(room.viewFor(room.getInviterId())));
                    matchDAO.recordShipPlacements(p2.getId(), layToaDoOTau(room.viewFor(room.getInviteeId())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (hA != null) {
            hA.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviterId())));
        }
        if (hB != null) {
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

        try {
            room.broadcastGodViewToSpectators();
        } catch (Exception ignored) {}

        Integer matchId = roomMatchIdMap.get(room.getRoomId());
        if (matchId != null) {
            User shooter = userDAO.checkLogin(result.getShooterId());
            if (shooter != null) {
                int turnNumber = roomTurnCountMap.getOrDefault(room.getRoomId(), 1);
                roomTurnCountMap.put(room.getRoomId(), turnNumber + 1);

                String coordinate = (result.getTarget() != null) ? formatCoordinate(result.getTarget()) : "RAIN";
                String missileTypeStr = (result.getMissile() != null) ? result.getMissile().name() : "SIMPLE";

                com.google.gson.JsonArray affectedArr = new com.google.gson.JsonArray();
                boolean anyHit = false;
                String sunkType = null;

                for (ShotResult.CellResult cell : result.getCells()) {
                    Point p = cell.getPoint();
                    String cellCoord = formatCoordinate(p);

                    String cellRes = "MISS";
                    if (cell.getSunkShip() != null) {
                        cellRes = "SUNK";
                        anyHit = true;
                        int shipSize = (cell.getSunkShip().getCells() != null) ? cell.getSunkShip().getCells().size() : 3;
                        sunkType = "SHIP_" + shipSize;
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

                    try {
                        matchDAO.recordShotStats(shooter.getId(), cellCoord, cell.isHit());
                    } catch (Exception ignored) {}
                }

                java.util.List<com.mycompany.battleship.common.model.Ship> sunkShips = result.getSunkShips();
                boolean isSunk = (sunkType != null) || (sunkShips != null && !sunkShips.isEmpty());
                if (isSunk && sunkType == null) {
                    com.mycompany.battleship.common.model.Ship ship = sunkShips.get(0);
                    int shipSize = (ship.getCells() != null) ? ship.getCells().size() : 3;
                    sunkType = "SHIP_" + shipSize;
                }
                if (sunkType != null && sunkType.length() > 20) {
                    sunkType = sunkType.substring(0, 20);
                }

                boolean isHit = result.isDirectHit() || anyHit;
                String resultStr = isSunk ? "SUNK" : (isHit ? "HIT" : "MISS");
                String affectedCellsJson = gson.toJson(affectedArr);

                try {
                    matchDAO.saveMatchTurn(
                            matchId,
                            turnNumber,
                            shooter.getId(),
                            coordinate,
                            resultStr,
                            sunkType,
                            missileTypeStr,
                            affectedCellsJson
                    );
                } catch (Exception e) {
                    System.err.println("Lỗi lưu lượt bắn vào Database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onGameOver(GameRoom room, GameResult result) {
        ClientHandler winnerHandler = getHandler(result.getWinnerId());
        ClientHandler loserHandler = getHandler(result.getLoserId());

        long deadlineMillis = System.currentTimeMillis() + 30000;

        if (winnerHandler != null) {
            Map<String, Object> winPayload = new HashMap<>();
            winPayload.put("outcome", "WIN");
            winPayload.put("reason", result.getReason().name());
            winPayload.put("result", result);
            winPayload.put("rematchAvailable", true);
            winPayload.put("rematchDeadlineMillis", deadlineMillis);
            winnerHandler.sendMessage(new MessageDTO("GAME_OVER", winPayload));
        }

        if (loserHandler != null) {
            Map<String, Object> losePayload = new HashMap<>();
            losePayload.put("outcome", "LOSE");
            losePayload.put("reason", result.getReason().name());
            losePayload.put("result", result);
            losePayload.put("rematchAvailable", true);
            losePayload.put("rematchDeadlineMillis", deadlineMillis);
            loserHandler.sendMessage(new MessageDTO("GAME_OVER", losePayload));
        }

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
        }

        // CHÚ Ý: Đã xóa toàn bộ các lệnh setStatus("ONLINE") ở đây

        if (winnerHandler != null && winnerHandler.getCurrentUser() != null) {
            winnerHandler.getCurrentUser().setScore(winnerHandler.getCurrentUser().getScore() + GameResult.WIN_POINTS);
            winnerHandler.getCurrentUser().setWins(winnerHandler.getCurrentUser().getWins() + 1);
        }
        if (loserHandler != null && loserHandler.getCurrentUser() != null) {
            loserHandler.getCurrentUser().setScore(Math.max(0, loserHandler.getCurrentUser().getScore() - GameResult.WIN_POINTS));
            loserHandler.getCurrentUser().setLosses(loserHandler.getCurrentUser().getLosses() + 1);
        }

        BattleshipServer.broadcastLobbyList();
    }
    
    @Override
    public void onRematchRequested(GameRoom room, String fromPlayerId) {
        String otherId = fromPlayerId.equals(room.getInviterId()) ? room.getInviteeId() : room.getInviterId();
        ClientHandler sender = getHandler(fromPlayerId);
        ClientHandler otherHandler = getHandler(otherId);

        if (otherHandler == null || otherHandler.getCurrentUser() == null) {
            if (sender != null) {
                sender.sendMessage(new MessageDTO("REMATCH_DECLINED", "Đối thủ hiện đang Offline, không thể tái đấu!"));
            }
            return;
        }

        GameRoom otherRoom = BattleshipServer.gameManager.findByPlayer(otherId);
        boolean inAnotherMatch = (otherRoom != null && !otherRoom.getRoomId().equals(room.getRoomId()))
                || (otherRoom == null && "IN_GAME".equalsIgnoreCase(otherHandler.getCurrentUser().getStatus()));

        if (inAnotherMatch) {
            if (sender != null) {
                sender.sendMessage(new MessageDTO("REMATCH_DECLINED", "Đối thủ đang trong trận đấu khác!"));
            }
            return;
        }

        otherHandler.sendMessage(new MessageDTO("REMATCH_REQUESTED", fromPlayerId));
    }

    @Override
    public void onPlayerLeft(GameRoom room, String playerId) {
        // Hủy trận nếu thoát khi đang đặt tàu / đếm ngược
        if (room.getPhase() == com.mycompany.battleship.common.model.GamePhase.SETUP || 
            room.getPhase() == com.mycompany.battleship.common.model.GamePhase.COUNTDOWN) {
            Integer matchId = roomMatchIdMap.get(room.getRoomId());
            if (matchId != null) {
                matchDAO.abortMatch(matchId, 0);
            }
        }
        sendToBoth(room, new MessageDTO("OPPONENT_LEFT", playerId));
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

        // Nguyên tắc 3: Đặt trạng thái ONLINE duy nhất 1 lần khi phòng chính thức đóng
        String p1Id = room.getInviterId();
        String p2Id = room.getInviteeId();

        User u1 = userDAO.checkLogin(p1Id);
        if (u1 != null) userDAO.updateStatus(u1.getId(), "ONLINE");

        User u2 = userDAO.checkLogin(p2Id);
        if (u2 != null) userDAO.updateStatus(u2.getId(), "ONLINE");

        ClientHandler h1 = getHandler(p1Id);
        if (h1 != null && h1.getCurrentUser() != null) h1.getCurrentUser().setStatus("ONLINE");

        ClientHandler h2 = getHandler(p2Id);
        if (h2 != null && h2.getCurrentUser() != null) h2.getCurrentUser().setStatus("ONLINE");

        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", room.getCloseReason());
        sendToBoth(room, new MessageDTO("ROOM_CLOSED", payload));

        BattleshipServer.broadcastLobbyList();
    }
}