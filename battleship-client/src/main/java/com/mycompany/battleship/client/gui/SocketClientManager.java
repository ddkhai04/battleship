package com.mycompany.battleship.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.battleship.common.model.GameView;
import com.mycompany.battleship.common.model.MessageDTO;
import com.mycompany.battleship.common.model.ShotResult;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
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
    public static AdminGUI adminScreen; 

    public static GameRoomGUI gameRoomScreen;
    public static Stage gameStage;
    private static Alert waitingAlert;

    public static boolean ketNoiServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            Thread threadLangNghe = new Thread(() -> langNgheServer());
            threadLangNghe.setDaemon(true);
            threadLangNghe.start();

            return true;
        } catch (IOException e) {
            System.out.println("[LOI] Khong ket noi duoc Server: " + e.getMessage());
            return false;
        }
    }

    // Gửi tin nhắn bằng cấu trúc JSON chuẩn
    public static void guiTinNhan(MessageDTO msg) {
        try {
            if (out == null) {
                System.out.println("[LOI] Chua ket noi Server, khong gui duoc.");
                return;
            }
            String json = GSON.toJson(msg);
            out.write(json);
            out.newLine();
            out.flush();
            
            String logContent = json;
            if (json.contains("password")) {
                logContent = json.replaceFirst("\"password\":\"[^\"]*\"", "\"password\":\"***HIDDEN***\"");
            }
            System.out.println("[GUI JSON] " + logContent);
        } catch (IOException e) {
            System.out.println("[LOI] Gui du lieu that bai: " + e.getMessage());
        }
    }

    // Giữ lại hàm cũ để tránh lỗi các màn hình chưa kịp chuyển đổi sang JSON
    public static void guiTinNhan(String noiDung) {
        try {
            if (out == null) return;
            out.write(noiDung);
            out.newLine();
            out.flush();
            System.out.println("[GUI RAW] " + noiDung);
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
        Object payloadObj = null;
        boolean isJsonFormat = false;

        // Ưu tiên parse theo MessageDTO
        try {
            if (duLieu.trim().startsWith("{")) {
                MessageDTO msg = GSON.fromJson(duLieu, MessageDTO.class);
                lenh = msg.getCommand();
                payloadObj = msg.getPayload();
                isJsonFormat = true;
            }
        } catch (Exception e) {
            isJsonFormat = false;
        }

        // Fallback đọc chuỗi cũ nếu ServerGameEventListener chưa kịp sửa
        String[] parts = null;
        if (!isJsonFormat) {
            parts = duLieu.split("\\|", 2);
            lenh = parts[0];
        }

        final String command = lenh;
        final Object payload = payloadObj;
        final boolean finalIsJson = isJsonFormat;
        final String rawData = duLieu;
        final String[] stringParts = parts;

        // Tiện ích chuyển payload sang JSON String cho các màn hình tự Parse
        final String payloadJson = finalIsJson ? GSON.toJson(payload) : "";

        switch (command) {
            case "LOGIN_OK":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        if (finalIsJson && payload != null) {
                            JsonObject pObj = JsonParser.parseString(payloadJson).getAsJsonObject();
                            loginScreen.dangNhapThanhCong(pObj);
                        } else if (stringParts != null) {
                            loginScreen.dangNhapThanhCong(stringParts[1].split("\\|"));
                        }
                    }
                });
                break;

            case "LOGIN_FAIL":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        String lyDo = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "Lỗi đăng nhập.");
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
                        String lyDo = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "Lỗi đăng ký.");
                        registerScreen.dangKyThatBai(lyDo);
                    }
                });
                break;

            case "ADMIN_DATA":
                Platform.runLater(() -> {
                    if (adminScreen != null && finalIsJson) {
                        adminScreen.capNhatDuLieuAdmin(JsonParser.parseString(payloadJson).getAsJsonObject());
                    }
                });
                break;

            case "PLAYER_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        // Tạm thời truyền Json String để LobbyGUI tự xử lý
                        String noiDung = finalIsJson ? payloadJson : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        lobbyScreen.capNhatDanhSachNguoiChoi(noiDung);
                    }
                });
                break;

            case "HISTORY_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String noiDung = finalIsJson ? payloadJson : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        lobbyScreen.capNhatLichSuDau(noiDung);
                    }
                });
                break;

            case "RANK_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String noiDung = finalIsJson ? payloadJson : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        lobbyScreen.capNhatBangXepHang(noiDung);
                    }
                });
                break;

            case "INVITE_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String nguoiMoi = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        lobbyScreen.nhanLoiThachDau(nguoiMoi);
                    }
                });
                break;

            case "REJECT_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String nguoiTuChoi = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        lobbyScreen.loiMoiBiTuChoi(nguoiTuChoi);
                    }
                });
                break;

            case "INVITE_CANCELLED":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String nguoiHuy = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        lobbyScreen.doiThuHuyLoiMoi(nguoiHuy);
                    }
                });
                break;

            case "INVITE_FAIL":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        String lyDo = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "Lỗi gửi lời mời.");
                        lobbyScreen.loiThachDauThatBai(lyDo);
                    }
                });
                break;

            case "MATCH_START":
                Platform.runLater(() -> {
                    String[] mParts = rawData.split("\\|");
                    if (lobbyScreen != null) lobbyScreen.batDauTranDau(mParts);

                    gameRoomScreen = new GameRoomGUI();
                    gameStage = new Stage();

                    String tenDoiThu = mParts.length > 2 ? mParts[2] : "Đối thủ";
                    if (finalIsJson && payload != null) {
                        JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        tenDoiThu = obj.has("opponent") ? obj.get("opponent").getAsString() : "Đối thủ";
                    }

                    int diemCuaMinh = 0;
                    try {
                        diemCuaMinh = Integer.parseInt(LobbyGUI.currentScore);
                    } catch (Exception ignored) {}

                    gameRoomScreen.setPlayerNames(LobbyGUI.currentUsername, tenDoiThu);
                    gameRoomScreen.setPlayerScores(diemCuaMinh, 0);

                    // Đã chuyển sang gửi MessageDTO JSON
                    gameRoomScreen.setOnAttackCellClicked((row, col) -> {
                        String weapon = gameRoomScreen.getSelectedWeapon();
                        Map<String, Object> fireData = new HashMap<>();
                        fireData.put("type", weapon);
                        if (!"RAIN".equals(weapon)) {
                            fireData.put("row", row);
                            fireData.put("col", col);
                        }
                        guiTinNhan(new MessageDTO("FIRE", fireData));
                    });

                    gameRoomScreen.setOnExitGameClicked(() -> {
                        guiTinNhan(new MessageDTO("SURRENDER", null));
                        if (gameStage != null) gameStage.close();
                        quayVeSanh();
                    });

                    gameRoomScreen.setOnRematchClicked(() -> guiTinNhan(new MessageDTO("REMATCH", null)));

                    try {
                        gameRoomScreen.start(gameStage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                break;

            case "GAME_VIEW":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String viewData = finalIsJson ? payloadJson : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        GameView view = GSON.fromJson(viewData, GameView.class);
                        gameRoomScreen.renderGameView(view);
                    }
                });
                break;

            case "COUNTDOWN":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        int count = finalIsJson ? ((Double) payload).intValue() : (stringParts != null && stringParts.length > 1 ? Integer.parseInt(stringParts[1]) : 0);
                        gameRoomScreen.showCountdown(count);
                    }
                });
                break;

            case "TURN_START":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String turnPlayer = "";
                        int seconds = 45;
                        if (finalIsJson) {
                            JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                            turnPlayer = obj.get("turnPlayer").getAsString();
                            seconds = obj.get("seconds").getAsInt();
                        } else if (stringParts != null && stringParts.length > 1) {
                            String[] tParts = stringParts[1].split("\\|");
                            turnPlayer = tParts[0];
                            seconds = Integer.parseInt(tParts[1]);
                        }

                        gameRoomScreen.hideCountdown();
                        boolean myTurn = turnPlayer.equals(LobbyGUI.currentUsername);
                        gameRoomScreen.setTurnIndicator(myTurn);
                        gameRoomScreen.startTurnTimer(seconds);
                    }
                });
                break;

            case "SHOT_RESULT":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String shotData = finalIsJson ? payloadJson : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "");
                        ShotResult shot = GSON.fromJson(shotData, ShotResult.class);
                        gameRoomScreen.applyShotResult(shot);
                    }
                });
                break;

            case "GAME_OVER":
            case "MATCH_END":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String outcome = "LOSE";
                        String reason = "";
                        if (finalIsJson) {
                            JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                            outcome = obj.has("outcome") ? obj.get("outcome").getAsString() : "LOSE";
                            reason = obj.has("reason") ? obj.get("reason").getAsString() : "";
                        } else {
                            String[] goParts = rawData.split("\\|");
                            outcome = goParts.length > 1 ? goParts[1] : "LOSE";
                            reason = goParts.length > 2 ? goParts[2] : "";
                        }
                        
                        boolean isWinner = "WIN".equalsIgnoreCase(outcome);

                        gameRoomScreen.showResultPopup(isWinner, reason, () -> {
                            guiTinNhan(new MessageDTO("LEAVE_MATCH", null));
                            if (gameStage != null) gameStage.close();
                            quayVeSanh();
                        }, () -> {
                            guiTinNhan(new MessageDTO("REMATCH", null));
                            if (waitingAlert != null) waitingAlert.close();
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
                    String challenger = finalIsJson ? (String) payload : (stringParts != null && stringParts.length > 1 ? stringParts[1] : "Đối thủ");
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
                            guiTinNhan(new MessageDTO("REMATCH", null));
                        } else {
                            guiTinNhan(new MessageDTO("REJECT_REMATCH", null));
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
                    if (finalIsJson) {
                        if (payload instanceof String) {
                            msg = (String) payload;
                        } else if (payload != null) {
                            try {
                                msg = JsonParser.parseString(payloadJson).getAsJsonObject().get("message").getAsString();
                            } catch (Exception ignored) {}
                        }
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