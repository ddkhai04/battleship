package com.mycompany.battleship.server;

import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.common.model.UserDTO;
import com.mycompany.battleship.server.dao.UserDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.mindrot.jbcrypt.BCrypt;

public class ClientHandler implements Runnable {
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String loggedInUsername = null;
    private User currentUser = null;
    
    // Lưu lại tham chiếu đến luồng của đối thủ khi vào trận
    private ClientHandler opponent = null; 

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Đã sẵn sàng giao tiếp với một Client mới!");
            
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Nhận được lệnh: " + request);
                
                String[] parts = request.split("\\|");
                String command = parts[0];

                switch (command) {
                    case "LOGIN":
                        if (parts.length >= 3) handleLogin(parts[1], parts[2]);
                        break;
                    case "REGISTER":
                        if (parts.length >= 3) handleRegister(parts[1], parts[2]);
                        break;
                    case "LIST_PLAYERS":
                        handleListPlayers();
                        break;
                    case "INVITE":
                        if (parts.length >= 2) handleInvite(parts[1]);
                        break;
                    case "ACCEPT":
                    case "INVITE_ACCEPT":    
                        if (parts.length >= 2) handleAccept(parts[1]);
                        break;
                    case "REJECT":
                    case "INVITE_REJECT":    
                        if (parts.length >= 2) handleReject(parts[1]);
                        break;
                    default:
                        System.out.println("Lệnh không xác định: " + command);
                }
            }
        } catch (IOException e) {
            System.out.println("Client đột ngột ngắt kết nối: " + e.getMessage());
        } finally {
            if (loggedInUsername != null) {
                BattleshipServer.onlineUsers.remove(loggedInUsername);
                
                if (currentUser != null) {
                    new UserDAO().updateStatus(currentUser.getId(), "OFFLINE");
                }
                
                // GIẢI CỨU ĐỐI THỦ: Xử lý nếu người này đang trong trận mà thoát ngang
                if (this.opponent != null) {
                    System.out.println("Giải cứu " + this.opponent.loggedInUsername + " do đối thủ thoát đột ngột.");
                    
                    // Gửi lệnh báo cho Client kia biết để đóng bàn cờ
                    this.opponent.sendMessage("OPPONENT_QUIT|Đối thủ đã mất kết nối. Trận đấu bị hủy.");
                    
                    // Kéo người ở lại về trạng thái ONLINE
                    if (this.opponent.currentUser != null) {
                        new UserDAO().updateStatus(this.opponent.currentUser.getId(), "ONLINE");
                        this.opponent.currentUser.setStatus("ONLINE");
                    }
                    
                    // Xóa liên kết
                    this.opponent.opponent = null;
                    this.opponent = null;
                }
                
                System.out.println(loggedInUsername + " đã thoát. Còn lại " + BattleshipServer.onlineUsers.size() + " người online.");
                
                // BROADCAST: Cập nhật ngay lập tức ra toàn bộ sảnh chờ
                for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                    client.handleListPlayers();
                }
            }
        }
    }

    private void handleLogin(String username, String password) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.checkLogin(username);
        
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            if (BattleshipServer.onlineUsers.containsKey(username)) {
                out.println("LOGIN_FAIL|Tài khoản này đang được đăng nhập ở nơi khác.");
                System.out.println(username + " đăng nhập thất bại do bị trùng phiên.");
                return;
            }
            
            this.loggedInUsername = username;
            this.currentUser = user;
            
            BattleshipServer.onlineUsers.put(username, this);
            
            userDAO.updateStatus(user.getId(), "ONLINE");
            this.currentUser.setStatus("ONLINE");
            
            out.println("LOGIN_OK|" + username + "|" + user.getScore());
            System.out.println(username + " đã đăng nhập thành công!");
            
            for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                client.handleListPlayers();
            }
            
        } else {
            out.println("LOGIN_FAIL|Sai tài khoản hoặc mật khẩu");
        }
    }

    private void handleRegister(String username, String password) {
        UserDAO userDAO = new UserDAO();
        
        if (userDAO.checkUsernameExist(username)) {
            out.println("REGISTER_FAIL|Tài khoản đã tồn tại");
            return;
        }
        
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword);
        newUser.setNickname(username);
        
        if (userDAO.registerUser(newUser)) {
            out.println("REGISTER_OK");
            System.out.println("Đăng ký thành công tài khoản: " + username);
        } else {
            out.println("REGISTER_FAIL|Lỗi hệ thống CSDL");
        }
    }

    private void handleListPlayers() {
        UserDAO userDAO = new UserDAO();
        java.util.List<UserDTO> lobbyUsers = userDAO.getLobbyUsers(); 
        
        if (lobbyUsers.isEmpty()) {
            out.println("PLAYER_LIST|");
            return;
        }

        StringBuilder sb = new StringBuilder("PLAYER_LIST|");
        for (int i = 0; i < lobbyUsers.size(); i++) {
            UserDTO u = lobbyUsers.get(i);
            
            String realStatus = u.getStatus();
            
            // Lọc toàn bộ lỗi "ma"
            if (!BattleshipServer.onlineUsers.containsKey(u.getNickname())) {
                realStatus = "OFFLINE";
            }
            
            sb.append(u.getNickname()).append(",")
              .append(realStatus).append(",")
              .append(u.getScore());
              
            if (i < lobbyUsers.size() - 1) {
                sb.append(";");
            }
        }
        
        out.println(sb.toString());
    }

    private void handleInvite(String targetUser) {
        if ("IN_GAME".equals(this.currentUser.getStatus())) {
            out.println("INVITE_FAIL|Bạn đang trong trận đấu, không thể thách đấu thêm.");
            return;
        }

        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        
        if (targetHandler != null && targetHandler.currentUser != null) {
            if ("IN_GAME".equals(targetHandler.currentUser.getStatus())) {
                out.println("INVITE_FAIL|Người chơi " + targetUser + " đang bận trong trận đấu khác.");
                return;
            }
            
            targetHandler.sendMessage("INVITE_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " đã gửi lời mời tới " + targetUser);
        } else {
            out.println("INVITE_FAIL|Người chơi " + targetUser + " không online.");
        }
    }

    private void handleAccept(String challengerUsername) {
        if ("IN_GAME".equals(this.currentUser.getStatus())) {
            out.println("ERROR|Bạn đang trong trận đấu khác, không thể vào trận này.");
            return;
        }

        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);
        
        if (challengerHandler != null && challengerHandler.currentUser != null && this.currentUser != null) {
            
            if ("IN_GAME".equals(challengerHandler.currentUser.getStatus())) {
                out.println("ERROR|Người thách đấu đã tham gia trận đấu khác.");
                return;
            }
            
            UserDAO userDAO = new UserDAO();
            
            userDAO.updateStatus(this.currentUser.getId(), "IN_GAME");
            userDAO.updateStatus(challengerHandler.currentUser.getId(), "IN_GAME");
            
            this.currentUser.setStatus("IN_GAME");
            challengerHandler.currentUser.setStatus("IN_GAME");
            
            this.opponent = challengerHandler;
            challengerHandler.opponent = this;
            
            String roomId = "ROOM_" + System.currentTimeMillis();
            
            // [ĐÃ FIX]: Tách riêng lệnh gửi cho từng người để hiển thị đúng tên đối thủ chéo nhau
            challengerHandler.sendMessage("MATCH_START|" + roomId + "|" + this.loggedInUsername);
            out.println("MATCH_START|" + roomId + "|" + challengerUsername);
            
            for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                client.handleListPlayers();
            }
            
            System.out.println("Trận đấu bắt đầu giữa: " + challengerUsername + " và " + this.loggedInUsername);
        } else {
            out.println("ERROR|Người thách đấu đã thoát hoặc không hợp lệ.");
        }
    }

    private void handleReject(String challengerUsername) {
        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);
        
        if (challengerHandler != null) {
            // Lệnh này đã bắn đúng về phía Client của người mời
            challengerHandler.sendMessage("REJECT_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " đã từ chối lời mời của " + challengerUsername);
        }
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}