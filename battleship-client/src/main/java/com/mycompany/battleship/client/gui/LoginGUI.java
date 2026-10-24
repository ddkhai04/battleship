package com.mycompany.battleship.client.gui;

import com.google.gson.JsonObject;
import com.mycompany.battleship.common.model.MessageDTO;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author DINH THE LINH
 */
public class LoginGUI extends Application {

    private Stage primaryStage;
    private TextField txtUsername;
    private PasswordField txtPassword;
    private Label lblThongBao;
    private Button btnLogin;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Command Center");

        // Báo cho class mạng biết màn hình này đang cần nhận dữ liệu
        SocketClientManager.loginScreen = this;

        // 1. Tựa đề
        Label lblTitle = new Label("ĐĂNG NHẬP TÀI KHOẢN");
        lblTitle.getStyleClass().add("title-label");

        Label lblSubtitle = new Label("COMMAND CENTER ACCESS");
        lblSubtitle.getStyleClass().add("subtitle-label");

        VBox titleBox = new VBox(5, lblTitle, lblSubtitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        // 2. Ô nhập liệu
        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");
        txtUsername.setMaxWidth(350);
        txtUsername.getStyleClass().add("input-field");

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");
        txtPassword.setMaxWidth(350);
        txtPassword.getStyleClass().add("input-field");

        lblThongBao = new Label("");
        lblThongBao.getStyleClass().add("message-label");
        lblThongBao.setWrapText(true);
        lblThongBao.setMaxWidth(350);

        // 3. Nút bấm
        btnLogin = new Button("ĐĂNG NHẬP");
        btnLogin.setPrefWidth(350);
        btnLogin.getStyleClass().add("btn-primary");

        Button btnRegister = new Button("Chưa có tài khoản? Đăng ký ngay");
        btnRegister.getStyleClass().add("btn-link");

        // 4. Xử lý logic
        btnLogin.setOnAction(e -> xuLyDangNhap());

        btnRegister.setOnAction(e -> {
             RegisterGUI registerGUI = new RegisterGUI();
             registerGUI.start(primaryStage);
        });

        // 5. Layout Card đẹp mắt
        VBox formCard = new VBox(15);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(30, 40, 30, 40));
        formCard.getStyleClass().add("form-card");
        formCard.getChildren().addAll(titleBox, txtUsername, txtPassword, lblThongBao, btnLogin, btnRegister);
        formCard.setMaxWidth(450);

        // Khung nền Radar
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("auth-root");
        root.getChildren().add(formCard);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void xuLyDangNhap() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            lblThongBao.getStyleClass().removeAll("message-info", "message-error", "message-success");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText("Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        btnLogin.setDisable(true);
        lblThongBao.getStyleClass().removeAll("message-info", "message-error", "message-success");
        lblThongBao.getStyleClass().add("message-info");
        lblThongBao.setText("Đang kết nối tới server trung tâm...");

        Thread threadKetNoi = new Thread(() -> {
            boolean ok = SocketClientManager.ketNoiServer();
            if (ok) {
                // ĐÓNG GÓI DỮ LIỆU JSON ĐỂ GỬI ĐI
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", user);
                payload.put("password", pass);
                
                SocketClientManager.guiTinNhan(new MessageDTO("LOGIN", payload));
            } else {
                Platform.runLater(() -> {
                    lblThongBao.getStyleClass().removeAll("message-info", "message-error", "message-success");
                    lblThongBao.getStyleClass().add("message-error");
                    lblThongBao.setText("Không kết nối được Server. Kiểm tra Server đã chạy chưa!");
                    btnLogin.setDisable(false);
                });
            }
        });
        threadKetNoi.setDaemon(true);
        threadKetNoi.start();
    }

    // HÀM MỚI: Nhận dữ liệu JSON từ Server
    public void dangNhapThanhCong(JsonObject payload) {
    String username = payload.has("username") ? payload.get("username").getAsString() : "";
    String diem = payload.has("score") ? payload.get("score").getAsString() : "0";
    String role = payload.has("role") ? payload.get("role").getAsString() : "USER";

    System.out.println("Giao diện nhận Login OK: " + username + " | Quyền: " + role);
    chuyenManHinhTheoRole(username, diem, role); 
}


    

    public void dangNhapThatBai(String lyDo) {
        btnLogin.setDisable(false);
        lblThongBao.getStyleClass().removeAll("message-info", "message-error", "message-success");
        lblThongBao.getStyleClass().add("message-error");
        lblThongBao.setText(lyDo);
        txtPassword.clear();
    }

    public void hienThiThongBaoXanh(String thongBao) {
        lblThongBao.getStyleClass().removeAll("message-error", "message-info", "message-success");
        lblThongBao.getStyleClass().add("message-success");
        lblThongBao.setText(thongBao);
    }
    private void chuyenManHinhTheoRole(String username, String diem, String role) {
    if ("ADMIN".equalsIgnoreCase(role)) {
        // Mở giao diện Quản trị viên
        AdminGUI.currentAdminUsername = username;
        AdminGUI adminGUI = new AdminGUI();
        adminGUI.start(primaryStage);
    } else {
        // Mở giao diện Sảnh chờ người chơi bình thường
        LobbyGUI.currentUsername = username;
        LobbyGUI.currentScore = diem;
        LobbyGUI lobbyGUI = new LobbyGUI();
        lobbyGUI.start(primaryStage);
    }
}
    public static void main(String[] args) {
        launch(args);
    }
}