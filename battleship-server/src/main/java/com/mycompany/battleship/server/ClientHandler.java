package com.mycompany.battleship.server;

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
                System.out.println(loggedInUsername + " đã thoát. Còn lại " + BattleshipServer.onlineUsers.size() + " người online.");
                // Sau này có thể gọi hàm báo cho các máy khác biết để cập nhật lại danh sách sảnh
            }
        }
    }

    // --- CÁC HÀM XỬ LÝ CHUỖI LOGIC ---

    private void handleLogin(String username, String password) {
        // TODO: Kết nối tới class UserDAO của Thành viên 1 để soi CSDL
        // Tạm thời Fake logic để test giao tiếp mạng trước:
        if ("khiemnguyen".equals(username) && "123456".equals(password)) {
            this.loggedInUsername = username;
            BattleshipServer.onlineUsers.put(username, this); // Ghi danh vào Map
            out.println("LOGIN_OK|" + username + "|100"); // 100 là điểm số giả lập
        } else {
            out.println("LOGIN_FAIL|Sai tài khoản hoặc mật khẩu");
        }
    }

    private void handleRegister(String username, String password) {
        // TODO: Kết nối với UserDAO để Insert
        out.println("REGISTER_OK"); 
    }

    private void handleListPlayers() {
        // TODO: Duyệt BattleshipServer.onlineUsers để nối chuỗi
        out.println("PLAYER_LIST|khiemnguyen,ONLINE,100;testuser,IN_GAME,50");
    }

    private void handleInvite(String targetUser) {
        // TODO: Tìm Socket của targetUser trong Map và đẩy chữ INVITE_FROM
        System.out.println(this.loggedInUsername + " đang mời " + targetUser + " chơi game!");
    }
    
    // Hàm phụ trợ để Server chủ động gửi tin nhắn xuống Client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}