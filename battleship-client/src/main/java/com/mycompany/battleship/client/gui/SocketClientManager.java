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
    public static ProfileGUI profileScreen;
    

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
        MessageDTO msg;
        try {
            msg = GSON.fromJson(duLieu, MessageDTO.class);
            if (msg == null || msg.getCommand() == null) return;
        } catch (Exception e) {
            System.out.println("[LOI PARSE JSON] " + duLieu);
            return;
        }

        final String command = msg.getCommand();
        final Object payload = msg.getPayload();
        final String payloadJson = payload != null ? GSON.toJson(payload) : "";

        switch (command) {
            case "LOGIN_OK":
                Platform.runLater(() -> {
                    if (loginScreen != null && payload != null) {
                        JsonObject pObj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        loginScreen.dangNhapThanhCong(pObj);
                    }
                });
                break;

            case "LOGIN_FAIL":
                Platform.runLater(() -> {
                    if (loginScreen != null) {
                        loginScreen.dangNhapThatBai(payload != null ? payload.toString() : "Lỗi đăng nhập.");
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
                        registerScreen.dangKyThatBai(payload != null ? payload.toString() : "Lỗi đăng ký.");
                    }
                });
                break;

            case "PROFILE_DATA":
                Platform.runLater(() -> {
                    if (profileScreen != null && payload != null) {
                        profileScreen.hienHoSo(GSON.fromJson(payloadJson, com.mycompany.battleship.common.model.ProfileDTO.class));
                    }
                });
                break;

            case "ADMIN_DATA":
                Platform.runLater(() -> {
                    if (adminScreen != null && payload != null) {
                        adminScreen.capNhatDuLieuAdmin(JsonParser.parseString(payloadJson).getAsJsonObject());
                    }
                });
                break;

            case "PLAYER_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.capNhatDanhSachNguoiChoi(payloadJson);
                    }
                });
                break;

            case "HISTORY_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.capNhatLichSuDau(payloadJson);
                    }
                });
                break;

            case "RANK_LIST":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.capNhatBangXepHang(payloadJson);
                    }
                });
                break;

            case "SPECTATE_UPDATE":
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    com.google.gson.JsonObject spectateData = gson.toJsonTree(msg.getPayload()).getAsJsonObject();
                    javafx.application.Platform.runLater(() -> {
                        SpectateGUI.moHoacCapNhat(spectateData);
                    });
                } catch (Exception e) {
                    System.err.println("Lỗi cập nhật Spectate: " + e.getMessage());
                }
                break;

            case "INVITE_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.nhanLoiThachDau(payload != null ? payload.toString() : "");
                    }
                });
                break;

            case "REJECT_FROM":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.loiMoiBiTuChoi(payload != null ? payload.toString() : "");
                    }
                });
                break;

            case "INVITE_CANCELLED":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.doiThuHuyLoiMoi(payload != null ? payload.toString() : "");
                    }
                });
                break;

            case "INVITE_FAIL":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.loiThachDauThatBai(payload != null ? payload.toString() : "Lỗi gửi lời mời.");
                    }
                });
                break;

            case "MATCH_START":
                Platform.runLater(() -> {
                    if (lobbyScreen != null) {
                        lobbyScreen.batDauTranDau();
                    }
                    if (gameStage != null) {
                        gameStage.close();
                    }
                    if (waitingAlert != null) {
                        waitingAlert.close();
                        waitingAlert = null;
                    }

                    gameRoomScreen = new GameRoomGUI();
                    gameStage = new Stage();

                    String tenDoiThu = "Đối thủ";
                    int setupSeconds = 45;
                    if (payload != null) {
                        JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        if (obj.has("opponent")) {
                            tenDoiThu = obj.get("opponent").getAsString();
                        }
                        if (obj.has("setupSeconds")) {
                            setupSeconds = obj.get("setupSeconds").getAsInt();
                        }
                    }

                    int diemCuaMinh = 0;
                    try {
                        diemCuaMinh = Integer.parseInt(LobbyGUI.currentScore);
                    } catch (Exception ignored) {}

                    gameRoomScreen.setPlayerNames(LobbyGUI.currentUsername, tenDoiThu);
                    gameRoomScreen.setPlayerScores(diemCuaMinh, 0);

                    gameRoomScreen.setOnShipsPlaced((shipsData, isReady) -> {
                        Map<String, Object> placePayload = new HashMap<>();
                        placePayload.put("ships", shipsData);
                        placePayload.put("ready", isReady);
                        guiTinNhan(new MessageDTO("PLACE_SHIPS", placePayload));
                    });

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
                        if (gameStage != null) {
                            gameStage.close();
                            gameStage = null;
                        }
                        quayVeSanh();
                    });

                    gameRoomScreen.setOnRematchClicked(() -> guiTinNhan(new MessageDTO("REMATCH", null)));

                    try {
                        gameRoomScreen.start(gameStage);
                        gameRoomScreen.startSetupPhase(setupSeconds);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                break;

            case "SETUP_STATUS":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null && payload != null) {
                        JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        String readyPlayer = obj.has("player") ? obj.get("player").getAsString() : "";
                        if (!readyPlayer.equalsIgnoreCase(LobbyGUI.currentUsername)) {
                            gameRoomScreen.onOpponentReady();
                        }
                    }
                });
                break;

            case "GAME_VIEW":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        GameView view = GSON.fromJson(payloadJson, GameView.class);
                        gameRoomScreen.renderGameView(view);
                    }
                });
                break;

            case "COUNTDOWN":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null && payload != null) {
                        int count = ((Double) payload).intValue();
                        gameRoomScreen.showCountdown(count);
                    }
                });
                break;

            case "TURN_START":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null && payload != null) {
                        JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        String turnPlayer = obj.get("turnPlayer").getAsString();
                        int seconds = obj.get("seconds").getAsInt();

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
                        ShotResult shot = GSON.fromJson(payloadJson, ShotResult.class);
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
                        if (payload != null) {
                            JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                            outcome = obj.has("outcome") ? obj.get("outcome").getAsString() : "LOSE";
                            reason = obj.has("reason") ? obj.get("reason").getAsString() : "";
                        }

                        boolean isWinner = "WIN".equalsIgnoreCase(outcome);

                        gameRoomScreen.showResultPopup(isWinner, reason, () -> {
                            guiTinNhan(new MessageDTO("LEAVE_MATCH", null));
                            if (gameStage != null) {
                                gameStage.close();
                                gameStage = null;
                            }
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

            case "REPLAY_DATA":
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    com.google.gson.JsonObject replayData = gson.toJsonTree(msg.getPayload()).getAsJsonObject();
                    javafx.application.Platform.runLater(() -> {
                        try {
                            ReplayGUI replayGUI = new ReplayGUI(replayData);
                            replayGUI.show();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case "OPPONENT_QUIT":
            case "PLAYER_LEFT":
            case "ROOM_CLOSED":
                Platform.runLater(() -> {
                    // Đóng các hộp thoại chờ nếu đang mở
                    if (waitingAlert != null && waitingAlert.isShowing()) {
                        waitingAlert.close();
                        waitingAlert = null;
                    }

                    // Tắt màn hình đếm ngược hoặc kết quả nếu có
                    if (gameRoomScreen != null) {
                        try {
                            gameRoomScreen.hideCountdown();
                            gameRoomScreen.hideResultPopup();
                        } catch (Exception ignored) {}
                    }

                    // Hiển thị thông báo trận đấu bị hủy
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText("Trận đấu đã kết thúc / bị hủy!");
                    
                    String reasonText = "Đối thủ đã rời phòng.";
                    if (payload != null && payload instanceof com.google.gson.internal.LinkedTreeMap) {
                        try {
                            JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                            if (obj.has("reason")) reasonText = "Lý do: " + obj.get("reason").getAsString();
                        } catch (Exception ignored) {}
                    }
                    alert.setContentText(reasonText + " Đang quay về sảnh...");
                    
                    try { DialogStyle.apply(alert); } catch (Exception ignored) {}
                    alert.showAndWait();

                    // Đóng cửa sổ game và dọn dẹp để về sảnh
                    if (gameStage != null) {
                        gameStage.close();
                        gameStage = null;
                    }
                    quayVeSanh();
                });
                break;

            case "REMATCH_REQUESTED":
                Platform.runLater(() -> {
                    String challenger = payload != null ? payload.toString() : "Đối thủ";
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Tái đấu");
                    alert.setHeaderText(challenger + " muốn tái đấu ván nữa!");
                    alert.setContentText("Bạn có đồng ý không (tự động hủy sau 30s)?");

                    javafx.scene.control.ButtonType btnDongY = new javafx.scene.control.ButtonType("Đồng ý", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                    javafx.scene.control.ButtonType btnTuChoi = new javafx.scene.control.ButtonType("Từ chối", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnDongY, btnTuChoi);
                    DialogStyle.apply(alert);

                    // Sử dụng setResultConverter để xử lý sự kiện không chặn luồng
                    alert.setResultConverter(ans -> {
                        if (ans == btnDongY) {
                            guiTinNhan(new MessageDTO("REMATCH", null));
                        } else {
                            guiTinNhan(new MessageDTO("REJECT_REMATCH", null));
                            if (gameStage != null) {
                                gameStage.close();
                                gameStage = null;
                            }
                            quayVeSanh();
                        }
                        return ans;
                    });
                    
                    alert.show(); // Hiển thị không chặn luồng
                });
                break;

            case "REMATCH_DECLINED":
                Platform.runLater(() -> {
                    if (waitingAlert != null) {
                        waitingAlert.close();
                        waitingAlert = null;
                    }
                    String msgDecline = (payload instanceof String && !((String) payload).isEmpty() && !((String) payload).equals(LobbyGUI.currentUsername))
                            ? (String) payload
                            : "Đối thủ đã từ chối tái đấu!";
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText(msgDecline);
                    DialogStyle.apply(alert);
                    alert.showAndWait();

                    if (gameStage != null) {
                        gameStage.close();
                        gameStage = null;
                    }
                    quayVeSanh();
                });
                break;

            case "HEATMAP_DATA":
                Platform.runLater(() -> {
                    if (payload != null) {
                        JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        if (adminScreen != null) {
                            adminScreen.capNhatHeatmap(obj);
                        } else {
                            HeatmapDialog.hienThi(obj);
                        }
                    }
                });
                break;

            case "GAME_ERROR":
            case "ERROR":
                Platform.runLater(() -> {
                    String errMsg = "Đã có lỗi xảy ra.";
                    if (payload instanceof String) {
                        errMsg = (String) payload;
                    } else if (payload != null) {
                        try {
                            errMsg = JsonParser.parseString(payloadJson).getAsJsonObject().get("message").getAsString();
                        } catch (Exception ignored) {}
                    }
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText(null);
                    alert.setContentText(errMsg);
                    DialogStyle.apply(alert);
                    alert.show();
                });
                break;
                case "CHAT":
                // Tin chat trong trận: {senderId, text, timestamp} - server gửi cho cả 2 người chơi
                Platform.runLater(() -> {
                    if (gameRoomScreen != null && payload != null) {
                        JsonObject obj = JsonParser.parseString(payloadJson).getAsJsonObject();
                        String nguoiGui = obj.has("senderId") ? obj.get("senderId").getAsString() : "";
                        String noiDung = obj.has("text") ? obj.get("text").getAsString() : "";
                        long thoiGian = obj.has("timestamp") ? obj.get("timestamp").getAsLong() : 0;
                        gameRoomScreen.nhanTinNhanChat(nguoiGui, noiDung, thoiGian);
                    }
                });
                break;

                case "CHAT_ERROR":
                Platform.runLater(() -> {
                    if (gameRoomScreen != null) {
                        String loi = (payload instanceof String) ? (String) payload : "Không gửi được tin nhắn.";
                        gameRoomScreen.baoLoiChat(loi);
                    }
                });
                break;
            default:
                System.out.println("[CANH BAO] Khong hieu lenh: " + command);
        }
    }

    private static void quayVeSanh() {
        try {
            gameRoomScreen = null;
            if (lobbyScreen != null) {
                lobbyScreen.batDauTranDau();
            }
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