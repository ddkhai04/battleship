package com.mycompany.battleship.server;

import com.google.gson.Gson;
import com.mycompany.battleship.common.model.Emote;
import com.mycompany.battleship.common.model.GameException;
import com.mycompany.battleship.common.model.MessageDTO;
import com.mycompany.battleship.common.model.MissileType;
import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.common.model.UserDTO;
import com.mycompany.battleship.server.dao.MatchDAO;
import com.mycompany.battleship.server.dao.UserDAO;
import com.mycompany.battleship.server.game.GameRoom;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String loggedInUsername = null;
    private User currentUser = null;
    private Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getLoggedInUsername() {
        return loggedInUsername;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Ready to communicate with client at " + socket.getRemoteSocketAddress());

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received: " + request);

                // Parse chuỗi JSON thành đối tượng MessageDTO
                MessageDTO msg;
                try {
                    msg = gson.fromJson(request, MessageDTO.class);
                } catch (Exception e) {
                    System.out.println("Invalid JSON format: " + request);
                    continue;
                }

                String command = msg.getCommand();
                Map<String, Object> payload = null;
                if (msg.getPayload() != null) {
                    payload = (Map<String, Object>) msg.getPayload();
                }

                switch (command) {
                    case "LOGIN":
                        if (payload != null) handleLogin((String) payload.get("username"), (String) payload.get("password"));
                        break;
                    case "GET_ADMIN_DATA":
                        handleGetAdminData();
                        break;
                    case "GET_REPLAY_DATA":
                        System.out.println(">>> [SERVER] Nhan duoc yeu cau GET_REPLAY_DATA tu Admin!");
                        if (payload != null && payload.containsKey("matchId")) {
                            int matchId = ((Number) payload.get("matchId")).intValue();
                            handleGetReplayData(matchId);
                        } else {
                            System.err.println(">>> [SERVER] Payload thieu matchId!");
                        }
                        break;
                    case "SPECTATE_JOIN":
                        if (payload != null && payload.containsKey("roomId")) {
                            handleSpectateJoin((String) payload.get("roomId"));
                        }
                        break;
                    case "SPECTATE_LEAVE":
                        handleSpectateLeave();
                        break;
                    case "REGISTER":
                        if (payload != null) handleRegister((String) payload.get("username"), (String) payload.get("password"));
                        break;
                    case "LIST_PLAYERS":
                        handleListPlayers();
                        break;
                    case "LEADERBOARD":
                        handleLeaderboard();
                        break;
                    case "MATCH_HISTORY":
                        handleMatchHistory();
                        break;   
                    case "INVITE":
                        if (payload != null) handleInvite((String) payload.get("target"));
                        break;
                    case "CANCEL_INVITE":
                        if (payload != null) handleCancelInvite((String) payload.get("target"));
                        break;
                    case "ACCEPT":
                    case "INVITE_ACCEPT":
                        if (payload != null) handleAccept((String) payload.get("challenger"));
                        break;
                    case "REJECT":
                    case "INVITE_REJECT":
                        if (payload != null) handleReject((String) payload.get("challenger"));
                        break;
                    case "FIRE":
                        if (payload != null) handleFire(payload);
                        break;
                    case "CHAT":
                        if (payload != null) handleChat((String) payload.get("text"));
                        break;
                    case "EMOTE":
                        if (payload != null) handleEmote((String) payload.get("emoteName"));
                        break;
                    case "REMATCH":
                        handleRematch();
                        break;
                    case "REJECT_REMATCH":
                        handleRejectRematch();
                        break;
                    case "GET_HEATMAP":
                        handleGetHeatmap(payload);
                        break;
                    case "SURRENDER":
                    case "LEAVE_MATCH":
                        handleSurrender();
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + loggedInUsername + " (" + e.getMessage() + ")");
        } finally {
            if (loggedInUsername != null) {
                BattleshipServer.gameManager.onPlayerDisconnected(loggedInUsername);
                BattleshipServer.onlineUsers.remove(loggedInUsername);

                if (currentUser != null) {
                    new UserDAO().updateStatus(currentUser.getId(), "OFFLINE");
                }

                System.out.println(loggedInUsername + " left. Online: " + BattleshipServer.onlineUsers.size());
                BattleshipServer.broadcastLobbyList();
            }
        }
    }
    private void handleGetAdminData() {
        if (this.currentUser == null || this.currentUser.getRole() != User.Role.ADMIN) {
            sendMessage(new MessageDTO("ERROR", "Bạn không có quyền quản trị!"));
            return;
        }

        UserDAO userDAO = new UserDAO();
        List<User> allUsers = userDAO.getAllUsers();

        // Gom danh sách user phù hợp với các cột của AdminGUI
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User u : allUsers) {
            Map<String, Object> uMap = new HashMap<>();
            uMap.put("id", u.getId());
            uMap.put("username", u.getUsername());
            uMap.put("nickname", u.getNickname());
            uMap.put("role", u.getRole().name());
            String realStatus = BattleshipServer.onlineUsers.containsKey(u.getUsername()) ? "ONLINE" : "OFFLINE";
            if (BattleshipServer.gameManager.isInGame(u.getUsername())) {
                realStatus = "IN_GAME";
            }
            uMap.put("status", realStatus);
            uMap.put("score", u.getScore());
            uMap.put("wins", u.getWins());
            uMap.put("losses", u.getLosses());
            userList.add(uMap);
        }

        // Danh sách trận ongoing từ GameManager
        List<Map<String, Object>> matchList = new ArrayList<>();
        // Duyệt qua các phòng đang diễn ra trong GameManager
        for (GameRoom room : BattleshipServer.gameManager.getAllRooms()) { 
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("matchId", room.getRoomId());
            mMap.put("player1Name", room.getInviterId());
            mMap.put("player2Name", room.getInviteeId());
            mMap.put("status", room.getPhase().name());
            matchList.add(mMap);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("users", userList);
        responseData.put("matches", matchList);
        // Bổ sung thêm vào cuối handleGetAdminData() trước khi sendMessage:
        MatchDAO matchDAO = new com.mycompany.battleship.server.dao.MatchDAO();
        responseData.put("finishedMatches", matchDAO.getFinishedMatchesForAdmin());

        sendMessage(new MessageDTO("ADMIN_DATA", responseData));
    }
    
    
        private void handleGetReplayData(int matchId) {
            try {
                com.mycompany.battleship.server.dao.MatchDAO dao = new com.mycompany.battleship.server.dao.MatchDAO();
                java.util.Map<Integer, String> boards = dao.getMatchBoards(matchId);
                List<com.mycompany.battleship.common.model.MatchTurn> turns = dao.getMatchTurns(matchId);
                Map<String, Object> matchInfo = dao.getMatchById(matchId);

                java.util.Map<String, Object> res = new java.util.HashMap<>();
                res.put("matchId", matchId);
                res.put("matchInfo", matchInfo);
                res.put("boards", boards);
                res.put("turns", turns);

                sendMessage(new MessageDTO("REPLAY_DATA", res));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleSpectateJoin(String roomId) {
            if (currentUser == null || currentUser.getRole() != User.Role.ADMIN) {
                sendMessage(new MessageDTO("ERROR", "Chỉ Admin mới có quyền theo dõi trận đấu!"));
                return;
            }
            GameRoom room = BattleshipServer.gameManager.findByRoomId(roomId);
            if (room != null) {
                room.addSpectator(this);
            } else {
                sendMessage(new MessageDTO("ERROR", "Phòng đấu không tồn tại hoặc đã kết thúc!"));
            }
        }

        private void handleSpectateLeave() {
            for (GameRoom room : BattleshipServer.gameManager.getAllRooms()) {
                room.removeSpectator(this);
            }
        }
    
    private void handleGetHeatmap(Map<String, Object> payload) {
        if (this.currentUser == null) return;

        // Chỉ cho phép tài khoản có quyền ADMIN
        String role = (this.currentUser.getRole() != null) ? this.currentUser.getRole().toString() : "USER";
        if (!"ADMIN".equalsIgnoreCase(role)) {
            sendMessage(new MessageDTO("ERROR", "Bạn không có quyền xem biểu đồ nhiệt!"));
            return;
        }

        // Lấy thống kê toàn server (truyền targetUserId = 0)
        int targetUserId = 0;
        if (payload != null && payload.containsKey("userId")) {
            targetUserId = ((Double) payload.get("userId")).intValue();
        }

        com.mycompany.battleship.server.dao.MatchDAO matchDAO = new com.mycompany.battleship.server.dao.MatchDAO();
        Map<String, Integer> placementHeatmap = matchDAO.getShipPlacementHeatmap(targetUserId);
        Map<String, Integer> shotHeatmap = matchDAO.getShotTargetHeatmap(targetUserId);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", targetUserId);
        responseData.put("placementHeatmap", placementHeatmap);
        responseData.put("shotHeatmap", shotHeatmap);

        sendMessage(new MessageDTO("HEATMAP_DATA", responseData));
    }
    
    private void handleLogin(String username, String password) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.checkLogin(username);

        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            if (BattleshipServer.onlineUsers.containsKey(username)) {
                Map<String, Object> failPayload = new HashMap<>();
                failPayload.put("message", "This account is already logged in elsewhere.");
                sendMessage(new MessageDTO("LOGIN_FAIL", failPayload));
                return;
            }

            this.loggedInUsername = username;
            this.currentUser = user;
            BattleshipServer.onlineUsers.put(username, this);

            userDAO.updateStatus(user.getId(), "ONLINE");
            this.currentUser.setStatus("ONLINE");

            // Đóng gói payload cho LOGIN_OK
            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("username", username);
            payloadData.put("score", user.getScore());
            
            // Xử lý an toàn tránh NullPointerException nếu role bị null
            String roleStr = (user.getRole() != null) ? user.getRole().name() : "USER";
            payloadData.put("role", roleStr);

            sendMessage(new MessageDTO("LOGIN_OK", payloadData));
            BattleshipServer.broadcastLobbyList();
        } else {
            Map<String, Object> failPayload = new HashMap<>();
            failPayload.put("message", "Invalid username or password.");
            sendMessage(new MessageDTO("LOGIN_FAIL", failPayload));
        }
    }

    private void handleRegister(String username, String password) {
        UserDAO userDAO = new UserDAO();
        if (userDAO.checkUsernameExist(username)) {
            sendMessage(new MessageDTO("REGISTER_FAIL", "Username already exists."));
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword);
        newUser.setNickname(username);
        // Tạm gán Role rỗng hoặc default trước khi insert tùy logic DAO của bạn

        if (userDAO.registerUser(newUser)) {
            sendMessage(new MessageDTO("REGISTER_OK", null));
        } else {
            sendMessage(new MessageDTO("REGISTER_FAIL", "Database system error."));
        }
    }

    public void handleListPlayers() {
        UserDAO userDAO = new UserDAO();
        java.util.List<UserDTO> lobbyUsers = userDAO.getLobbyUsers();
        
        for (UserDTO u : lobbyUsers) {
            if (!BattleshipServer.onlineUsers.containsKey(u.getNickname())) {
                u.setStatus("OFFLINE");
            }
        }
        
        // Gửi thẳng List Object, Gson sẽ tự động convert thành mảng JSON
        sendMessage(new MessageDTO("PLAYER_LIST", lobbyUsers));
    }

    private void handleInvite(String targetUser) {
        if ("IN_GAME".equals(this.currentUser.getStatus())) {
            sendMessage(new MessageDTO("INVITE_FAIL", "You are currently in a game, cannot send invites."));
            return;
        }
        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        if (targetHandler != null && targetHandler.currentUser != null) {
            if ("IN_GAME".equals(targetHandler.currentUser.getStatus())) {
                sendMessage(new MessageDTO("INVITE_FAIL", "Player " + targetUser + " is busy in another match."));
                return;
            }
            targetHandler.sendMessage(new MessageDTO("INVITE_FROM", this.loggedInUsername));
        } else {
            sendMessage(new MessageDTO("INVITE_FAIL", "Player " + targetUser + " is not online."));
        }
    }

    private void handleCancelInvite(String targetUser) {
        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        if (targetHandler != null) {
            targetHandler.sendMessage(new MessageDTO("INVITE_CANCELLED", this.loggedInUsername));
        }
    }

    private void handleAccept(String challengerUsername) {
        if ("IN_GAME".equals(this.currentUser.getStatus())) {
            sendMessage(new MessageDTO("ERROR", "You are in another match, cannot join this one."));
            return;
        }
        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);

        if (challengerHandler != null && challengerHandler.currentUser != null && this.currentUser != null) {
            if ("IN_GAME".equals(challengerHandler.currentUser.getStatus())) {
                sendMessage(new MessageDTO("ERROR", "The challenger has joined another match."));
                return;
            }

            UserDAO userDAO = new UserDAO();
            userDAO.updateStatus(this.currentUser.getId(), "IN_GAME");
            userDAO.updateStatus(challengerHandler.currentUser.getId(), "IN_GAME");
            this.currentUser.setStatus("IN_GAME");
            challengerHandler.currentUser.setStatus("IN_GAME");

            try {
                BattleshipServer.gameManager.createRoom(
                        challengerUsername,
                        this.loggedInUsername,
                        new ServerGameEventListener()
                );
                System.out.println("Created GameRoom: " + challengerUsername + " vs " + this.loggedInUsername);
            } catch (Exception e) {
                System.err.println("Failed to create room: " + e.getMessage());
                userDAO.updateStatus(this.currentUser.getId(), "ONLINE");
                userDAO.updateStatus(challengerHandler.currentUser.getId(), "ONLINE");
                this.currentUser.setStatus("ONLINE");
                challengerHandler.currentUser.setStatus("ONLINE");
                sendMessage(new MessageDTO("ERROR", e.getMessage()));
                challengerHandler.sendMessage(new MessageDTO("ERROR", e.getMessage()));
                return;
            }
            BattleshipServer.broadcastLobbyList();
        } else {
            sendMessage(new MessageDTO("ERROR", "The challenger has left or is invalid."));
        }
    }

    private void handleReject(String challengerUsername) {
        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);
        if (challengerHandler != null) {
            challengerHandler.sendMessage(new MessageDTO("REJECT_FROM", this.loggedInUsername));
        }
    }

    private void handleFire(Map<String, Object> payload) {
        if (this.loggedInUsername == null) return;
        GameRoom room = BattleshipServer.gameManager.findByPlayer(this.loggedInUsername);
        if (room == null) {
            sendMessage(new MessageDTO("GAME_ERROR", "Not currently in any active match."));
            return;
        }
        
        try {
            String typeStr = (String) payload.get("type");
            MissileType type = MissileType.valueOf(typeStr.toUpperCase());
            Point target = null;
            
            if (type.needsTarget()) {
                if (!payload.containsKey("row") || !payload.containsKey("col")) {
                    sendMessage(new MessageDTO("GAME_ERROR", "Missing target coordinates."));
                    return;
                }
                // Gson tự động parse số liệu từ JSON thành Double
                int r = ((Double) payload.get("row")).intValue();
                int c = ((Double) payload.get("col")).intValue();
                target = new Point(r, c);
            }
            room.fire(this.loggedInUsername, type, target);
        } catch (Exception e) {
            sendMessage(new MessageDTO("GAME_ERROR", "Unknown missile type or bad coordinate."));
        }
    }

    private void handleChat(String text) {
        if (this.loggedInUsername == null) return;
        GameRoom room = BattleshipServer.gameManager.findByPlayer(this.loggedInUsername);
        if (room == null) return;
        try {
            room.sendChat(this.loggedInUsername, text);
        } catch (GameException e) {
            sendMessage(new MessageDTO("CHAT_ERROR", e.getMessage()));
        }
    }

    private void handleEmote(String emoteName) {
        if (this.loggedInUsername == null) return;
        GameRoom room = BattleshipServer.gameManager.findByPlayer(this.loggedInUsername);
        if (room == null) return;
        try {
            Emote emote = Emote.valueOf(emoteName.toUpperCase());
            room.sendEmote(this.loggedInUsername, emote);
        } catch (IllegalArgumentException e) {
            sendMessage(new MessageDTO("EMOTE_ERROR", "Invalid emote name: " + emoteName));
        } catch (GameException e) {
            sendMessage(new MessageDTO("EMOTE_ERROR", e.getMessage()));
        }
    }

    private void handleRematch() {
        if (this.loggedInUsername == null) return;
        GameRoom room = BattleshipServer.gameManager.findByPlayer(this.loggedInUsername);
        if (room == null) return;
        try {
            room.requestRematch(this.loggedInUsername);
        } catch (GameException e) {
            sendMessage(new MessageDTO("GAME_ERROR", e.getMessage()));
        }
    }

    private void handleSurrender() {
        if (this.loggedInUsername == null) return;
        GameRoom room = BattleshipServer.gameManager.findByPlayer(this.loggedInUsername);
        if (room != null) {
            room.leave(this.loggedInUsername);
        }
    }

    public void handleLeaderboard() {
        com.mycompany.battleship.server.dao.UserDAO userDAO = new com.mycompany.battleship.server.dao.UserDAO();
        java.util.List<com.mycompany.battleship.common.model.UserDTO> topPlayers = userDAO.getLeaderboard(50); 
        sendMessage(new MessageDTO("RANK_LIST", topPlayers));
    }

    private void handleMatchHistory() {
        if (this.currentUser == null) {
            sendMessage(new MessageDTO("HISTORY_LIST", new java.util.ArrayList<>()));
            return;
        }
        
        com.mycompany.battleship.server.dao.MatchDAO matchDAO = new com.mycompany.battleship.server.dao.MatchDAO();
        java.util.List<com.mycompany.battleship.common.model.MatchHistoryDTO> historyList = matchDAO.getMatchHistory(this.currentUser.getId());
        
        // Formatter ngày tháng trực tiếp trên object DTO trước khi gửi nếu cần, hoặc để Client tự format
        sendMessage(new MessageDTO("HISTORY_LIST", historyList));
    }
    
    private void handleRejectRematch() {
        if (this.loggedInUsername == null) return;
        GameRoom room = BattleshipServer.gameManager.findByPlayer(this.loggedInUsername);
        
        if (room != null) {
            String opponent = room.getInviterId().equals(this.loggedInUsername) ? room.getInviteeId() : room.getInviterId();
            ClientHandler oppHandler = BattleshipServer.onlineUsers.get(opponent);
            if (oppHandler != null) {
                oppHandler.sendMessage(new MessageDTO("REMATCH_DECLINED", null));
            }
            room.leave(this.loggedInUsername);
        }
    }

    // --- CÁC HÀM GỬI TIN NHẮN TỚI CLIENT ---

    // Hàm mới: Gửi tin nhắn qua cấu trúc MessageDTO (JSON)
    public void sendMessage(MessageDTO message) {
        if (out != null) {
            out.println(gson.toJson(message));
        }
    }
    
}