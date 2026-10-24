package com.mycompany.battleship.client.gui;

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

        Label lblTitle = new Label("TẠO TÀI KHOẢN MỚI");
        lblTitle.getStyleClass().add("title-label");

        VBox titleBox = new VBox(lblTitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 15, 0));

        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập (3-20 ký tự)...");
        txtUsername.setMaxWidth(350);
        txtUsername.getStyleClass().add("input-field");

        // Tự động xóa viền đỏ khi người dùng bắt đầu gõ sửa lại chữ
        txtUsername.setOnKeyTyped(e -> txtUsername.getStyleClass().remove("input-error"));

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

        VBox formCard = new VBox(18);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(35, 45, 35, 45));
        formCard.getStyleClass().add("form-card");
        formCard.setMaxWidth(450);

        formCard.getChildren().addAll(titleBox, txtUsername, txtPassword, txtConfirm, lblThongBao, btnRegister, btnBack);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("auth-root");
        root.getChildren().add(formCard);

        Scene scene = new Scene(root, 800, 650);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void xuLyDangKy() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        String confirm = txtConfirm.getText().trim();

        txtUsername.getStyleClass().remove("input-error");

        if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            lblThongBao.getStyleClass().removeAll("message-info", "message-error");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (user.length() < 3 || user.length() > 20) {
            lblThongBao.getStyleClass().removeAll("message-info", "message-error");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText("Tên đăng nhập phải từ 3 đến 20 ký tự!");

            if (!txtUsername.getStyleClass().contains("input-error")) {
                txtUsername.getStyleClass().add("input-error");
            }
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
                // ĐÓNG GÓI DỮ LIỆU JSON ĐỂ GỬI ĐI
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", user);
                payload.put("password", pass);
                
                SocketClientManager.guiTinNhan(new MessageDTO("REGISTER", payload));
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
        Platform.runLater(() -> {
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
            loginGUI.hienThiThongBaoXanh("Bạn đã đăng ký tài khoản thành công!");
        });
    }

    public void dangKyThatBai(String lyDo) {
        Platform.runLater(() -> {
            btnRegister.setDisable(false);
            lblThongBao.getStyleClass().removeAll("message-info", "message-error");
            lblThongBao.getStyleClass().add("message-error");
            lblThongBao.setText(lyDo);
        });
    }
}