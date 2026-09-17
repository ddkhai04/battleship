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
            // Khởi tạo 2 luồng Đọc/Ghi Ký tự như đã thống nhất
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Đã sẵn sàng giao tiếp với Client!");
            
            // Bước tiếp theo chúng ta sẽ viết vòng lặp while(true) đọc chuỗi lệnh ở đây
            
        } catch (IOException e) {
            System.out.println("Lỗi luồng mạng với Client: " + e.getMessage());
        }
    }
    
    // Hàm phụ trợ để Server chủ động gửi tin nhắn xuống Client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}