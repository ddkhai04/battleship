package client.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javafx.application.Platform;

/**
 * Lớp xử lý kết nối Socket của Client.
 *
 * Gộp toàn bộ việc gửi/nhận dữ liệu vào 1 file duy nhất cho dễ giải thích:
 * - Dùng Socket + BufferedReader/BufferedWriter (đúng kiến thức Lập trình mạng).
 * - Có 1 Thread chạy vòng lặp while(true) để liên tục đọc dữ liệu từ Server.
 * - Mỗi dòng dữ liệu nhận về có dạng: LENH|thamso1|thamso2|...
 *   nên chỉ cần split("\\|") rồi switch-case theo LENH là xử lý được.
 * - Vì đang đọc dữ liệu ở Thread riêng (không phải luồng JavaFX), nên khi
 *   cần cập nhật giao diện (Label, Button, chuyển màn hình...) BẮT BUỘC phải
 *   bọc trong Platform.runLater(), nếu không sẽ bị lỗi/crash JavaFX.
 *
 * Toàn bộ project chỉ cần 1 Socket duy nhất (dùng chung từ lúc Login cho tới
 * lúc vào phòng chơi) nên mình để các biến ở dạng static, không cần tạo đối
 * tượng (new SocketClientManager()) ở đâu cả, gọi thẳng qua tên lớp.
 *
 * @author Thanh vien 4 - Client Lobby & JavaFX UI
 */
public class SocketClientManager {

    // Sửa lại IP/PORT cho đúng với máy chạy Server thật khi demo
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 5000;

    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;

    // Màn hình nào đang cần nhận thông báo từ Server thì gán chính nó vào đây.
    // Ví dụ ở LoginGUI, lúc khởi tạo giao diện ta viết:
    //      SocketClientManager.loginScreen = this;
    // Khi có kết quả server trả về, mình gọi thẳng hàm xử lý trong LoginGUI.
    public static LoginGUI loginScreen;
    public static RegisterGUI registerScreen;
    public static LobbyGUI lobbyScreen;

    /**
     * Mở kết nối tới Server. Gọi hàm này 1 lần lúc bắt đầu đăng nhập.
     * Trả về true nếu kết nối thành công.
     */
    public static boolean ketNoiServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            // Tạo 1 Thread riêng để lắng nghe Server, chạy song song với giao diện
            Thread threadLangNghe = new Thread(new Runnable() {
                @Override
                public void run() {
                    langNgheServer();
                }
            });
            threadLangNghe.setDaemon(true); // để thread tự tắt khi đóng chương trình
            threadLangNghe.start();

            return true;
        } catch (IOException e) {
            System.out.println("[LOI] Khong ket noi duoc Server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi 1 dòng dữ liệu (lệnh) xuống Server.
     * Ví dụ: guiTinNhan("LOGIN|linh|123456");
     */
    public static void guiTinNhan(String noiDung) {
        try {
            if (out == null) {
                System.out.println("[LOI] Chua ket noi Server, khong gui duoc.");
                return;
            }
            out.write(noiDung);
            out.newLine();
            out.flush();
            System.out.println("[GUI] " + noiDung);
        } catch (IOException e) {
            System.out.println("[LOI] Gui du lieu that bai: " + e.getMessage());
        }
    }

    /**
     * Vòng lặp lắng nghe Server, chạy liên tục trong Thread riêng.
     * Đây là phần "cốt lõi" của Lập trình mạng: đọc từng dòng text server gửi về.
     */
    private static void langNgheServer() {
        try {
            while (true) {
                String duLieu = in.readLine();

                // Server đóng kết nối thì readLine() trả về null -> thoát vòng lặp
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

    /**
     * Tách dữ liệu server gửi về thành từng phần rồi xử lý bằng switch-case.
     * Định dạng: LENH|thamso1|thamso2|...
     */
    private static void xuLyTinNhan(String duLieu) {
        String[] parts = duLieu.split("\\|");
        String lenh = parts[0];

        switch (lenh) {

            // ================= LOGIN =================
            case "LOGIN_OK":
                // parts[1] = username, parts[2] = diem
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (loginScreen != null) {
                            loginScreen.dangNhapThanhCong(parts);
                        }
                    }
                });
                break;

            case "LOGIN_FAIL":
                // parts[1] = ly do
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (loginScreen != null) {
                            String lyDo = parts.length > 1 ? parts[1] : "Sai tai khoan hoac mat khau.";
                            loginScreen.dangNhapThatBai(lyDo);
                        }
                    }
                });
                break;

            // ================= REGISTER =================
            case "REGISTER_OK":
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (registerScreen != null) {
                            registerScreen.dangKyThanhCong();
                        }
                    }
                });
                break;

            case "REGISTER_FAIL":
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (registerScreen != null) {
                            String lyDo = parts.length > 1 ? parts[1] : "Dang ky khong thanh cong.";
                            registerScreen.dangKyThatBai(lyDo);
                        }
                    }
                });
                break;

            // ================= LOBBY: danh sach nguoi choi =================
            case "PLAYER_LIST":
                // parts[1] = "user1,status,diem;user2,status,diem;..."
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lobbyScreen != null) {
                            String noiDungDanhSach = parts.length > 1 ? parts[1] : "";
                            lobbyScreen.capNhatDanhSachNguoiChoi(noiDungDanhSach);
                        }
                    }
                });
                break;

            // ================= LOBBY: lich su dau =================
            case "HISTORY_LIST":
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lobbyScreen != null) {
                            String noiDung = parts.length > 1 ? parts[1] : "";
                            lobbyScreen.capNhatLichSuDau(noiDung);
                        }
                    }
                });
                break;

            // ================= LOBBY: bang xep hang =================
            case "RANK_LIST":
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lobbyScreen != null) {
                            String noiDung = parts.length > 1 ? parts[1] : "";
                            lobbyScreen.capNhatBangXepHang(noiDung);
                        }
                    }
                });
                break;

            // ================= THACH DAU =================
            case "INVITE_FROM":
                // parts[1] = nguoi moi, parts[2] = diem cua nguoi moi
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lobbyScreen != null) {
                            String nguoiMoi = parts.length > 1 ? parts[1] : "";
                            lobbyScreen.nhanLoiThachDau(nguoiMoi);
                        }
                    }
                });
                break;

            case "INVITE_REJECTED":
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lobbyScreen != null) {
                            String nguoiTuChoi = parts.length > 1 ? parts[1] : "";
                            lobbyScreen.loiMoiBiTuChoi(nguoiTuChoi);
                        }
                    }
                });
                break;

            case "MATCH_START":
                // parts[1] = roomId, parts[2] = doithu, parts[3] = nguoiDiTruoc
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lobbyScreen != null) {
                            lobbyScreen.batDauTranDau(parts);
                        }
                    }
                });
                break;

            case "ERROR":
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("[SERVER BAO LOI] " + (parts.length > 1 ? parts[1] : ""));
                    }
                });
                break;

            default:
                System.out.println("[CANH BAO] Khong hieu lenh: " + lenh);
        }
    }

    /** Đóng kết nối khi thoát chương trình hoặc đăng xuất. */
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