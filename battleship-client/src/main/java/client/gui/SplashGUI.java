package client.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

/**
 * @author DINH THE LINH
 */
public class SplashGUI extends Application {

    // Dùng static để nhạc không bị tắt khi chuyển scene
    private static MediaPlayer mediaPlayer;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Battleship Online - Khởi động");

        // --- 1. BẬT NHẠC NỀN ---
        // Kiểm tra null để tránh việc nhạc phát đè lên nhau nếu mở lại màn hình này
        if (mediaPlayer == null) {
            try {
                URL musicUrl = getClass().getResource("/bg.mp3");
                if (musicUrl != null) {
                    Media media = new Media(musicUrl.toExternalForm());
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Lặp lại vô hạn
                    mediaPlayer.setVolume(0.4); // Chỉnh âm lượng (0.0 đến 1.0)
                    mediaPlayer.play();
                } else {
                    System.out.println("[LỖI] Không tìm thấy file nhạc bg.mp3");
                }
            } catch (Exception ex) {
                System.out.println("Lỗi khởi tạo nhạc: " + ex.getMessage());
            }
        }

        // --- 2. TẠO TIÊU ĐỀ GAME BATTLE-SHIP VỚI FONT CUSTOM ---
        Text gameTitle = new Text("BATTLE  SHIP");
        
        // Nạp font TitanOne-Regular.ttf từ thư mục resources
        URL fontUrl = getClass().getResource("/TitanOne-Regular.ttf");
        if (fontUrl != null) {
            Font customFont = Font.loadFont(fontUrl.toExternalForm(), 75);
            gameTitle.setFont(customFont);
        } else {
            // Backup nếu lỗi nạp font
            gameTitle.setFont(Font.font("Impact", FontWeight.BOLD, 75));
        }

        gameTitle.setFill(Color.web("#2b323c")); // Màu chữ xám đen
        gameTitle.setStroke(Color.WHITE); // Viền chữ màu trắng
        gameTitle.setStrokeWidth(5.0); // Độ dày viền trắng

        // Thêm bóng đổ phía sau viền trắng để chữ nổi khối 3D lên nền trời
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10.0);
        shadow.setOffsetX(0.0);
        shadow.setOffsetY(8.0);
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        gameTitle.setEffect(shadow);

        // --- 3. TẠO NÚT BẮT ĐẦU ---
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

        // Chuyển sang màn hình Đăng nhập khi click
        btnStart.setOnAction(e -> {
            LoginGUI login = new LoginGUI();
            login.start(primaryStage);
        });

        // --- 4. ĐẶT ẢNH NỀN VÀ GẮN COMPONENT ---
        StackPane root = new StackPane();
        
        // Ưu tiên tìm file ảnh động GIF trước
        File fileAnh = new File("src/main/resources/bg.gif"); 
        
        // Nếu không có file GIF, tự động lùi về dùng file PNG tĩnh
        if (!fileAnh.exists()) {
            fileAnh = new File("src/main/resources/bg.png"); 
        }
        
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
        
        // Căn chỉnh vị trí Tiêu đề (Nằm ở trên cùng, xích xuống một chút)
        StackPane.setAlignment(gameTitle, Pos.TOP_CENTER);
        gameTitle.setTranslateY(60); 

        // Căn chỉnh vị trí Nút bấm (Nằm ở dưới cùng, nhích lên một chút)
        StackPane.setAlignment(btnStart, Pos.BOTTOM_CENTER);
        btnStart.setTranslateY(-40); 

        // Thêm cả Tiêu đề và Nút bấm vào màn hình
        root.getChildren().addAll(gameTitle, btnStart);

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