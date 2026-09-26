package com.mycompany.battleship.client.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javafx.application.Platform;
import javafx.stage.Stage;

public class SocketClientManager {

    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 2209;

    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;

    public static LoginGUI loginScreen;
    public static RegisterGUI registerScreen;
    public static LobbyGUI lobbyScreen;
    
    public static GameRoomGUI gameRoomScreen; 
    public static Stage gameStage;

    public static boolean ketNoiServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            Thread threadLangNghe = new Thread(new Runnable() {
                @Override
                public void run() {
                    langNgheServer();
                }
            });
            threadLangNghe.setDaemon(true);
            threadLangNghe.start();

            return true;
        } catch (IOException e) {
            System.out.println("[LOI] Khong ket noi duoc Server: " + e.getMessage());
            return false;
        }
    }

    public static void guiTinNhan(String noiDung) {
        try {
            if (out == null) {
                System.out.println("[LOI] Chua ket noi Server, khong gui duoc.");
                return;
            }
            out.write(noiDung);
            out.newLine();
            out.flush();
            
            String logContent = noiDung;
            if (noiDung.startsWith("LOGIN|") || noiDung.startsWith("REGISTER|")) {
                String[] parts = noiDung.split("\\|");
                if (parts.length >= 3) {
                    logContent = parts[0] + "|" + parts[1] + "|***HIDDEN***";
                }
            }
            System.out.println("[GUI] " + logContent);
            
        } catch (IOException e) {
            System.out.println("[LOI] Gui du lieu that bai: " + e.getMessage());
        }
    }

    private static void langNgheServer() {
        try {
            while (true) {
                String duLieu = in.readLine();
                if (duLieu == null) {
                    System.out.println("[THONG BAO] Server da dong ket noi.");
                    break;
                }
                System.out.println("[NHAN] " + duLieu);
                xuLyTinNhan(duLieu);
            }
        } catch (IOException e) {
            System.out.println("[LOI] Mat ket noi voi Server: " + e.getMessage());
        }
    }

    private static void xuLyTinNhan(String duLieu) {
        String[] parts = duLieu.split("\\|");
        String lenh = parts[0];

        switch (lenh) {
            case "LOGIN_OK":
                Platform.runLater(() -> {
                    if (loginScreen != null) loginScreen.dangNhapThanhCong(parts);
                });
                break;

            case "LOGIN_FAIL":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        String lyDo = parts.length > 1 ? parts[1] : "Sai tai khoan hoac mat khau.";
                        loginScreen.dangNhapThatBai(lyDo);
                    }
                });
                break;

            case "REGISTER_OK":
                Platform.runLater(() -> {
                    if (registerScreen != null) registerScreen.dangKyThanhCong();
                });
                break;

            case "REGISTER_FAIL":
                Platform.runLater(() -> {
                    if (registerScreen != null) {
                        String lyDo = parts.length > 1 ? parts[1] : "Dang ky khong thanh cong.";
                        registerScreen.dangKyThatBai(lyDo);
                    }
                });
                break;

            case "PLAYER_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String noiDungDanhSach = parts.length > 1 ? parts[1] : "";
                        lobbyScreen.capNhatDanhSachNguoiChoi(noiDungDanhSach);
                    }
                });
                break;

            case "HISTORY_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String noiDung = parts.length > 1 ? parts[1] : "";
                        lobbyScreen.capNhatLichSuDau(noiDung);
                    }
                });
                break;

            case "RANK_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String noiDung = parts.length > 1 ? parts[1] : "";
                        lobbyScreen.capNhatBangXepHang(noiDung);
                    }
                });
                break;

            case "INVITE_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String nguoiMoi = parts.length > 1 ? parts[1] : "";
                        lobbyScreen.nhanLoiThachDau(nguoiMoi);
                    }
                });
                break;

            case "REJECT_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String nguoiTuChoi = parts.length > 1 ? parts[1] : "";
                        lobbyScreen.loiMoiBiTuChoi(nguoiTuChoi);
                    }
                });
                break;

            case "INVITE_CANCELLED":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String nguoiHuy = parts.length > 1 ? parts[1] : "";
                        lobbyScreen.doiThuHuyLoiMoi(nguoiHuy);
                    }
                });
                break;
                
            case "INVITE_FAIL":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String lyDo = parts.length > 1 ? parts[1] : "Không thể gửi lời mời lúc này.";
                        lobbyScreen.loiThachDauThatBai(lyDo);
                    }
                });
                break;

            case "MATCH_START":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.batDauTranDau(parts); 
                    }
                    
                    gameRoomScreen = new GameRoomGUI();
                    gameStage = new Stage();
                    
                    // --- ĐOẠN THÊM MỚI: TRUYỀN TÊN THẬT VÀO GIAO DIỆN ---
                    // Tên của mình lấy từ Lobby, tên đối thủ Server gửi về ở parts[2]
                    String tenDoiThu = parts.length > 2 ? parts[2] : "Đối thủ";
                    int diemCuaMinh = 0;
                    try {
                        diemCuaMinh = Integer.parseInt(LobbyGUI.currentScore);
                    } catch (Exception ex) {}
                    
                    gameRoomScreen.setPlayerNames(LobbyGUI.currentUsername, tenDoiThu);
                    gameRoomScreen.setPlayerScores(diemCuaMinh, 0); // (Điểm đối thủ để tạm 0 nếu Server chưa gửi)
                    // ---------------------------------------------------
                    
                    gameRoomScreen.setOnExitGameClicked(() -> {
                        guiTinNhan("SURRENDER");
                        if (gameStage != null) {
                            gameStage.close();
                        }
                        try {
                            new LobbyGUI().start(new Stage());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                    
                    try {
                        gameRoomScreen.start(gameStage); 
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                break;
                
            // KHI NHẬN ĐƯỢC KẾT QUẢ VÁN ĐẤU (Kể cả người B nhận tin người A thoát)
            case "MATCH_END":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String ketQua = parts.length > 1 ? parts[1] : "LOSE";
                        String reason = parts.length > 2 ? parts[2] : ""; 
                        boolean isWinner = "WIN".equals(ketQua);
                        
                        gameRoomScreen.showResultPopup(isWinner, reason, () -> {
                            // Khi người B bấm "Về sảnh" hoặc "OK"
                            if (gameStage != null) {
                                gameStage.close();
                            }
                            try {
                                new LobbyGUI().start(new Stage());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }, null); 
                    }
                });
                break;
                
            // Xử lý thêm trường hợp đối thủ tắt nóng ứng dụng (Bấm dấu X góc màn hình)
            case "OPPONENT_QUIT":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        gameRoomScreen.showResultPopup(true, "SURRENDER", () -> {
                            if (gameStage != null) gameStage.close();
                            try { new LobbyGUI().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
                        }, null);
                    }
                });
                break;

            case "ERROR":
                Platform.runLater(() -> {
                    System.out.println("[SERVER BAO LOI] " + (parts.length > 1 ? parts[1] : ""));
                });
                break;

            default:
                System.out.println("[CANH BAO] Khong hieu lenh: " + lenh);
        }
    }

    public static void dongKetNoi() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("[LOI] Dong socket that bai: " + e.getMessage());
        }
    }
}