package com.mycompany.battleship.server;

import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.common.model.UserDTO;
import com.mycompany.battleship.server.dao.UserDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.mindrot.jbcrypt.BCrypt; // Import thư viện BCrypt

public class ClientHandler implements Runnable {
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String loggedInUsername = null;
    private User currentUser = null;

    // Constructor nhận kết nối từ BattleshipServer truyền sang
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
            // Vòng lặp liên tục đọc dòng chữ từ Client gửi lên
            while ((request = in.readLine()) != null) {
                System.out.println("Nhận được lệnh: " + request);
                
                // Tách chuỗi theo định dạng hợp đồng (ngăn cách bởi dấu |)
                String[] parts = request.split("\\|");
                String command = parts[0]; // Từ khóa đầu tiên luôn là Tên lệnh

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
                        if (parts.length >= 2) handleReject(parts[1]);
                        break;
                    default:
                        System.out.println("Lệnh không xác định: " + command);
                }
            }
        } catch (IOException e) {
            System.out.println("Client đột ngột ngắt kết nối: " + e.getMessage());
        } finally {
            // Xử lý dọn dẹp khi Client tắt cửa sổ game hoặc rớt mạng
            if (loggedInUsername != null) {
                BattleshipServer.onlineUsers.remove(loggedInUsername);
                
                // Gọi DAO cập nhật trạng thái về OFFLINE
                if (currentUser != null) {
                    new UserDAO().updateStatus(currentUser.getId(), "OFFLINE");
                }
                
                System.out.println(loggedInUsername + " đã thoát. Còn lại " + BattleshipServer.onlineUsers.size() + " người online.");
                
                // BROADCAST: Cập nhật ngay lập tức trạng thái OFFLINE ra toàn bộ sảnh chờ
                for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                    client.handleListPlayers();
                }
            }
        }
    }

    // --- CÁC HÀM XỬ LÝ CHUỖI LOGIC ---

    private void handleLogin(String username, String password) {
        UserDAO userDAO = new UserDAO();
        
        // Gọi hàm để lấy thông tin tài khoản từ DB
        User user = userDAO.checkLogin(username);
        
        // SỬ DỤNG BCRYPT ĐỂ KIỂM TRA MẬT KHẨU
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            
            // --- KIỂM TRA CHỐNG TRÙNG TÀI KHOẢN ---
            if (BattleshipServer.onlineUsers.containsKey(username)) {
                out.println("LOGIN_FAIL|Tài khoản này đang được đăng nhập ở nơi khác.");
                System.out.println(username + " đăng nhập thất bại do bị trùng phiên.");
                return;
            }
            
            this.loggedInUsername = username;
            this.currentUser = user; // Lưu lại để dùng cho các luồng khác
            
            BattleshipServer.onlineUsers.put(username, this);
            
            // Đổi trạng thái trong CSDL thành ONLINE
            userDAO.updateStatus(user.getId(), "ONLINE");
            this.currentUser.setStatus("ONLINE");
            
            // Trả về lệnh kèm Điểm số thật từ Database
            out.println("LOGIN_OK|" + username + "|" + user.getScore());
            System.out.println(username + " đã đăng nhập thành công!");
            
            // BROADCAST: Cập nhật ngay lập tức trạng thái ONLINE ra toàn bộ sảnh chờ
            for (ClientHandler client : BattleshipServer.onlineUsers.values()) {
                client.handleListPlayers();
            }
            
        } else {
            out.println("LOGIN_FAIL|Sai tài khoản hoặc mật khẩu");
        }
    }

    private void handleRegister(String username, String password) {
        UserDAO userDAO = new UserDAO();
        
        // Kiểm tra xem tên tài khoản đã bị người khác đăng ký chưa
        if (userDAO.checkUsernameExist(username)) {
            out.println("REGISTER_FAIL|Tài khoản đã tồn tại");
            return;
        }
        
        // MÃ HÓA MẬT KHẨU BẰNG BCRYPT TRƯỚC KHI LƯU (Độ phức tạp = 12)
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        // Tạo đối tượng User mới
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword); // Lưu chuỗi đã mã hóa
        newUser.setNickname(username);
        
        // Gọi hàm Insert xuống DB
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
            
            // Lọc lỗi "Online ma" từ Database nếu user không có thực trên RAM Server
            if ("ONLINE".equals(realStatus) && !BattleshipServer.onlineUsers.containsKey(u.getNickname())) {
                realStatus = "OFFLINE";
            }
            
            // Định dạng: nickname,status,score
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
        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        
        if (targetHandler != null) {
            targetHandler.sendMessage("INVITE_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " đã gửi lời mời tới " + targetUser);
        } else {
            out.println("INVITE_FAIL|Người chơi " + targetUser + " không online.");
        }
    }

    private void handleAccept(String challengerUsername) {
        ClientHandler challengerHandler = BattleshipServer.onlineUsers.get(challengerUsername);
        
        if (challengerHandler != null && challengerHandler.currentUser != null && this.currentUser != null) {
            UserDAO userDAO = new UserDAO();
            
            // 1. Cập nhật trạng thái xuống CSDL thành IN_GAME cho cả 2 người
            userDAO.updateStatus(this.currentUser.getId(), "IN_GAME");
            userDAO.updateStatus(challengerHandler.currentUser.getId(), "IN_GAME");
            
            // Cập nhật trạng thái trong bộ nhớ RAM của luồng
            this.currentUser.setStatus("IN_GAME");
            challengerHandler.currentUser.setStatus("IN_GAME");
            
            String roomId = "ROOM_" + System.currentTimeMillis();
            
            // 2. Gửi lệnh MATCH_START cho 2 người chơi vào trận
            challengerHandler.sendMessage("MATCH_START|" + roomId + "|" + this.loggedInUsername + "|" + challengerUsername);
            out.println("MATCH_START|" + roomId + "|" + challengerUsername + "|" + challengerUsername);
            
            // 3. BROADCAST: Cập nhật trạng thái IN_GAME ra toàn sảnh chờ ngay lập tức
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
            challengerHandler.sendMessage("REJECT_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " đã từ chối lời mời của " + challengerUsername);
        }
    }
    
    // Hàm phụ trợ để Server chủ động gửi tin nhắn xuống Client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}