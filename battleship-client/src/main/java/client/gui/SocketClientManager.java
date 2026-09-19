package client.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javafx.application.Platform;

/**
 * @author Thanh vien 4 - Client Lobby & JavaFX UI
 */
public class SocketClientManager {

    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 2209;

    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;

    public static LoginGUI loginScreen;
    public static RegisterGUI registerScreen;
    public static LobbyGUI lobbyScreen;

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
            
            // --- FIX BẢO MẬT: CHE MẬT KHẨU KHI IN RA CONSOLE ---
            String logContent = noiDung;
            if (noiDung.startsWith("LOGIN|") || noiDung.startsWith("REGISTER|")) {
                String[] parts = noiDung.split("\\|");
                if (parts.length >= 3) {
                    // Giữ lại phần tử 0 (Lệnh) và phần tử 1 (Tên đăng nhập), che phần tử 2 (Mật khẩu)
                    logContent = parts[0] + "|" + parts[1] + "|***HIDDEN***";
                }
            }
            System.out.println("[GUI] " + logContent);
            // ---------------------------------------------------
            
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
                
            // THÊM MỚI: Xử lý khi đối thủ bận hoặc offline
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