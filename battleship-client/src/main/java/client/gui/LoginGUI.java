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
        lblTitle.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 28));
        lblTitle.setStyle("-fx-text-fill: #ecf0f1; -fx-letter-spacing: 2px;");

        Label lblSubtitle = new Label("");
        lblSubtitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblSubtitle.setStyle("-fx-text-fill: #3498db; -fx-letter-spacing: 4px;");

        VBox titleBox = new VBox(5, lblTitle, lblSubtitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        // 2. Ô nhập liệu
        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");
        txtUsername.setMaxWidth(350);
        txtUsername.setStyle("-fx-padding: 12px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-color: #ffffff; -fx-font-size: 14px;");

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");
        txtPassword.setMaxWidth(350);
        txtPassword.setStyle("-fx-padding: 12px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-color: #ffffff; -fx-font-size: 14px;");

        lblThongBao = new Label("");
        lblThongBao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblThongBao.setWrapText(true);
        lblThongBao.setMaxWidth(350);

        // 3. Nút bấm
        btnLogin = new Button("ĐĂNG NHẬP");
        btnLogin.setPrefWidth(350);
        btnLogin.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px; -fx-background-radius: 8px; -fx-font-size: 14px; -fx-cursor: hand;");

        Button btnRegister = new Button("Chưa có tài khoản? Đăng ký ngay");
        btnRegister.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-font-size: 13px; -fx-underline: true; -fx-cursor: hand;");

        // 4. Xử lý logic theo kiểu sinh viên
        btnLogin.setOnAction(e -> xuLyDangNhap());

        btnRegister.setOnAction(e -> {
             // Mở form đăng ký (sẽ báo đỏ nếu bạn chưa tạo file RegisterGUI)
             RegisterGUI registerGUI = new RegisterGUI();
             registerGUI.start(primaryStage);
        });

        // 5. Layout Card đẹp mắt
        VBox formCard = new VBox(15);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(30, 40, 30, 40));
        formCard.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #34495e; -fx-border-width: 2px;");
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.4));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        formCard.setEffect(shadow);
        
        formCard.getChildren().addAll(titleBox, txtUsername, txtPassword, lblThongBao, btnLogin, btnRegister);
        formCard.setMaxWidth(450);

        // Khung nền Radar
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a252f;"); 
        root.getChildren().add(formCard);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // ==============================================================
    // LOGIC KẾT NỐI SERVER CỦA CLAUDE
    // ==============================================================
    private void xuLyDangNhap() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            lblThongBao.setText("Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        btnLogin.setDisable(true);
        lblThongBao.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px;");
        lblThongBao.setText("Đang kết nối tới server trung tâm...");

        // Kết nối mạng bằng Thread để không đơ UI
        Thread threadKetNoi = new Thread(() -> {
            boolean ok = SocketClientManager.ketNoiServer();
            if (ok) {
                SocketClientManager.guiTinNhan("LOGIN|" + user + "|" + pass);
            } else {
                Platform.runLater(() -> {
                    lblThongBao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                    lblThongBao.setText("Không kết nối được Server. Kiểm tra Server đã chạy chưa!");
                    btnLogin.setDisable(false);
                });
            }
        });
        threadKetNoi.setDaemon(true);
        threadKetNoi.start();
    }

    public void dangNhapThanhCong(String[] parts) {
        String username = parts.length > 1 ? parts[1] : "";
        String diem = parts.length > 2 ? parts[2] : "0";

        // Chuyển dữ liệu sang sảnh chờ
        LobbyGUI.currentUsername = username;
        LobbyGUI.currentScore = diem;

        // Mở màn hình Lobby (Sẽ báo đỏ nếu bạn chưa tạo LobbyGUI)
        LobbyGUI lobbyGUI = new LobbyGUI();
        lobbyGUI.start(primaryStage);
    }

    public void dangNhapThatBai(String lyDo) {
        btnLogin.setDisable(false);
        lblThongBao.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblThongBao.setText(lyDo);
        txtPassword.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }
}