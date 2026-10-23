package com.mycompany.battleship.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    public static AdminGUI adminScreen; // Quản lý màn hình Admin

    public static GameRoomGUI gameRoomScreen;
    public static Stage gameStage;
    private static Alert waitingAlert;

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
            if (noiDung.contains("LOGIN") || noiDung.contains("REGISTER")) {
                logContent = noiDung.replaceFirst("\"password\":\\s*\"[^\"]*\"", "\"password\":\"***HIDDEN***\"");
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
        String lenh = "";
        JsonObject jsonPayload = null;
        boolean isJsonFormat = false;

        // Thử parse theo chuẩn JSON mới {"command": "...", "payload": {...}}
        try {
            if (duLieu.trim().startsWith("{")) {
                JsonObject root = JsonParser.parseString(duLieu).getAsJsonObject();
                if (root.has("command")) {
                    lenh = root.get("command").getAsString();
                    if (root.has("payload") && root.get("payload").isJsonObject()) {
                        jsonPayload = root.getAsJsonObject("payload");
                    }
                    isJsonFormat = true;
                }
            }
        } catch (Exception e) {
            isJsonFormat = false;
        }

        // Nếu không phải JSON, dùng cách cắt chuỗi cũ
        String[] parts = null;
        if (!isJsonFormat) {
            parts = duLieu.split("\\|", 2);
            lenh = parts[0];
        }

        // Gán sang các biến final để sử dụng an toàn bên trong các hàm Lambda (Platform.runLater)
        final String command = lenh;
        final JsonObject payload = jsonPayload;
        final boolean finalIsJson = isJsonFormat;
        final String rawData = duLieu;
        final String[] stringParts = parts;

        switch (command) {
            case "LOGIN_OK":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        if (finalIsJson && payload != null) {
                            loginScreen.dangNhapThanhCong(payload);
                        } else if (stringParts != null) {
                            loginScreen.dangNhapThanhCong(stringParts[1].split("\\|"));
                        }
                    }
                });
                break;

            case "LOGIN_FAIL":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        String lyDo = "Sai tài khoản hoặc mật khẩu.";
                        if (finalIsJson && payload != null && payload.has("message")) {
                            lyDo = payload.get("message").getAsString();
                        } else if (stringParts != null && stringParts.length > 1) {
                            lyDo = stringParts[1];
                        }
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
                        String lyDo = "Đăng ký không thành công.";
                        if (finalIsJson && payload != null && payload.has("message")) {
                            lyDo = payload.get("message").getAsString();
                        } else if (stringParts != null && stringParts.length > 1) {
                            lyDo = stringParts[1];
                        }
                        registerScreen.dangKyThatBai(lyDo);
                    }
                });
                break;

            // Xử lý dữ liệu trả về cho giao diện ADMIN
            case "ADMIN_DATA":
                Platform.runLater(() -> {
                    if (adminScreen != null && payload != null) {
                        adminScreen.capNhatDuLieuAdmin(payload);
                    }
                });
                break;

            // Xử lý dữ liệu Xem trực tiếp (Spectate - God View)
            case "SPECTATE_VIEW":
                Platform.runLater(() -> {
                    if (payload != null) {
                        if (gameRoomScreen == null) {
                            gameRoomScreen = new GameRoomGUI();
                            gameStage = new Stage();
                            try {
                                gameRoomScreen.start(gameStage);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // gameRoomScreen.renderSpectatorView(payload); // Gọi hàm hiển thị God View trong GameRoomGUI
                    }
                });
                break;

            // Xử lý dữ liệu Xem lại trận đấu (Replay)
            case "REPLAY_DATA":
                Platform.runLater(() -> {
                    if (payload != null) {
                        ReplayGUI replayGUI = new ReplayGUI(payload);
                        try {
                            replayGUI.start(new Stage());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case "PLAYER_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String noiDungDanhSach = stringParts.length > 1 ? stringParts[1] : "";
                        lobbyScreen.capNhatDanhSachNguoiChoi(noiDungDanhSach);
                    }
                });
                break;

            case "HISTORY_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String noiDung = stringParts.length > 1 ? stringParts[1] : "";
                        lobbyScreen.capNhatLichSuDau(noiDung);
                    }
                });
                break;

            case "RANK_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String noiDung = stringParts.length > 1 ? stringParts[1] : "";
                        lobbyScreen.capNhatBangXepHang(noiDung);
                    }
                });
                break;

            case "INVITE_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String nguoiMoi = stringParts.length > 1 ? stringParts[1] : "";
                        lobbyScreen.nhanLoiThachDau(nguoiMoi);
                    }
                });
                break;

            case "REJECT_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String nguoiTuChoi = stringParts.length > 1 ? stringParts[1] : "";
                        lobbyScreen.loiMoiBiTuChoi(nguoiTuChoi);
                    }
                });
                break;

            case "INVITE_CANCELLED":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String nguoiHuy = stringParts.length > 1 ? stringParts[1] : "";
                        lobbyScreen.doiThuHuyLoiMoi(nguoiHuy);
                    }
                });
                break;

            case "INVITE_FAIL":
                Platform.runLater(() -> {
                    if (lobbyScreen != null && stringParts != null) {
                        String lyDo = stringParts.length > 1 ? stringParts[1] : "Không thể gửi lời mời lúc này.";
                        lobbyScreen.loiThachDauThatBai(lyDo);
                    }
                });
                break;

            case "MATCH_START":
                Platform.runLater(() -> {
                    String[] mParts = rawData.split("\\|");
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

                    gameRoomScreen.setOnAttackCellClicked((row, col) -> {
                        String weapon = gameRoomScreen.getSelectedWeapon();
                        if ("RAIN".equals(weapon)) {
                            guiTinNhan("FIRE|RAIN");
                        } else {
                            guiTinNhan("FIRE|" + weapon + "|" + row + "|" + col);
                        }
                    });

                    gameRoomScreen.setOnExitGameClicked(() -> {
                        guiTinNhan("SURRENDER");
                        if (gameStage != null) {
                            gameStage.close();
                        }
                        quayVeSanh();
                    });

                    gameRoomScreen.setOnRematchClicked(() -> guiTinNhan("REMATCH"));

                    try {
                        gameRoomScreen.start(gameStage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                break;

            case "GAME_VIEW":
                if (stringParts != null && stringParts.length > 1) {
                    GameView view = GSON.fromJson(stringParts[1], GameView.class);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.renderGameView(view);
                        }
                    });
                }
                break;

            case "COUNTDOWN":
                if (stringParts != null && stringParts.length > 1) {
                    int count = Integer.parseInt(stringParts[1]);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.showCountdown(count);
                        }
                    });
                }
                break;

            case "TURN_START":
                if (stringParts != null && stringParts.length > 1) {
                    String[] tParts = stringParts[1].split("\\|");
                    String turnPlayer = tParts[0];
                    int seconds = Integer.parseInt(tParts[1]);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.hideCountdown();
                            boolean myTurn = turnPlayer.equals(LobbyGUI.currentUsername);
                            gameRoomScreen.setTurnIndicator(myTurn);
                            gameRoomScreen.startTurnTimer(seconds);
                        }
                    });
                }
                break;

            case "SHOT_RESULT":
                if (stringParts != null && stringParts.length > 1) {
                    ShotResult shot = GSON.fromJson(stringParts[1], ShotResult.class);
                    Platform.runLater(() -> {
                        if (gameRoomScreen != null) {
                            gameRoomScreen.applyShotResult(shot);
                        }
                    });
                }
                break;

            case "GAME_OVER":
            case "MATCH_END":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String[] goParts = rawData.split("\\|");
                        String outcome = goParts.length > 1 ? goParts[1] : "LOSE";
                        String reason = goParts.length > 2 ? goParts[2] : "";
                        boolean isWinner = "WIN".equalsIgnoreCase(outcome);

                        gameRoomScreen.showResultPopup(isWinner, reason, () -> {
                            guiTinNhan("LEAVE_MATCH");
                            if (gameStage != null) gameStage.close();
                            quayVeSanh();
                        }, () -> {
                            guiTinNhan("REMATCH");
                            if (waitingAlert != null) {
                                waitingAlert.close();
                            }
                            waitingAlert = new Alert(Alert.AlertType.INFORMATION);
                            waitingAlert.setTitle("Đang chờ");
                            waitingAlert.setHeaderText("Đã gửi yêu cầu tái đấu!");
                            waitingAlert.setContentText("Đang chờ đối phương xác nhận...");
                            DialogStyle.apply(waitingAlert);
                            waitingAlert.show();
                        });
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
                    String[] rParts = rawData.split("\\|");
                    String challenger = rParts.length > 1 ? rParts[1] : "Đối thủ";
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Tái đấu");
                    alert.setHeaderText(challenger + " muốn tái đấu ván nữa!");
                    alert.setContentText("Bạn có đồng ý không?");
                    
                    javafx.scene.control.ButtonType btnDongY = new javafx.scene.control.ButtonType("Đồng ý", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                    javafx.scene.control.ButtonType btnTuChoi = new javafx.scene.control.ButtonType("Từ chối", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnDongY, btnTuChoi);
                    DialogStyle.apply(alert);

                    alert.showAndWait().ifPresent(ans -> {
                        if (ans == btnDongY) {
                            guiTinNhan("REMATCH");
                        } else {
                            guiTinNhan("REJECT_REMATCH");
                            if (gameStage != null) gameStage.close();
                            quayVeSanh();
                        }
                    });
                });
                break;
            
            case "REMATCH_DECLINED":
                Platform.runLater(() -> {
                    if (waitingAlert != null) {
                        waitingAlert.close();
                        waitingAlert = null;
                    }
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText("Đối thủ đã từ chối tái đấu!");
                    DialogStyle.apply(alert);
                    alert.showAndWait();
                    
                    if (gameStage != null) gameStage.close();
                    quayVeSanh();
                });
                break;  
                
            case "GAME_ERROR":
            case "ERROR":
                Platform.runLater(() -> {
                    String msg = "Đã có lỗi xảy ra.";
                    if (finalIsJson && payload != null && payload.has("message")) {
                        msg = payload.get("message").getAsString();
                    } else if (stringParts != null && stringParts.length > 1) {
                        msg = stringParts[1];
                    }
                    System.out.println("[SERVER BAO LOI] " + msg);
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText(null);
                    alert.setContentText(msg);
                    DialogStyle.apply(alert);
                    alert.show();
                });
                break;

            default:
                System.out.println("[CANH BAO] Khong hieu lenh: " + command);
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