package com.mycompany.battleship.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableCell;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.util.Optional;

/**
 * @author DINH THE LINH
 */
public class LobbyGUI extends Application {

    public static String currentUsername = "";
    public static String currentScore = "0";

    private Stage primaryStage;
    private Label lblUser;
    private Label lblScore;

    private TextField txtSearch;
    private TableView<String[]> tblPlayers;
    private ObservableList<String[]> dsNguoiChoi = FXCollections.observableArrayList();

    private TableView<String[]> tblHistory;
    private ObservableList<String[]> dsLichSu = FXCollections.observableArrayList();

    private TableView<String[]> tblRank;
    private ObservableList<String[]> dsXepHang = FXCollections.observableArrayList();

    // Các biến quản lý hộp thoại và chống spam
    private Alert alertChoXacNhan;
    private Alert alertNhanLoiMoi;
    private boolean isWaiting = false;
    private boolean biHuyBo = false; 

    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Sảnh Chờ");

        SocketClientManager.lobbyScreen = this;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("lobby-root"); 
        root.setTop(taoThanhTieuDe());

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(taoTabNguoiChoi());
        tabPane.getTabs().add(taoTabLichSu());
        tabPane.getTabs().add(taoTabXepHang());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("lobby-tabs");
        
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().equals("Bảng xếp hạng")) {
                    SocketClientManager.guiTinNhan("LEADERBOARD");
                } else if (newTab.getText().equals("Lịch sử đấu")) {
                    SocketClientManager.guiTinNhan("MATCH_HISTORY");
                } else if (newTab.getText().equals("Danh sách người chơi")) {
                    // Dùng LIST_PLAYERS mặc định, không kèm tham số tìm kiếm để reset danh sách
                    SocketClientManager.guiTinNhan("LIST_PLAYERS"); 
                }
            }
        });
        
        BorderPane.setMargin(tabPane, new Insets(20, 28, 28, 28));
        root.setCenter(tabPane);

        SocketClientManager.guiTinNhan("LIST_PLAYERS");

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(duongDanCss());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** Đường dẫn stylesheet dùng chung với màn hình trận đấu (game-style.css). */
    private String duongDanCss() {
        return getClass().getResource("/game-style.css").toExternalForm();
    }

    /** Áp dụng phong cách chung cho các hộp thoại của sảnh chờ. */
    private void apDungStyle(Alert alert) {
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(duongDanCss());
        dp.getStyleClass().add("lobby-dialog");
    }

    /** Dùng chung cho 3 bảng: bo góc, cột giãn đều, thông báo khi trống. */
    private <T> void tuyChinhBang(TableView<T> bang) {
        bang.getStyleClass().add("lobby-table");
        bang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bang.setPlaceholder(new Label("Chưa có dữ liệu"));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(bang.widthProperty());
        clip.heightProperty().bind(bang.heightProperty());
        bang.setClip(clip);

        VBox.setVgrow(bang, Priority.ALWAYS);
    }

    /** Thanh công cụ phía trên mỗi bảng (nút làm mới, ô tìm kiếm...). */
    private HBox taoThanhCongCu(javafx.scene.Node... nodes) {
        HBox toolbar = new HBox(10, nodes);
        toolbar.setPadding(new Insets(16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    /** Ghép thanh công cụ + bảng thành nội dung của một tab. */
    private VBox taoNoiDungTab(HBox toolbar, TableView<?> bang) {
        VBox.setMargin(bang, new Insets(0, 16, 16, 16));
        return new VBox(toolbar, bang);
    }

    private HBox taoThanhTieuDe() {
        // Avatar + tên + điểm: cùng kiểu với thanh trên của màn hình trận đấu
        String ten = currentUsername == null ? "" : currentUsername;

        Circle avatarBg = new Circle(20);
        avatarBg.getStyleClass().addAll("player-avatar", "avatar-color-1");
        Label initial = new Label(ten.isEmpty() ? "?" : ten.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("player-avatar-initial");
        StackPane avatar = new StackPane(avatarBg, initial);

        lblUser = new Label(ten);
        lblUser.getStyleClass().add("player-name");

        lblScore = new Label("★ " + currentScore);
        lblScore.getStyleClass().add("score-badge");

        VBox textBox = new VBox(3, lblUser, lblScore);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox userBox = new HBox(10, avatar, textBox);
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.setPrefWidth(280);

        Label lblTitle = new Label("BATTLE SHIP");
        lblTitle.getStyleClass().add("lobby-title");
        Label lblSub = new Label("COMMAND CENTER");
        lblSub.getStyleClass().add("lobby-subtitle");
        VBox titleBox = new VBox(2, lblTitle, lblSub);
        titleBox.setAlignment(Pos.CENTER);

        Button btnLogout = new Button("Đăng xuất");
        btnLogout.getStyleClass().add("lobby-btn-exit");
        btnLogout.setOnAction(e -> {
            SocketClientManager.guiTinNhan("LOGOUT");
            SocketClientManager.dongKetNoi();
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        HBox rightBox = new HBox(btnLogout);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setPrefWidth(280);

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

    private Tab taoTabNguoiChoi() {
        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo tên...");
        txtSearch.getStyleClass().add("lobby-search");
        
        Button btnSearch = new Button("Làm mới"); // Đổi nút thành Làm mới để load lại danh sách từ server
        btnSearch.getStyleClass().add("lobby-btn-primary");
        btnSearch.setOnAction(e -> {
            SocketClientManager.guiTinNhan("LIST_PLAYERS");
            txtSearch.clear(); // Bấm làm mới thì xóa trắng ô tìm kiếm
        });

        HBox toolbar = taoThanhCongCu(txtSearch, btnSearch);

        tblPlayers = new TableView<>();
        tuyChinhBang(tblPlayers);
        TableColumn<String[], String> colTen = new TableColumn<>("Tên người chơi");
        colTen.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colTen.setPrefWidth(250);

        TableColumn<String[], String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colTrangThai.setPrefWidth(150);
        colTrangThai.setCellFactory(col -> new javafx.scene.control.TableCell<String[], String>() {
        private final javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(6);

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                setText(item.toLowerCase()); 
                setGraphicTextGap(8); 

                if (item.equalsIgnoreCase("ONLINE")) {
                    dot.setFill(javafx.scene.paint.Color.web("#00897B")); 
                    setStyle("-fx-text-fill: #00897B; -fx-font-weight: bold;");
                } else if (item.equalsIgnoreCase("OFFLINE")) {
                    dot.setFill(javafx.scene.paint.Color.web("#B0BEC5"));
                    setStyle("-fx-text-fill: #90A4AE; -fx-font-weight: normal;");
                } else {
                    dot.setFill(javafx.scene.paint.Color.web("#F9A825"));
                    setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
                }

                setGraphic(dot);
            }
        }
    });
        
        TableColumn<String[], String> colDiem = new TableColumn<>("Điểm");
        colDiem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colDiem.setPrefWidth(100);

        TableColumn<String[], Void> colThachDau = new TableColumn<>("Thách đấu");
        colThachDau.setPrefWidth(120);
        colThachDau.setCellFactory(col -> new javafx.scene.control.TableCell<String[], Void>() {
            private final Button btn = new Button("Thách đấu");
            {
                btn.getStyleClass().add("lobby-btn-challenge");
                btn.setOnAction(e -> {
                    if (isWaiting) {
                        Alert canhkBao = new Alert(AlertType.WARNING, "Bạn đang đợi một người khác trả lời. Hãy Hủy yêu cầu cũ trước!");
                        apDungStyle(canhkBao);
                        canhkBao.show();
                        return;
                    }
                    
                    String[] row = getTableView().getItems().get(getIndex());
                    String tenDoiThu = row[0];
                    String trangThai = row[1];
                    if (tenDoiThu.equalsIgnoreCase(currentUsername) || !trangThai.equals("ONLINE")) return;
                    
                    hienThiHopThoaiCho(tenDoiThu);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                String[] row = getTableView().getItems().get(getIndex());
                boolean coTheThachDau = row[1].equals("ONLINE") && !row[0].equalsIgnoreCase(currentUsername);
                setGraphic(coTheThachDau ? btn : null);
            }
        });

        // TÍNH NĂNG MỚI: Bọc danh sách gốc vào FilteredList để TÌM KIẾM TỨC THÌ
        FilteredList<String[]> filteredData = new FilteredList<>(dsNguoiChoi, p -> true);
        
        // Lắng nghe sự kiện người dùng GÕ CHỮ vào ô tìm kiếm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(player -> {
                // Nếu xóa trắng ô tìm kiếm thì hiện tất cả
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                // Kiểm tra xem tên người chơi có chứa từ khóa đang nhập không
                return player[0].toLowerCase().contains(lowerCaseFilter); 
            });
        });

        tblPlayers.getColumns().addAll(colTen, colTrangThai, colDiem, colThachDau);
        // Nhúng cái danh sách ĐÃ ĐƯỢC LỌC (filteredData) vào bảng thay vì danh sách gốc
        tblPlayers.setItems(filteredData);

        return new Tab("Danh sách người chơi", taoNoiDungTab(toolbar, tblPlayers));
    }

    private Tab taoTabLichSu() {
        Button btnRefresh = new Button("Làm mới");
        btnRefresh.getStyleClass().add("lobby-btn-primary");
        btnRefresh.setOnAction(e -> SocketClientManager.guiTinNhan("MATCH_HISTORY"));

        tblHistory = new TableView<>();
        tuyChinhBang(tblHistory);
        TableColumn<String[], String> colId = new TableColumn<>("Mã trận");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> colDoiThu = new TableColumn<>("Đối thủ");
        colDoiThu.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> colKetQua = new TableColumn<>("Kết quả");
        colKetQua.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        // Hiển thị kết quả dạng huy hiệu: Thắng (xanh ngọc) / Thua (đỏ nhạt)
        colKetQua.setCellFactory(col -> new TableCell<String[], String>() {
            private final Label pill = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                pill.setText(item);
                pill.getStyleClass().setAll("label", "lobby-pill",
                        item.equalsIgnoreCase("Thắng") ? "lobby-result-win" : "lobby-result-lose");
                setText(null);
                setGraphic(pill);
            }
        });

        TableColumn<String[], String> colThoiGian = new TableColumn<>("Thời gian");
        colThoiGian.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));

        tblHistory.getColumns().addAll(colId, colDoiThu, colKetQua, colThoiGian);
        tblHistory.setItems(dsLichSu);

        return new Tab("Lịch sử đấu", taoNoiDungTab(taoThanhCongCu(btnRefresh), tblHistory));
    }

    private Tab taoTabXepHang() {
        Button btnRefresh = new Button("Làm mới");
        btnRefresh.getStyleClass().add("lobby-btn-primary");
        btnRefresh.setOnAction(e -> SocketClientManager.guiTinNhan("LEADERBOARD"));

        tblRank = new TableView<>();
        tuyChinhBang(tblRank);
        TableColumn<String[], String> colHang = new TableColumn<>("Hạng");
        colHang.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        // Top 3 hiển thị dạng huy hiệu vàng / bạc / đồng
        colHang.setCellFactory(col -> new TableCell<String[], String>() {
            private final Label pill = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item.equals("1") || item.equals("2") || item.equals("3")) {
                    pill.setText("#" + item);
                    pill.getStyleClass().setAll("label", "lobby-pill", "lobby-rank-" + item);
                    setText(null);
                    setGraphic(pill);
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        });

        TableColumn<String[], String> colTen = new TableColumn<>("Tên người chơi");
        colTen.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> colThang = new TableColumn<>("Thắng");
        colThang.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> colThua = new TableColumn<>("Thua");
        colThua.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));

        TableColumn<String[], String> colDiem = new TableColumn<>("Điểm");
        colDiem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[4]));

        tblRank.getColumns().addAll(colHang, colTen, colThang, colThua, colDiem);
        tblRank.setItems(dsXepHang);

        return new Tab("Bảng xếp hạng", taoNoiDungTab(taoThanhCongCu(btnRefresh), tblRank));
    }

    public void capNhatDanhSachNguoiChoi(String noiDung) {
        dsNguoiChoi.clear();
        if (noiDung.isEmpty()) return;
        for (String bg : noiDung.split(";")) {
            String[] cot = bg.split(",");
            if (cot.length >= 3) dsNguoiChoi.add(cot);
        }
    }

    public void capNhatLichSuDau(String noiDung) {
        dsLichSu.clear();
        if (noiDung.isEmpty()) return;
        for (String bg : noiDung.split(";")) {
            String[] cot = bg.split(",");
            if (cot.length >= 4) dsLichSu.add(cot);
        }
    }

    public void capNhatBangXepHang(String noiDung) {
        dsXepHang.clear();
        if (noiDung == null || noiDung.isEmpty()) return;
        
        for (String bg : noiDung.split(";")) {
            String[] cot = bg.split(",");
            // Kiểm tra độ dài mảng phải đủ 5 phần tử (hạng, tên, thắng, thua, điểm)
            if (cot.length >= 5) {
                dsXepHang.add(cot);
                // Nếu đúng là user đang đăng nhập thì cập nhật điểm lên thanh header
                if (cot[1].equalsIgnoreCase(currentUsername)) {
                    currentScore = cot[4];
                    Platform.runLater(() -> {
                        lblScore.setText("★ " + currentScore);
                    });
                }
            }
        }
    }

    public void hienThiHopThoaiCho(String tenDoiThu) {
        isWaiting = true; 
        alertChoXacNhan = new Alert(AlertType.CONFIRMATION);
        alertChoXacNhan.setTitle("Đang chờ phản hồi...");
        alertChoXacNhan.setHeaderText("Đã gửi chiến thư đến [" + tenDoiThu + "]");
        alertChoXacNhan.setContentText("Vui lòng đợi thuyền trưởng đối phương xác nhận...");
        
        ButtonType btnHuy = new ButtonType("Hủy yêu cầu", ButtonBar.ButtonData.CANCEL_CLOSE);
        alertChoXacNhan.getButtonTypes().setAll(btnHuy);

        apDungStyle(alertChoXacNhan);

        SocketClientManager.guiTinNhan("INVITE|" + tenDoiThu);

        Optional<ButtonType> result = alertChoXacNhan.showAndWait();
        if (result.isPresent() && result.get() == btnHuy) {
            SocketClientManager.guiTinNhan("CANCEL_INVITE|" + tenDoiThu);
            isWaiting = false;
        }
    }

    public void nhanLoiThachDau(String nguoiMoi) {
        boolean dangChoNguoiKhac = isWaiting || (alertChoXacNhan != null && alertChoXacNhan.isShowing());
        boolean dangXemLoiMoi = (alertNhanLoiMoi != null && alertNhanLoiMoi.isShowing());

        if (dangChoNguoiKhac || dangXemLoiMoi) {
            SocketClientManager.guiTinNhan("INVITE_REJECT|" + nguoiMoi);
            return; 
        }

        biHuyBo = false; 
        alertNhanLoiMoi = new Alert(AlertType.CONFIRMATION);
        if (primaryStage != null) alertNhanLoiMoi.initOwner(primaryStage);
        
        alertNhanLoiMoi.setTitle("Tín hiệu chiến đấu!");
        alertNhanLoiMoi.setHeaderText(nguoiMoi + " đang muốn khai hỏa với bạn!");
        alertNhanLoiMoi.setContentText("Bạn có sẵn sàng tham chiến không?");

        ButtonType btnDongY = new ButtonType("Vào trận", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnTuChoi = new ButtonType("Từ chối", ButtonBar.ButtonData.CANCEL_CLOSE);
        alertNhanLoiMoi.getButtonTypes().setAll(btnDongY, btnTuChoi);
        apDungStyle(alertNhanLoiMoi);

        Optional<ButtonType> ketQua = alertNhanLoiMoi.showAndWait();
        
        if (biHuyBo) {
            return; 
        }

        if (ketQua.isPresent() && ketQua.get() == btnDongY) {
            SocketClientManager.guiTinNhan("INVITE_ACCEPT|" + nguoiMoi);
        } else {
            SocketClientManager.guiTinNhan("INVITE_REJECT|" + nguoiMoi);
        }
    }

    public void loiMoiBiTuChoi(String nguoiTuChoi) {
        if (alertChoXacNhan != null && alertChoXacNhan.isShowing()) {
            alertChoXacNhan.close(); 
        }
        isWaiting = false; 

        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING, nguoiTuChoi + " không muốn giao chiến lúc này.", ButtonType.OK);
            apDungStyle(alert);
            alert.setTitle("Từ chối");
            alert.setHeaderText("Mục tiêu đã rút lui!");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }

    public void doiThuHuyLoiMoi(String nguoiHuy) {
        if (alertNhanLoiMoi != null && alertNhanLoiMoi.isShowing()) {
            biHuyBo = true; 
            alertNhanLoiMoi.close(); 

            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION, nguoiHuy + " đã mất kiên nhẫn và hủy lời mời!", ButtonType.OK);
                apDungStyle(alert);
                alert.setTitle("Hủy yêu cầu");
                alert.setHeaderText(null);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
                alert.show();
            });
        }
    }

    public void batDauTranDau(String[] parts) {
        if (alertChoXacNhan != null && alertChoXacNhan.isShowing()) {
            alertChoXacNhan.close();
            isWaiting = false; 
        }

        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    public void loiThachDauThatBai(String lyDo) {
        if (alertChoXacNhan != null && alertChoXacNhan.isShowing()) {
            alertChoXacNhan.close();
        }
        isWaiting = false; 

        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR, lyDo, ButtonType.OK);
            if (primaryStage != null) alert.initOwner(primaryStage);
            apDungStyle(alert);
            alert.setTitle("Thất bại");
            alert.setHeaderText("Không thể gửi chiến thư!");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }
}