package com.mycompany.battleship.client.gui;

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

        Font.loadFont(getClass().getResourceAsStream("/TitanOne-Regular.ttf"), 34);

        StackPane logo = new StackPane(taoIcon("pi-adduser", 34));
        logo.getStyleClass().add("pl-logo");

        Label lblTitle = new Label("BATTLESHIP");
        lblTitle.getStyleClass().add("pl-brand");

        Label lblSubtitle = new Label("T Ạ O   T À I   K H O Ả N   M Ớ I");
        lblSubtitle.getStyleClass().add("pl-auth-sub");

        VBox titleBox = new VBox(6, logo, lblTitle, lblSubtitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 10, 0));

        txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập (3-20 ký tự)...");
        txtUsername.setMaxWidth(350);
        txtUsername.getStyleClass().add("pl-input");

        // Tự động xóa viền đỏ khi người dùng bắt đầu gõ sửa lại chữ
        txtUsername.setOnKeyTyped(e -> txtUsername.getStyleClass().remove("input-error"));

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu...");
        txtPassword.setMaxWidth(350);
        txtPassword.getStyleClass().add("pl-input");

        txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Nhập lại mật khẩu...");
        txtConfirm.setMaxWidth(350);
        txtConfirm.getStyleClass().add("pl-input");

        lblThongBao = new Label("");
        lblThongBao.getStyleClass().add("message-label");
        lblThongBao.setWrapText(true);
        lblThongBao.setMaxWidth(350);

        btnRegister = new Button("ĐĂNG KÝ", taoIcon("pi-adduser", 16));
        btnRegister.setPrefWidth(350);
        btnRegister.getStyleClass().add("pl-btn-main");
        btnRegister.setOnAction(e -> xuLyDangKy());
        txtConfirm.setOnAction(e -> xuLyDangKy());   // bấm Enter ở ô cuối là đăng ký

        Button btnBack = new Button("Trở về màn hình Đăng nhập", taoIcon("pi-back", 15));
        btnBack.getStyleClass().add("pl-link");
        btnBack.setOnAction(e -> {
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        VBox formCard = new VBox(14);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(28, 42, 26, 42));
        formCard.getStyleClass().add("pl-auth-card");
        formCard.setMaxWidth(450);

        formCard.getChildren().addAll(titleBox, taoOCoIcon(txtUsername, "pi-user"), taoOCoIcon(txtPassword, "pi-lock"),
                taoOCoIcon(txtConfirm, "pi-lock"), lblThongBao, btnRegister, btnBack);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("auth-root");
        root.getChildren().add(formCard);

        Scene scene = new Scene(root, 800, 650);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/player.css").toExternalForm());

        // Sảnh / Admin đặt kích thước tối thiểu lớn; về màn này thì bỏ giới hạn đó đi
        primaryStage.setMinWidth(0);
        primaryStage.setMinHeight(0);
        primaryStage.setScene(scene);
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