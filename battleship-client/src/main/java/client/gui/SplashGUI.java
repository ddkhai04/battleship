package client.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.File;

/**
 * @author DINH THE LINH
 */
public class SplashGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Battleship Online - Khởi động");

        // 1. Tạo nút Bắt đầu
        Button btnStart = new Button("BẮT ĐẦU CHƠI");
        
        // CSS làm nút to, nổi bật, bo tròn giống nút Play của game
        String styleNormal = "-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 12px 40px; "
                + "-fx-background-radius: 40px; -fx-cursor: hand;";
        
        String styleHover = "-fx-background-color: #c0392b; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 12px 40px; "
                + "-fx-background-radius: 40px; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 5);";

        btnStart.setStyle(styleNormal);

        // Hiệu ứng rê chuột
        btnStart.setOnMouseEntered(e -> btnStart.setStyle(styleHover));
        btnStart.setOnMouseExited(e -> btnStart.setStyle(styleNormal));

        // 2. Chuyển sang màn hình Đăng nhập khi click
        btnStart.setOnAction(e -> {
            LoginGUI login = new LoginGUI();
            login.start(primaryStage);
        });

        // 3. Đặt ảnh nền cho giao diện
        StackPane root = new StackPane();
        
        // Đọc thẳng file từ ổ cứng
        File fileAnh = new File("src/main/resources/bg.png"); 
        
        if (!fileAnh.exists()) {
            System.out.println("[LỖI] Không tìm thấy ảnh tại: " + fileAnh.getAbsolutePath());
            root.setStyle("-fx-background-color: #2c3e50;"); 
        } else {
            String imageUrl = fileAnh.toURI().toString();
            // Thiết lập size 100% 100% để ép khít mép cửa sổ
            root.setStyle("-fx-background-image: url('" + imageUrl + "'); "
                    + "-fx-background-size: 100% 100%; "
                    + "-fx-background-position: center center; "
                    + "-fx-background-repeat: no-repeat;");
        }
        
        // Đẩy nút bấm xuống phía dưới cùng của màn hình 
        StackPane.setAlignment(btnStart, Pos.BOTTOM_CENTER);
        btnStart.setTranslateY(-30); // Căn lên 30px so với mép dưới cho vừa mắt

        root.getChildren().add(btnStart);

        // Kích thước màn hình CHÍNH XÁC với ảnh 772x433
        Scene scene = new Scene(root, 772, 433);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}