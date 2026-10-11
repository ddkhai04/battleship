package com.mycompany.battleship.client.gui;

import com.google.gson.Gson;
import com.mycompany.battleship.common.model.GameView;
import com.mycompany.battleship.common.model.ShotResult;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class SocketClientManager {

    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 2209;

    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;
    private static final Gson GSON = new Gson();

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
        String[] parts = duLieu.split("\\|", 2);
        String lenh = parts[0];

        switch (lenh) {
            case "LOGIN_OK":
                Platform.runLater(() -> {
                    if (loginScreen != null) loginScreen.dangNhapThanhCong(duLieu.split("\\|"));
                });
                break;

            case "LOGIN_FAIL":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        String[] p = duLieu.split("\\|");
                        String lyDo = p.length > 1 ? p[1] : "Sai tai khoan hoac mat khau.";
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
                        String[] p = duLieu.split("\\|");
                        String lyDo = p.length > 1 ? p[1] : "Dang ky khong thanh cong.";
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
                    String[] mParts = duLieu.split("\\|");
                    if (lobbyScreen != null) {
                        lobbyScreen.batDauTranDau(mParts);
                    }

                    gameRoomScreen = new GameRoomGUI();
                    gameStage = new Stage();

                    String tenDoiThu = mParts.length > 2 ? mParts[2] : "Đối thủ";
                    int diemCuaMinh = 0;
                    try {
                        diemCuaMinh = Integer.parseInt(LobbyGUI.currentScore);
                    } catch (Exception ignored) {}

                    gameRoomScreen.setPlayerNames(LobbyGUI.currentUsername, tenDoiThu);
                    gameRoomScreen.setPlayerScores(diemCuaMinh, 0);

                    // Gắn sự kiện bắn khi người chơi click vào ô cờ đối thủ
                    gameRoomScreen.setOnAttackCellClicked((row, col) -> {
                        String weapon = gameRoomScreen.getSelectedWeapon();
                        if ("RAIN".equals(weapon)) {
                            guiTinNhan("FIRE|RAIN");
                        } else {
                            guiTinNhan("FIRE|" + weapon + "|" + row + "|" + col);
                        }
                    });

                    // Gắn sự kiện khi bấm nút Thoát
                    gameRoomScreen.setOnExitGameClicked(() -> {
                        guiTinNhan("SURRENDER");
                        if (gameStage != null) {
                            gameStage.close();
                        }
                        quayVeSanh();
                    });

                    // Gắn sự kiện bấm nút Tái đấu
                    gameRoomScreen.setOnRematchClicked(() -> guiTinNhan("REMATCH"));

                    try {
                        gameRoomScreen.start(gameStage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                break;

            // ĐỒNG BỘ BÀN CỜ
            case "GAME_VIEW":
                if (parts.length > 1) {
                    GameView view = GSON.fromJson(parts[1], GameView.class);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.renderGameView(view);
                        }
                    });
                }
                break;

            // ĐẾM NGƯỢC 3, 2, 1
            case "COUNTDOWN":
                if (parts.length > 1) {
                    int count = Integer.parseInt(parts[1]);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.showCountdown(count);
                        }
                    });
                }
                break;

            // BẮT ĐẦU LƯỢT BẮN
            case "TURN_START":
                if (parts.length > 1) {
                    String[] tParts = parts[1].split("\\|");
                    String turnPlayer = tParts[0];
                    int seconds = Integer.parseInt(tParts[1]);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.hideCountdown(); // Tắt số 1 đè trên màn hình
                            boolean myTurn = turnPlayer.equals(LobbyGUI.currentUsername);
                            gameRoomScreen.setTurnIndicator(myTurn);
                            gameRoomScreen.startTurnTimer(seconds); // Cả 2 client cùng chạy đếm lùi
                        }
                    });
                }
                break;

            // KẾT QUẢ PHÁT BẮN
            case "SHOT_RESULT":
                if (parts.length > 1) {
                    ShotResult shot = GSON.fromJson(parts[1], ShotResult.class);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.applyShotResult(shot);
                        }
                    });
                }
                break;

            // KẾT THÚC TRẬN ĐẤU
            case "GAME_OVER":
            case "MATCH_END":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String[] goParts = duLieu.split("\\|");
                        String outcome = goParts.length > 1 ? goParts[1] : "LOSE";
                        String reason = goParts.length > 2 ? goParts[2] : "";
                        boolean isWinner = "WIN".equalsIgnoreCase(outcome);

                        gameRoomScreen.showResultPopup(isWinner, reason, () -> {
                            if (gameStage != null) gameStage.close();
                            quayVeSanh();
                        }, () -> guiTinNhan("REMATCH"));
                    }
                });
                break;

            case "OPPONENT_QUIT":
            case "PLAYER_LEFT":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        gameRoomScreen.showResultPopup(true, "SURRENDER", () -> {
                            if (gameStage != null) gameStage.close();
                            quayVeSanh();
                        }, null);
                    }
                });
                break;

            case "REMATCH_REQUESTED":
                Platform.runLater(() -> {
                    String[] rParts = duLieu.split("\\|");
                    String challenger = rParts.length > 1 ? rParts[1] : "Đối thủ";
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Tái đấu");
                    alert.setHeaderText(challenger + " muốn tái đấu ván nữa!");
                    alert.setContentText("Bạn có đồng ý không?");
                    alert.showAndWait().ifPresent(ans -> {
                        if (ans == ButtonType.OK) {
                            guiTinNhan("REMATCH");
                        }
                    });
                });
                break;

            case "GAME_ERROR":
            case "ERROR":
                Platform.runLater(() -> {
                    String msg = parts.length > 1 ? parts[1] : "Đã có lỗi xảy ra.";
                    System.out.println("[SERVER BAO LOI] " + msg);
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText(null);
                    alert.setContentText(msg);
                    alert.show();
                });
                break;

            default:
                System.out.println("[CANH BAO] Khong hieu lenh: " + lenh);
        }
    }

    private static void quayVeSanh() {
        try {
            new LobbyGUI().start(new Stage());
        } catch (Exception ex) {
            ex.printStackTrace();
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