package com.mycompany.battleship.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class BattleshipServer {
    
    // Lưu trữ các Client đang kết nối (Key là username) để quản lý danh sách online và gửi lời mời
    public static ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 2209; // Cổng mạng cho Server
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Lobby Server đang khởi động và lắng nghe ở port " + port + "...");
            
            // Vòng lặp vĩnh cửu để liên tục đón người chơi mới
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có kết nối mới từ: " + socket.getInetAddress());
                
                // Giao kết nối vừa nhận cho một Thread xử lý độc lập
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}