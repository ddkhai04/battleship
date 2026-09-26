package com.mycompany.battleship.server;

import com.google.gson.Gson;
import com.mycompany.battleship.common.model.ChatMessage;
import com.mycompany.battleship.common.model.EmoteEvent;
import com.mycompany.battleship.common.model.GameResult;
import com.mycompany.battleship.common.model.ShotResult;
import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.server.dao.MatchDAO;
import com.mycompany.battleship.server.dao.UserDAO;
import com.mycompany.battleship.server.game.GameEventListener;
import com.mycompany.battleship.server.game.GameRoom;

public class ServerGameEventListener implements GameEventListener {

    private static final Gson gson = new Gson();

    private ClientHandler getHandler(String username) {
        if (username == null) return null;
        return BattleshipServer.onlineUsers.get(username);
    }

    private void sendToBoth(GameRoom room, String message) {
        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());
        if (hA != null) hA.sendMessage(message);
        if (hB != null) hB.sendMessage(message);
    }

    @Override
    public void onMatchPrepared(GameRoom room) {
        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());

        if (hA != null) {
            hA.sendMessage("MATCH_START|" + room.getRoomId() + "|" + room.getInviteeId());
            hA.sendMessage("GAME_VIEW|" + gson.toJson(room.viewFor(room.getInviterId())));
        }
        if (hB != null) {
            hB.sendMessage("MATCH_START|" + room.getRoomId() + "|" + room.getInviterId());
            hB.sendMessage("GAME_VIEW|" + gson.toJson(room.viewFor(room.getInviteeId())));
        }
    }

    @Override
    public void onCountdown(GameRoom room, int secondsLeft) {
        sendToBoth(room, "COUNTDOWN|" + secondsLeft);
    }

    @Override
    public void onTurnStarted(GameRoom room, String playerId, int turnSeconds, long deadlineMillis) {
        sendToBoth(room, "TURN_START|" + playerId + "|" + turnSeconds + "|" + deadlineMillis);
    }

    @Override
    public void onShotResolved(GameRoom room, ShotResult result) {
        sendToBoth(room, "SHOT_RESULT|" + gson.toJson(result));

        // Cập nhật lại góc nhìn bàn cờ cho từng người chơi
        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());
        if (hA != null) {
            hA.sendMessage("GAME_VIEW|" + gson.toJson(room.viewFor(room.getInviterId())));
        }
        if (hB != null) {
            hB.sendMessage("GAME_VIEW|" + gson.toJson(room.viewFor(room.getInviteeId())));
        }
    }

    @Override
    public void onGameOver(GameRoom room, GameResult result) {
        String jsonResult = gson.toJson(result);

        ClientHandler winnerHandler = getHandler(result.getWinnerId());
        ClientHandler loserHandler = getHandler(result.getLoserId());

        if (winnerHandler != null) {
            winnerHandler.sendMessage("GAME_OVER|WIN|" + result.getReason() + "|" + jsonResult);
        }
        if (loserHandler != null) {
            loserHandler.sendMessage("GAME_OVER|LOSE|" + result.getReason() + "|" + jsonResult);
        }

        // Lưu kết quả trận đấu vào MySQL Database
        UserDAO userDAO = new UserDAO();
        User winnerUser = userDAO.checkLogin(result.getWinnerId());
        User loserUser = userDAO.checkLogin(result.getLoserId());

        if (winnerUser != null && loserUser != null) {
            MatchDAO matchDAO = new MatchDAO();
            matchDAO.saveMatchResult(
                    winnerUser.getId(),
                    loserUser.getId(),
                    winnerUser.getId(),
                    loserUser.getId(),
                    GameResult.WIN_POINTS
            );

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
            otherHandler.sendMessage("REMATCH_REQUESTED|" + fromPlayerId);
        }
    }

    @Override
    public void onPlayerLeft(GameRoom room, String playerId) {
        sendToBoth(room, "PLAYER_LEFT|" + playerId);
    }

    @Override
    public void onChatMessage(GameRoom room, ChatMessage message) {
        sendToBoth(room, "CHAT|" + message.getSenderId() + "|" + message.getText() + "|" + message.getTimestampMillis());
    }

    @Override
    public void onEmote(GameRoom room, EmoteEvent event) {
        sendToBoth(room, "EMOTE|" + event.getSenderId() + "|" + event.getEmote().name() + "|" + event.getTimestampMillis());
    }

    @Override
    public void onRoomClosed(GameRoom room) {
        sendToBoth(room, "ROOM_CLOSED");
    }
}