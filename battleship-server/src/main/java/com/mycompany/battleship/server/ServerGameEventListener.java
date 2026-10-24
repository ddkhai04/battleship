package com.mycompany.battleship.server;

import com.mycompany.battleship.common.model.ChatMessage;
import com.mycompany.battleship.common.model.EmoteEvent;
import com.mycompany.battleship.common.model.GameResult;
import com.mycompany.battleship.common.model.MessageDTO;
import com.mycompany.battleship.common.model.ShotResult;
import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.server.dao.MatchDAO;
import com.mycompany.battleship.server.dao.UserDAO;
import com.mycompany.battleship.server.game.GameEventListener;
import com.mycompany.battleship.server.game.GameRoom;
import java.util.HashMap;
import java.util.Map;

public class ServerGameEventListener implements GameEventListener {

    private ClientHandler getHandler(String username) {
        if (username == null) return null;
        return BattleshipServer.onlineUsers.get(username);
    }

    // Đổi kiểu tham số thành MessageDTO
    private void sendToBoth(GameRoom room, MessageDTO message) {
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
        // Gửi thẳng đối tượng ShotResult, ClientHandler sẽ tự parse thành JSON
        sendToBoth(room, new MessageDTO("SHOT_RESULT", result));

        ClientHandler hA = getHandler(room.getInviterId());
        ClientHandler hB = getHandler(room.getInviteeId());
        if (hA != null) {
            hA.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviterId())));
        }
        if (hB != null) {
            hB.sendMessage(new MessageDTO("GAME_VIEW", room.viewFor(room.getInviteeId())));
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
            otherHandler.sendMessage(new MessageDTO("REMATCH_REQUESTED", fromPlayerId));
        }
    }

    @Override
    public void onPlayerLeft(GameRoom room, String playerId) {
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
        sendToBoth(room, new MessageDTO("ROOM_CLOSED", null));
    }
}