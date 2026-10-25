package com.mycompany.battleship.client.gui;

import com.google.gson.JsonObject;
import com.mycompany.battleship.common.model.MessageDTO;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

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

        // Nạp font tiêu đề "BATTLESHIP"
        Font.loadFont(getClass().getResourceAsStream("/TitanOne-Regular.ttf"), 34);

        // 1. Logo + tựa đề
        StackPane logo = new StackPane(taoIcon("pi-target", 36));
        logo.getStyleClass().add("pl-logo");

        Label lblTitle = new Label("BATTLESHIP");
        lblTitle.getStyleClass().add("pl-brand");

        // JavaFX CSS không có letter-spacing nên chèn khoảng trắng thủ công
        Label lblSubtitle = new Label("Đ Ă N G   N H Ậ P   ·   R A   K H Ơ I");
        lblSubtitle.getStyleClass().add("pl-auth-sub");

        VBox titleBox = new VBox(6, logo, lblTitle, lblSubtitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 14, 0));

        // 2. Ô nhập liệu (có icon bên trái)
        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");
        txtUsername.setMaxWidth(350);
        txtUsername.getStyleClass().add("pl-input");

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");
        txtPassword.setMaxWidth(350);
        txtPassword.getStyleClass().add("pl-input");

        lblThongBao = new Label("");
        lblThongBao.getStyleClass().add("message-label");
        lblThongBao.setWrapText(true);
        lblThongBao.setMaxWidth(350);

        // 3. Nút bấm
        btnLogin = new Button("ĐĂNG NHẬP", taoIcon("pi-login", 16));
        btnLogin.setPrefWidth(350);
        btnLogin.getStyleClass().add("pl-btn-main");

        Button btnRegister = new Button("Chưa có tài khoản? Đăng ký ngay", taoIcon("pi-adduser", 15));
        btnRegister.getStyleClass().add("pl-link");

        // 4. Xử lý logic
        btnLogin.setOnAction(e -> xuLyDangNhap());
        txtPassword.setOnAction(e -> xuLyDangNhap());   // gõ xong mật khẩu bấm Enter là đăng nhập

        btnRegister.setOnAction(e -> {
             RegisterGUI registerGUI = new RegisterGUI();
             registerGUI.start(primaryStage);
        });

        // 5. Thẻ kính mờ chứa form
        VBox formCard = new VBox(14);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(30, 40, 28, 40));
        formCard.getStyleClass().add("pl-auth-card");
        formCard.getChildren().addAll(titleBox, taoOCoIcon(txtUsername, "pi-user"), taoOCoIcon(txtPassword, "pi-lock"),
                lblThongBao, btnLogin, btnRegister);
        formCard.setMaxWidth(450);

        // Khung nền (ảnh bg1.gif lấy từ style.css)
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("auth-root");
        root.getChildren().add(formCard);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/player.css").toExternalForm());

        // Sảnh / Admin đặt kích thước tối thiểu lớn; về màn này thì bỏ giới hạn đó đi
        primaryStage.setMinWidth(0);
        primaryStage.setMinHeight(0);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        hieuUngTruotLen(formCard);
    }

    /** Tạo icon vẽ bằng đường SVG (hình dạng nằm trong player.css). */
    private Region taoIcon(String loai, double co) {
        Region icon = new Region();
        icon.getStyleClass().addAll("pl-icon", loai);
        icon.setMinSize(co, co);
        icon.setPrefSize(co, co);
        icon.setMaxSize(co, co);
        return icon;
    }

    /** Đặt icon chồng lên bên trái ô nhập (ô nhập đã chừa lề trái trong CSS). */
    private StackPane taoOCoIcon(TextField o, String icon) {
        Region ic = taoIcon(icon, 16);
        ic.getStyleClass().add("pl-input-icon");
        ic.setMouseTransparent(true);
        StackPane khung = new StackPane(o, ic);
        StackPane.setAlignment(ic, Pos.CENTER_LEFT);
        StackPane.setMargin(ic, new Insets(0, 0, 0, 17));
        khung.setMaxWidth(350);
        return khung;
    }

    /** Thẻ form trượt nhẹ từ dưới lên và hiện dần khi mở màn hình. */
    private void hieuUngTruotLen(VBox the) {
        the.setOpacity(0);
        FadeTransition hien = new FadeTransition(Duration.millis(500), the);
        hien.setToValue(1);
        TranslateTransition truot = new TranslateTransition(Duration.millis(500), the);
        truot.setFromY(30);
        truot.setToY(0);
        new ParallelTransition(hien, truot).play();
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