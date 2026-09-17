package server.mock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MockServer {
    public static void main(String[] args) {
        System.out.println("=== MOCK SERVER BATTLESHIP ĐANG CHẠY TRÊN PORT 5000 ===");
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                System.out.println("Đang chờ Client kết nối...");
                Socket socket = serverSocket.accept();
                System.out.println("Client đã kết nối thành công: " + socket.getInetAddress());

                // Mở luồng xử lý cho Client
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi Server: Cổng 5000 có thể đang bị chiếm dụng.");
        }
    }

    private static void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[CLIENT GỬI] " + message);
                String[] parts = message.split("\\|");
                String command = parts[0];

                String response = "";
                switch (command) {
                    case "LOGIN":
                        // Mặc định cho đăng nhập thành công
                        response = "LOGIN_OK|" + parts[1] + "|9999";
                        break;
                    case "REGISTER":
                        response = "REGISTER_OK";
                        break;
                    case "LIST_PLAYERS":
                        // Trả về 3 người chơi ảo
                        response = "PLAYER_LIST|Khai,ONLINE,150;Thanh,IN_GAME,90;Giang,ONLINE,40";
                        break;
                    case "MATCH_HISTORY":
                        // Trả về lịch sử ảo
                        response = "HISTORY_LIST|M01,Khai,WIN,10:30;M02,Thanh,LOSE,09:15";
                        break;
                    case "LEADERBOARD":
                        // Trả về bảng xếp hạng ảo
                        response = "RANK_LIST|1,Linh,100,10,9999;2,Khai,50,20,150;3,Thanh,10,50,90";
                        break;
                    default:
                        response = "ERROR|Lệnh không hợp lệ";
                }

                System.out.println("[SERVER TRẢ VỀ] " + response);
                out.write(response);
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Client đã ngắt kết nối.");
        }
    }
}