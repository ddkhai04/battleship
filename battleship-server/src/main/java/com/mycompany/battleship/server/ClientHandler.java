package com.mycompany.battleship.server;

import com.mycompany.battleship.common.model.User;
import com.mycompany.battleship.server.dao.UserDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
            }
        }
    }

    // --- CÁC HÀM XỬ LÝ CHUỖI LOGIC ---

    private void handleLogin(String username, String password) {
        UserDAO userDAO = new UserDAO();
        
        // Gọi hàm của Thành viên 1 để lấy thông tin tài khoản từ DB
        User user = userDAO.checkLogin(username);
        
        // Tự thực hiện kiểm tra mật khẩu bằng Java
        if (user != null && user.getPassword().equals(password)) {
            this.loggedInUsername = username;
            this.currentUser = user; // Lưu lại để dùng cho các luồng khác
            
            BattleshipServer.onlineUsers.put(username, this);
            
            // Đổi trạng thái trong CSDL thành ONLINE
            userDAO.updateStatus(user.getId(), "ONLINE");
            
            // Trả về lệnh kèm Điểm số thật từ Database
            out.println("LOGIN_OK|" + username + "|" + user.getScore());
            System.out.println(username + " đã đăng nhập thành công!");
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
        
        // Tạo đối tượng User mới. Tạm thời dùng chính username làm nickname
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
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
        // Lấy danh sách người chơi từ DB (Thành viên 1 đã viết sẵn hàm sắp xếp theo trạng thái và điểm)
        var lobbyUsers = userDAO.getLobbyUsers(); 
        
        if (lobbyUsers.isEmpty()) {
            out.println("PLAYER_LIST|");
            return;
        }

        StringBuilder sb = new StringBuilder("PLAYER_LIST|");
        for (int i = 0; i < lobbyUsers.size(); i++) {
            var u = lobbyUsers.get(i);
            // Định dạng: nickname,status,score
            sb.append(u.getNickname()).append(",")
              .append(u.getStatus()).append(",")
              .append(u.getScore());
              
            // Ngăn cách các user bằng dấu chấm phẩy
            if (i < lobbyUsers.size() - 1) {
                sb.append(";");
            }
        }
        
        out.println(sb.toString());
    }

    private void handleInvite(String targetUser) {
        // Tìm luồng Socket của đối phương trong Map onlineUsers
        ClientHandler targetHandler = BattleshipServer.onlineUsers.get(targetUser);
        
        if (targetHandler != null) {
            // Đẩy lệnh INVITE_FROM thẳng sang màn hình của người bị mời
            targetHandler.sendMessage("INVITE_FROM|" + this.loggedInUsername);
            System.out.println(this.loggedInUsername + " đã gửi lời mời tới " + targetUser);
        } else {
            // Trả lỗi về cho người mời nếu đối phương vừa thoát
            out.println("INVITE_FAIL|Người chơi " + targetUser + " không online.");
        }
    }
    
    // Hàm phụ trợ để Server chủ động gửi tin nhắn xuống Client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}