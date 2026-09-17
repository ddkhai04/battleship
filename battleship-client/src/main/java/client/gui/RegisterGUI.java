package client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * @author DINH THE LINH
 */
public class RegisterGUI extends Application {

    private Stage primaryStage;
    private TextField txtUsername;
    private PasswordField txtPassword;
    private PasswordField txtConfirm;
    private Label lblThongBao;
    private Button btnRegister;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Đăng ký");

        SocketClientManager.registerScreen = this;

        // 1. Tựa đề (Chữ to hơn cho cân xứng form lớn)
        Label lblTitle = new Label("TẠO TÀI KHOẢN MỚI");
        lblTitle.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 28));
        lblTitle.setStyle("-fx-text-fill: #ecf0f1; -fx-letter-spacing: 2px;");
        
        VBox titleBox = new VBox(lblTitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 15, 0));

        // 2. Ô nhập liệu (Đã mở rộng thành 350)
        String inputStyle = "-fx-padding: 12px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-color: #ffffff; -fx-font-size: 14px;";
        
        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập (3-20 ký tự)...");
        txtUsername.setMaxWidth(350);
        txtUsername.setStyle(inputStyle);

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu...");
        txtPassword.setMaxWidth(350);
        txtPassword.setStyle(inputStyle);

        txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Nhập lại mật khẩu...");
        txtConfirm.setMaxWidth(350);
        txtConfirm.setStyle(inputStyle);

        lblThongBao = new Label("");
        lblThongBao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        lblThongBao.setWrapText(true);
        lblThongBao.setMaxWidth(350);

        // 3. Nút bấm (Đã mở rộng thành 350)
        btnRegister = new Button("ĐĂNG KÝ");
        btnRegister.setPrefWidth(350);
        btnRegister.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px; -fx-background-radius: 8px; -fx-font-size: 15px; -fx-cursor: hand;");
        btnRegister.setOnAction(e -> xuLyDangKy());

        Button btnBack = new Button("Trở về màn hình Đăng nhập");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-underline: true; -fx-cursor: hand;");
        btnBack.setOnAction(e -> {
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        // 4. Layout Card (Đã mở rộng thành 450)
        VBox formCard = new VBox(18);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(35, 45, 35, 45));
        formCard.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #34495e; -fx-border-width: 2px;");
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.4));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        formCard.setEffect(shadow);
        formCard.setMaxWidth(450);
        
        formCard.getChildren().addAll(titleBox, txtUsername, txtPassword, txtConfirm, lblThongBao, btnRegister, btnBack);

        // Khung nền Radar
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a252f;");
        root.getChildren().add(formCard);

        // Phóng to toàn màn hình hiển thị thành 800x650
        Scene scene = new Scene(root, 800, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void xuLyDangKy() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        String confirm = txtConfirm.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            lblThongBao.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        if (!pass.equals(confirm)) {
            lblThongBao.setText("Mật khẩu nhập lại không khớp!");
            return;
        }

        btnRegister.setDisable(true);
        lblThongBao.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px;");
        lblThongBao.setText("Đang gửi yêu cầu lên server...");

        Thread threadKetNoi = new Thread(() -> {
            boolean ok = SocketClientManager.ketNoiServer();
            if (ok) {
                SocketClientManager.guiTinNhan("REGISTER|" + user + "|" + pass);
            } else {
                Platform.runLater(() -> {
                    lblThongBao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
                    lblThongBao.setText("Không kết nối được Server.");
                    btnRegister.setDisable(false);
                });
            }
        });
        threadKetNoi.setDaemon(true);
        threadKetNoi.start();
    }

    public void dangKyThanhCong() {
        System.out.println("Đăng ký thành công, quay về màn hình đăng nhập.");
        LoginGUI loginGUI = new LoginGUI();
        loginGUI.start(primaryStage);
    }

    public void dangKyThatBai(String lyDo) {
        btnRegister.setDisable(false);
        lblThongBao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        lblThongBao.setText(lyDo);
    }
}