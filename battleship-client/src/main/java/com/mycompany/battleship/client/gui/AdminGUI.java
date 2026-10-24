package com.mycompany.battleship.client.gui;

import com.mycompany.battleship.common.model.MessageDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * @author DINH THE LINH / AI Collaborator
 */
public class AdminGUI extends Application {

    public static String currentAdminUsername = "";

    private Stage primaryStage;
    private Label lblUser;

    // Bảng 1: Quản lý tài khoản
    private TableView<String[]> tblUsers;
    private ObservableList<String[]> dsUsers = FXCollections.observableArrayList();

    // Bảng 2: Giám sát trận đấu đang diễn ra (ONGOING)
    private TableView<String[]> tblMatches;
    private ObservableList<String[]> dsMatches = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Admin Command Center");

        // Đăng ký màn hình Admin với SocketClientManager
        SocketClientManager.adminScreen = this;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("lobby-root");
        root.setTop(taoThanhTieuDe());

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(taoTabQuanLyTaiKhoan());
        tabPane.getTabs().add(taoTabGiamSatTranDau());
        tabPane.getTabs().add(taoTabBieuDoNhiet());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("lobby-tabs");

        // Lắng nghe sự kiện đổi tab để yêu cầu dữ liệu mới từ Server
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                yeuCauDuLieuAdmin();
            }
        });

        BorderPane.setMargin(tabPane, new Insets(20, 28, 28, 28));
        root.setCenter(tabPane);

        // Lấy dữ liệu lần đầu tiên khi mở màn hình Admin
        yeuCauDuLieuAdmin();

        Scene scene = new Scene(root, 950, 600);
        scene.getStylesheets().add(duongDanCss());

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private String duongDanCss() {
        return getClass().getResource("/game-style.css").toExternalForm();
    }

    private void apDungStyle(Alert alert) {
        DialogStyle.apply(alert);
    }

    private <T> void tuyChinhBang(TableView<T> bang) {
        bang.getStyleClass().add("lobby-table");
        bang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bang.setPlaceholder(new Label("Đang tải dữ liệu từ trung tâm..."));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(bang.widthProperty());
        clip.heightProperty().bind(bang.heightProperty());
        bang.setClip(clip);

        VBox.setVgrow(bang, Priority.ALWAYS);
    }

    private HBox taoThanhCongCu(javafx.scene.Node... nodes) {
        HBox toolbar = new HBox(10, nodes);
        toolbar.setPadding(new Insets(16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private VBox taoNoiDungTab(HBox toolbar, TableView<?> bang) {
        VBox.setMargin(bang, new Insets(0, 16, 16, 16));
        return new VBox(toolbar, bang);
    }

    private HBox taoThanhTieuDe() {
        String ten = currentAdminUsername == null ? "Admin" : currentAdminUsername;

        Circle avatarBg = new Circle(20);
        avatarBg.getStyleClass().addAll("player-avatar", "avatar-color-1");
        Label initial = new Label("A");
        initial.getStyleClass().add("player-avatar-initial");
        StackPane avatar = new StackPane(avatarBg, initial);

        lblUser = new Label(ten + " (QUẢN TRỊ VIÊN)");
        lblUser.getStyleClass().add("player-name");

        Label lblBadge = new Label("🛡️ ADMIN");
        lblBadge.getStyleClass().add("score-badge");

        VBox textBox = new VBox(3, lblUser, lblBadge);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox userBox = new HBox(10, avatar, textBox);
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.setPrefWidth(300);

        Label lblTitle = new Label("BATTLE SHIP");
        lblTitle.getStyleClass().add("lobby-title");
        Label lblSub = new Label("COMMAND CENTER");
        lblSub.getStyleClass().add("lobby-subtitle");
        VBox titleBox = new VBox(2, lblTitle, lblSub);
        titleBox.setAlignment(Pos.CENTER);
        
        Button btnLogout = new Button("Đăng xuất");
        btnLogout.getStyleClass().add("lobby-btn-exit");
        btnLogout.setOnAction(e -> {
            SocketClientManager.guiTinNhan(new MessageDTO("LOGOUT", null));
            SocketClientManager.dongKetNoi();
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        HBox rightBox = new HBox(btnLogout);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setPrefWidth(300);

        Region spacerLeft = new Region();
        Region spacerRight = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        HBox box = new HBox(userBox, spacerLeft, titleBox, spacerRight, rightBox);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 28, 14, 28));
        box.getStyleClass().add("top-bar");
        return box;
    }
   
    // --- TAB 1: QUẢN LÝ TÀI KHOẢN ---
    private Tab taoTabQuanLyTaiKhoan() {
        Button btnRefresh = new Button("Làm mới danh sách");
        btnRefresh.getStyleClass().add("lobby-btn-primary");
        btnRefresh.setOnAction(e -> yeuCauDuLieuAdmin());

        HBox toolbar = taoThanhCongCu(btnRefresh);

        tblUsers = new TableView<>();
        tuyChinhBang(tblUsers);

        TableColumn<String[], String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colId.setMaxWidth(60);

        TableColumn<String[], String> colUser = new TableColumn<>("Tên đăng nhập");
        colUser.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> colNick = new TableColumn<>("Biệt danh (Nickname)");
        colNick.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> colRole = new TableColumn<>("Quyền hạn");
        colRole.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));

        TableColumn<String[], String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[4]));

        TableColumn<String[], String> colScore = new TableColumn<>("Điểm");
        colScore.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[5]));
        colScore.setMaxWidth(80);

        TableColumn<String[], String> colRecord = new TableColumn<>("Thắng / Thua");
        colRecord.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[6] + " / " + data.getValue()[7]));
        colRecord.setMaxWidth(100);

        tblUsers.getColumns().addAll(colId, colUser, colNick, colRole, colStatus, colScore, colRecord);
        tblUsers.setItems(dsUsers);

        return new Tab("Quản lý tài khoản", taoNoiDungTab(toolbar, tblUsers));
    }
    
    // 2 lưới hiển thị nhiệt độ (10x10 ô)
    private GridPane gridPlacementHeatmap = new GridPane();
    private GridPane gridShotHeatmap = new GridPane();

    private Tab taoTabBieuDoNhiet() {
        Button btnRefresh = new Button("Làm mới biểu đồ");
        btnRefresh.getStyleClass().add("lobby-btn-primary");
        btnRefresh.setOnAction(e -> {
            SocketClientManager.guiTinNhan(new MessageDTO("GET_HEATMAP", null));
        });

        HBox toolbar = taoThanhCongCu(btnRefresh);

        // Khởi tạo lưới 10x10 rỗng
        khoiTaoLuoiHeatmap(gridPlacementHeatmap);
        khoiTaoLuoiHeatmap(gridShotHeatmap);

        Label lblPlacement = new Label("🔥 TẦN SUẤT ĐẶT TÀU (SHIP PLACEMENT)");
        lblPlacement.setStyle("-fx-text-fill: #00E5FF; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblShot = new Label("🎯 TẦN SUẤT BẮN PHÁ (SHOT TARGET)");
        lblShot.setStyle("-fx-text-fill: #FF5252; -fx-font-weight: bold; -fx-font-size: 14px;");

        VBox boxPlacement = new VBox(10, lblPlacement, gridPlacementHeatmap);
        boxPlacement.setAlignment(Pos.CENTER);

        VBox boxShot = new VBox(10, lblShot, gridShotHeatmap);
        boxShot.setAlignment(Pos.CENTER);

        HBox heatmapsBox = new HBox(40, boxPlacement, boxShot);
        heatmapsBox.setAlignment(Pos.CENTER);
        heatmapsBox.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(heatmapsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(toolbar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return new Tab("📊 Biểu đồ nhiệt", content);
    }

    private void khoiTaoLuoiHeatmap(GridPane grid) {
        grid.getChildren().clear();
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Label cell = new Label("0");
                cell.setPrefSize(35, 35);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle("-fx-background-color: #1A2634; -fx-text-fill: #78909C; -fx-border-color: #263238; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11px;");
                grid.add(cell, c, r);
            }
        }
    }

    // --- TAB 2: GIÁM SÁT TRẬN ĐẤU & XEM TRỰC TIẾP (SPECTATE) ---
    private Tab taoTabGiamSatTranDau() {
        Button btnRefresh = new Button("Làm mới phòng");
        btnRefresh.getStyleClass().add("lobby-btn-primary");
        btnRefresh.setOnAction(e -> yeuCauDuLieuAdmin());

        HBox toolbar = taoThanhCongCu(btnRefresh);

        tblMatches = new TableView<>();
        tuyChinhBang(tblMatches);

        TableColumn<String[], String> colMatchId = new TableColumn<>("Mã trận");
        colMatchId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colMatchId.setMaxWidth(80);

        TableColumn<String[], String> colP1 = new TableColumn<>("Người chơi 1 (P1)");
        colP1.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> colP2 = new TableColumn<>("Người chơi 2 (P2)");
        colP2.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));

        TableColumn<String[], Void> colAction = new TableColumn<>("Hành động");
        colAction.setPrefWidth(150);
        colAction.setCellFactory(col -> new TableCell<String[], Void>() {
            private final Button btnSpectate = new Button("👁️ Xem trực tiếp");
            {
                btnSpectate.getStyleClass().add("lobby-btn-challenge");
                btnSpectate.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    String roomId = row[0]; // Hoặc matchId tùy cách truyền từ server
                    
                    // Gửi lệnh kết nối Spectate lên server
                    // Gửi lệnh kết nối Spectate lên server
                    java.util.Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("roomId", roomId);
                    SocketClientManager.guiTinNhan(new MessageDTO("SPECTATE_JOIN", payload));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnSpectate);
                }
            }
        });

        tblMatches.getColumns().addAll(colMatchId, colP1, colP2, colStatus, colAction);
        tblMatches.setItems(dsMatches);

        return new Tab("Giám sát trận đấu (Spectate)", taoNoiDungTab(toolbar, tblMatches));
    }

    private void yeuCauDuLieuAdmin() {
        SocketClientManager.guiTinNhan(new MessageDTO("GET_ADMIN_DATA", null));
    }
    public void capNhatHeatmap(JsonObject payload) {
        Platform.runLater(() -> {
            if (payload.has("placementHeatmap")) {
                toMauHeatmap(gridPlacementHeatmap, payload.getAsJsonObject("placementHeatmap"), true);
            }
            if (payload.has("shotHeatmap")) {
                toMauHeatmap(gridShotHeatmap, payload.getAsJsonObject("shotHeatmap"), false);
            }
        });
    }

    private void toMauHeatmap(GridPane grid, JsonObject dataMap, boolean isPlacement) {
        int maxVal = 1;
        // Tìm giá trị lớn nhất để tính tỉ lệ màu
        for (String key : dataMap.keySet()) {
            int val = dataMap.get(key).getAsInt();
            if (val > maxVal) maxVal = val;
        }

        for (javafx.scene.Node node : grid.getChildren()) {
            Integer c = GridPane.getColumnIndex(node);
            Integer r = GridPane.getRowIndex(node);
            if (r != null && c != null && node instanceof Label) {
                Label cell = (Label) node;
                String coord = r + "," + c; // Hoặc format tọa độ DB của bạn (vd: "A1" hoặc "0,0")
                int count = dataMap.has(coord) ? dataMap.get(coord).getAsInt() : 0;
                cell.setText(String.valueOf(count));

                if (count == 0) {
                    cell.setStyle("-fx-background-color: #1A2634; -fx-text-fill: #78909C; -fx-border-color: #263238; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11px;");
                } else {
                    double ratio = (double) count / maxVal;
                    // Đặt tàu dùng dải xanh-vàng, bắn dùng dải vàng-đỏ
                    String bgColor = isPlacement ? tinhMauNhietPlacement(ratio) : tinhMauNhietShot(ratio);
                    cell.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #FFFFFF33; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11px;");
                }
            }
        }
    }

    private String tinhMauNhietPlacement(double ratio) {
        if (ratio < 0.3) return "#00838F";
        if (ratio < 0.7) return "#00ACC1";
        return "#00E5FF";
    }

    private String tinhMauNhietShot(double ratio) {
        if (ratio < 0.3) return "#E65100";
        if (ratio < 0.7) return "#F4511E";
        return "#D50000";
    }
    /**
     * Nhận gói dữ liệu JSON từ Server khi có phản hồi "ADMIN_DATA"
     */
    public void capNhatDuLieuAdmin(JsonObject payload) {
        Platform.runLater(() -> {
            // 1. Cập nhật bảng Users
            if (payload.has("users")) {
                dsUsers.clear();
                JsonArray userArr = payload.getAsJsonArray("users");
                for (JsonElement el : userArr) {
                    JsonObject u = el.getAsJsonObject();
                    String id = String.valueOf(u.get("id").getAsInt());
                    String username = u.get("username").getAsString();
                    String nickname = u.get("nickname").getAsString();
                    String role = u.get("role").getAsString();
                    String status = u.get("status").getAsString();
                    String score = (u.has("score") && !u.get("score").isJsonNull()) ? u.get("score").getAsString() : "0";
                    String wins = String.valueOf(u.get("wins").getAsInt());
                    String losses = String.valueOf(u.get("losses").getAsInt());

                    dsUsers.add(new String[]{id, username, nickname, role, status, score, wins, losses});
                }
            }

            // 2. Cập nhật bảng Ongoing Matches (Trận đấu đang diễn ra)
            if (payload.has("matches")) {
                dsMatches.clear();
                JsonArray matchArr = payload.getAsJsonArray("matches");
                for (JsonElement el : matchArr) {
                    JsonObject m = el.getAsJsonObject();
                    String matchId = m.get("matchId").getAsString();
                    String p1 = m.get("player1Name").getAsString();
                    String p2 = m.get("player2Name").getAsString();
                    String status = m.get("status").getAsString();

                    dsMatches.add(new String[]{matchId, p1, p2, status});
                }
            }
        });
    }
}