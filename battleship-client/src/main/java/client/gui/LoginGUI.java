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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
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

        // 1. Tựa đề
        // Luu y: JavaFX CSS khong ho tro letter-spacing, nen minh gia lap
        // hieu ung "tracking rong" bang cach chen khoang trang thu cong.
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
        formCard.getStyleClass().add("form-card");
        formCard.getChildren().addAll(titleBox, txtUsername, txtPassword, lblThongBao, btnLogin, btnRegister);
        formCard.setMaxWidth(450);

        // Khung nền Radar
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("auth-root");
        root.getChildren().add(formCard);

        Scene scene = new Scene(root, 800, 600);
        // Nạp file CSS riêng vào Scene
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

       

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // ==============================================================
    // LOGIC KẾT NỐI SERVER - GIỮ NGUYÊN, KHÔNG THAY ĐỔI
    // ==============================================================
    private void xuLyDangNhap() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            lblThongBao.getStyleClass().removeAll("message-info", "message-error");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText("Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        btnLogin.setDisable(true);
        lblThongBao.getStyleClass().removeAll("message-info", "message-error");
        lblThongBao.getStyleClass().add("message-info");
        lblThongBao.setText("Đang kết nối tới server trung tâm...");

        // Kết nối mạng bằng Thread để không đơ UI
        Thread threadKetNoi = new Thread(() -> {
            boolean ok = SocketClientManager.ketNoiServer();
            if (ok) {
                SocketClientManager.guiTinNhan("LOGIN|" + user + "|" + pass);
            } else {
                Platform.runLater(() -> {
                    lblThongBao.getStyleClass().removeAll("message-info", "message-error");
                    lblThongBao.getStyleClass().add("message-error");
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
        lblThongBao.getStyleClass().removeAll("message-info", "message-error");
        lblThongBao.getStyleClass().add("message-error");
        lblThongBao.setText(lyDo);
        txtPassword.clear();
    }
    // Hàm này được gọi từ form RegisterGUI sau khi đăng ký thành công
    public void hienThiThongBaoXanh(String thongBao) {
        lblThongBao.getStyleClass().removeAll("message-error", "message-info");
        // Bơm thẳng màu xanh lá cây vào thông báo
        lblThongBao.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        lblThongBao.setText(thongBao);
    }

    public static void main(String[] args) {
        launch(args);
    }
}