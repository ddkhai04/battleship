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
        // Luu y: JavaFX CSS khong ho tro letter-spacing, nen minh gia lap
        // hieu ung "tracking rong" bang cach chen khoang trang thu cong.
        Label lblTitle = new Label("TẠO TÀI KHOẢN MỚI");
        lblTitle.getStyleClass().add("title-label");

        VBox titleBox = new VBox(lblTitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 15, 0));

        // 2. Ô nhập liệu (Đã mở rộng thành 350)
        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập (3-20 ký tự)...");
        txtUsername.setMaxWidth(350);
        txtUsername.getStyleClass().add("input-field");

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu...");
        txtPassword.setMaxWidth(350);
        txtPassword.getStyleClass().add("input-field");

        txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Nhập lại mật khẩu...");
        txtConfirm.setMaxWidth(350);
        txtConfirm.getStyleClass().add("input-field");

        lblThongBao = new Label("");
        lblThongBao.getStyleClass().add("message-label");
        lblThongBao.setWrapText(true);
        lblThongBao.setMaxWidth(350);

        // 3. Nút bấm (Đã mở rộng thành 350)
        btnRegister = new Button("ĐĂNG KÝ");
        btnRegister.setPrefWidth(350);
        btnRegister.getStyleClass().add("btn-primary");
        btnRegister.setOnAction(e -> xuLyDangKy());

        Button btnBack = new Button("Trở về màn hình Đăng nhập");
        btnBack.getStyleClass().add("btn-link");
        btnBack.setOnAction(e -> {
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        // 4. Layout Card (Đã mở rộng thành 450)
        VBox formCard = new VBox(18);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(35, 45, 35, 45));
        formCard.getStyleClass().add("form-card");
        formCard.setMaxWidth(450);

        formCard.getChildren().addAll(titleBox, txtUsername, txtPassword, txtConfirm, lblThongBao, btnRegister, btnBack);

        // Khung nền Radar
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(formCard);

        // Phóng to toàn màn hình hiển thị thành 800x650
        Scene scene = new Scene(root, 800, 650);
        // Nạp file CSS riêng vào Scene
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void xuLyDangKy() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        String confirm = txtConfirm.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            lblThongBao.getStyleClass().removeAll("message-info", "message-error");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        if (!pass.equals(confirm)) {
            lblThongBao.getStyleClass().removeAll("message-info", "message-error");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText("Mật khẩu nhập lại không khớp!");
            return;
        }

        btnRegister.setDisable(true);
        lblThongBao.getStyleClass().removeAll("message-info", "message-error");
        lblThongBao.getStyleClass().add("message-info");
        lblThongBao.setText("Đang gửi yêu cầu lên server...");

        Thread threadKetNoi = new Thread(() -> {
            boolean ok = SocketClientManager.ketNoiServer();
            if (ok) {
                SocketClientManager.guiTinNhan("REGISTER|" + user + "|" + pass);
            } else {
                Platform.runLater(() -> {
                    lblThongBao.getStyleClass().removeAll("message-info", "message-error");
                    lblThongBao.getStyleClass().add("message-error");
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
        lblThongBao.getStyleClass().removeAll("message-info", "message-error");
        lblThongBao.getStyleClass().add("message-error");
        lblThongBao.setText(lyDo);
    }
}