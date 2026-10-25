package com.mycompany.battleship.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.battleship.common.model.MessageDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author DINH THE LINH
 */
public class LobbyGUI extends Application {

    public static String currentUsername = "";
    public static String currentScore = "0";
    private static Stage activeLobbyStage;
    private Stage primaryStage;
    private Label lblUser;
    private Label lblScore;

    // 3 chỉ số trên banner chào mừng (chỉ để hiển thị)
    private Label lblBannerDiem = new Label("0");
    private Label lblBannerOnline = new Label("0");
    private Label lblBannerHang = new Label("—");

    private TextField txtSearch;
    private TableView<String[]> tblPlayers;
    private ObservableList<String[]> dsNguoiChoi = FXCollections.observableArrayList();

    private TableView<String[]> tblHistory;
    private ObservableList<String[]> dsLichSu = FXCollections.observableArrayList();

    private TableView<String[]> tblRank;
    private ObservableList<String[]> dsXepHang = FXCollections.observableArrayList();

    private Alert alertChoXacNhan;
    private Alert alertNhanLoiMoi;
    private boolean isWaiting = false;
    private boolean biHuyBo = false; 

    @Override
    public void start(Stage primaryStage) {
        if (activeLobbyStage != null && activeLobbyStage != primaryStage && activeLobbyStage.isShowing()) {
            activeLobbyStage.close();
        }
        activeLobbyStage = primaryStage;
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Sảnh Chờ");

        SocketClientManager.lobbyScreen = this;

        // Nạp font tiêu đề "BATTLESHIP"
        Font.loadFont(getClass().getResourceAsStream("/TitanOne-Regular.ttf"), 24);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("pl-lobby");
        root.setTop(taoThanhTieuDe());

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(taoTabNguoiChoi());
        tabPane.getTabs().add(taoTabLichSu());
        tabPane.getTabs().add(taoTabXepHang());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("pl-tabs");

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().equals("Bảng xếp hạng")) {
                    SocketClientManager.guiTinNhan(new MessageDTO("LEADERBOARD", null));
                } else if (newTab.getText().equals("Lịch sử đấu")) {
                    SocketClientManager.guiTinNhan(new MessageDTO("MATCH_HISTORY", null));
                } else if (newTab.getText().equals("Danh sách người chơi")) {
                    SocketClientManager.guiTinNhan(new MessageDTO("LIST_PLAYERS", null));
                }
            }
        });

        // Khung trắng bo góc bọc ngoài TabPane
        StackPane theTab = new StackPane(tabPane);
        theTab.getStyleClass().add("pl-card");

        HBox banner = taoBannerChaoMung();
        VBox noiDung = new VBox(18, banner, theTab);
        noiDung.setPadding(new Insets(20, 28, 24, 28));
        VBox.setVgrow(theTab, Priority.ALWAYS);
        root.setCenter(noiDung);

        SocketClientManager.guiTinNhan(new MessageDTO("LIST_PLAYERS", null));
        // Xin luôn bảng xếp hạng để banner biết hạng hiện tại của mình
        SocketClientManager.guiTinNhan(new MessageDTO("LEADERBOARD", null));

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(duongDanCss());
        scene.getStylesheets().add(getClass().getResource("/player.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);     // màn Đăng nhập khóa resize, sang sảnh mở lại
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(660);
        primaryStage.show();

        // Hiệu ứng mở màn: banner hiện dần
        banner.setOpacity(0);
        FadeTransition hien = new FadeTransition(Duration.millis(500), banner);
        hien.setToValue(1);
        hien.play();
    }
    public static void dongSanhChoHienTai() {
        Platform.runLater(() -> {
            if (SocketClientManager.lobbyScreen != null) {
                SocketClientManager.lobbyScreen.batDauTranDau();
            }
            if (activeLobbyStage != null && activeLobbyStage.isShowing()) {
                activeLobbyStage.close();
            }
        });
    }
    private String duongDanCss() {
        return getClass().getResource("/game-style.css").toExternalForm();
    }

    private void apDungStyle(Alert alert) {
        DialogStyle.apply(alert);
    }

    // =================================================================== HÀM PHỤ DỰNG GIAO DIỆN

    /** Tạo icon vẽ bằng đường SVG (hình dạng nằm trong player.css, ví dụ "pi-bolt"). */
    private Region taoIcon(String loai, double co) {
        Region icon = new Region();
        icon.getStyleClass().addAll("pl-icon", loai);
        icon.setMinSize(co, co);
        icon.setPrefSize(co, co);
        icon.setMaxSize(co, co);
        return icon;
    }

    /** Nút có icon; kieu = "pl-btn-ghost" / "pl-btn-fight" / "pl-btn-replay"... */
    private Button taoNut(String chu, String icon, String... kieu) {
        Button nut = new Button(chu, taoIcon(icon, 14));
        nut.getStyleClass().add("pl-btn");
        nut.getStyleClass().addAll(kieu);
        return nut;
    }

    /** Màn hình trống khi bảng chưa có dữ liệu: icon tròn + tiêu đề + mô tả. */
    private VBox taoTrangTrong(String icon, String tieuDe, String moTa) {
        StackPane khungIcon = new StackPane(taoIcon(icon, 34));
        khungIcon.getStyleClass().add("pl-empty-icon");
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("pl-empty-title");
        Label lblMoTa = new Label(moTa);
        lblMoTa.getStyleClass().add("pl-empty-sub");
        VBox box = new VBox(8, khungIcon, lblTieuDe, lblMoTa);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** Avatar tròn nhỏ, màu cố định theo tên. */
    private Label taoAvatarNho(String ten) {
        String t = (ten == null || ten.isEmpty()) ? "?" : ten;
        Label av = new Label(t.substring(0, 1).toUpperCase());
        av.getStyleClass().addAll("pl-avatar-sm", "pl-av-" + Math.abs(t.hashCode() % 6));
        return av;
    }

    /** Ô "avatar + tên" dùng chung cho các bảng. */
    private HBox taoONguoiChoi(String ten) {
        Label lblTen = new Label(ten);
        lblTen.getStyleClass().add("pl-ten");
        HBox hang = new HBox(10, taoAvatarNho(ten), lblTen);
        hang.setAlignment(Pos.CENTER_LEFT);
        if (ten != null && ten.equalsIgnoreCase(currentUsername)) {
            Label ban = new Label("(Bạn)");
            ban.getStyleClass().add("pl-phu");
            hang.getChildren().add(ban);
        }
        return hang;
    }

    private <T> void tuyChinhBang(TableView<T> bang) {
        bang.getStyleClass().add("pl-table");
        bang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bang.setPlaceholder(taoTrangTrong("pi-refresh", "Chưa có dữ liệu", "Bấm \"Làm mới\" để tải lại"));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(bang.widthProperty());
        clip.heightProperty().bind(bang.heightProperty());
        bang.setClip(clip);

        VBox.setVgrow(bang, Priority.ALWAYS);
    }

    /** Thanh đầu mỗi tab: tiêu đề + mô tả bên trái, các ô/nút bên phải. */
    private HBox taoThanhCongCu(String tieuDe, String moTa, Node... nodes) {
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("pl-section-title");
        Label lblMoTa = new Label(moTa);
        lblMoTa.getStyleClass().add("pl-section-sub");
        VBox chu = new VBox(2, lblTieuDe, lblMoTa);

        Region khoangTrong = new Region();
        HBox.setHgrow(khoangTrong, Priority.ALWAYS);

        HBox toolbar = new HBox(10);
        toolbar.getChildren().addAll(chu, khoangTrong);
        toolbar.getChildren().addAll(nodes);
        toolbar.setPadding(new Insets(14, 20, 14, 20));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private VBox taoNoiDungTab(HBox toolbar, TableView<?> bang) {
        VBox.setMargin(bang, new Insets(0, 20, 20, 20));
        return new VBox(toolbar, bang);
    }

    private Tab taoTab(String ten, String icon, Node noiDung) {
        Tab tab = new Tab(ten, noiDung);
        tab.setGraphic(taoIcon(icon, 16));
        return tab;
    }

    // =================================================================== THANH TIÊU ĐỀ + BANNER

    private HBox taoThanhTieuDe() {
        String ten = currentUsername == null ? "" : currentUsername;

        StackPane logo = new StackPane(taoIcon("pi-anchor", 26));
        logo.getStyleClass().add("pl-header-logo");

        Label lblTitle = new Label("BATTLESHIP");
        lblTitle.getStyleClass().add("pl-header-brand");
        Label lblSub = new Label("S Ả N H   C H Ờ   ·   O N L I N E");
        lblSub.getStyleClass().add("pl-header-sub");
        VBox titleBox = new VBox(0, lblTitle, lblSub);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(14, logo, titleBox);
        brand.setAlignment(Pos.CENTER_LEFT);

        // Thẻ người chơi: avatar + tên + điểm
        Label avatar = new Label(ten.isEmpty() ? "?" : ten.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("pl-avatar");

        lblUser = new Label(ten);
        lblUser.getStyleClass().add("pl-chip-name");

        lblScore = new Label(currentScore + " điểm", taoIcon("pi-star", 12));
        lblScore.getStyleClass().add("pl-chip-score");

        VBox textBox = new VBox(1, lblUser, lblScore);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox chip = new HBox(10, avatar, textBox);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("pl-chip");

        Button btnLogout = new Button("Đăng xuất", taoIcon("pi-logout", 15));
        btnLogout.getStyleClass().add("pl-btn-logout");
        btnLogout.setOnAction(e -> {
            SocketClientManager.guiTinNhan(new MessageDTO("LOGOUT", null));
            SocketClientManager.dongKetNoi();
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        Region khoangTrong = new Region();
        HBox.setHgrow(khoangTrong, Priority.ALWAYS);

        HBox box = new HBox(16, brand, khoangTrong, chip, btnLogout);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("pl-header");
        return box;
    }

    /** 1 ô chỉ số trên banner: icon + số + nhãn. */
    private HBox taoChiSo(String icon, Label lblSo, String nhan) {
        lblSo.getStyleClass().add("pl-stat-so");
        Label lblNhan = new Label(nhan);
        lblNhan.getStyleClass().add("pl-stat-nhan");
        VBox chu = new VBox(0, lblSo, lblNhan);
        HBox o = new HBox(10, taoIcon(icon, 22), chu);
        o.setAlignment(Pos.CENTER_LEFT);
        o.getStyleClass().add("pl-stat");
        return o;
    }

    /** Banner cam: lời chào + 3 chỉ số + mỏ neo lắc lư. */
    private HBox taoBannerChaoMung() {
        String ten = (currentUsername == null || currentUsername.isEmpty()) ? "thuyền trưởng" : currentUsername;
        Label lblChao = new Label("Chào thuyền trưởng " + ten + "!");
        lblChao.getStyleClass().add("pl-hero-title");
        Label lblMoTa = new Label("Chọn một đối thủ đang trực tuyến rồi bấm \"Thách đấu\" để ra khơi.");
        lblMoTa.getStyleClass().add("pl-hero-sub");
        VBox chu = new VBox(4, lblChao, lblMoTa);

        lblBannerDiem.setText(currentScore);
        HBox chiSo = new HBox(12,
                taoChiSo("pi-star", lblBannerDiem, "ĐIỂM CỦA BẠN"),
                taoChiSo("pi-users", lblBannerOnline, "ĐANG TRỰC TUYẾN"),
                taoChiSo("pi-trophy", lblBannerHang, "HẠNG HIỆN TẠI"));

        VBox trai = new VBox(14, chu, chiSo);

        // Mỏ neo lớn mờ bên phải, nhấp nhô lên xuống như trên sóng biển
        Region neo = taoIcon("pi-anchor", 96);
        neo.getStyleClass().add("pl-hero-anchor");
        TranslateTransition nhapNho = new TranslateTransition(Duration.seconds(1.8), neo);
        nhapNho.setFromY(-7);
        nhapNho.setToY(7);
        nhapNho.setAutoReverse(true);
        nhapNho.setCycleCount(Animation.INDEFINITE);
        nhapNho.play();

        Region khoangTrong = new Region();
        HBox.setHgrow(khoangTrong, Priority.ALWAYS);

        HBox banner = new HBox(trai, khoangTrong, neo);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("pl-hero");
        return banner;
    }

    // =================================================================== TAB 1: DANH SÁCH NGƯỜI CHƠI

    private Tab taoTabNguoiChoi() {
        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo tên...");
        txtSearch.getStyleClass().add("pl-search");
        txtSearch.setPrefWidth(240);
        // Icon kính lúp nằm chồng bên trái ô tìm kiếm
        Region kinhLup = taoIcon("pi-search", 15);
        kinhLup.setMouseTransparent(true);
        StackPane oTimKiem = new StackPane(txtSearch, kinhLup);
        StackPane.setAlignment(kinhLup, Pos.CENTER_LEFT);
        StackPane.setMargin(kinhLup, new Insets(0, 0, 0, 13));

        Button btnSearch = taoNut("Làm mới", "pi-refresh", "pl-btn-ghost");
        btnSearch.setOnAction(e -> {
            SocketClientManager.guiTinNhan(new MessageDTO("LIST_PLAYERS", null));
            txtSearch.clear();
        });

        HBox toolbar = taoThanhCongCu("Danh sách người chơi",
                "Người đang trực tuyến sẽ có nút \"Thách đấu\"", oTimKiem, btnSearch);

        tblPlayers = new TableView<>();
        tuyChinhBang(tblPlayers);
        tblPlayers.setPlaceholder(taoTrangTrong("pi-users", "Chưa có người chơi nào", "Bấm \"Làm mới\" để tải lại danh sách"));

        TableColumn<String[], String> colTen = new TableColumn<>("NGƯỜI CHƠI");
        colTen.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colTen.setPrefWidth(250);
        colTen.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : taoONguoiChoi(item));
            }
        });

        // Trạng thái: viên thuốc có chấm màu (xanh = trực tuyến, xám = ngoại tuyến, vàng = đang thi đấu)
        TableColumn<String[], String> colTrangThai = new TableColumn<>("TRẠNG THÁI");
        colTrangThai.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colTrangThai.setPrefWidth(150);
        colTrangThai.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                String lop;
                String chu;
                if (item.equalsIgnoreCase("ONLINE")) {
                    lop = "pl-pill-online";
                    chu = "Trực tuyến";
                } else if (item.equalsIgnoreCase("OFFLINE")) {
                    lop = "pl-pill-offline";
                    chu = "Ngoại tuyến";
                } else {
                    lop = "pl-pill-busy";
                    chu = "Đang thi đấu";
                }
                Circle cham = new Circle(4);
                cham.getStyleClass().add("pl-dot");
                Label pill = new Label(chu, cham);
                pill.getStyleClass().addAll("pl-pill", lop);
                setGraphic(pill);
            }
        });

        TableColumn<String[], String> colDiem = new TableColumn<>("ĐIỂM");
        colDiem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colDiem.setPrefWidth(110);
        colDiem.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label diem = new Label(item, taoIcon("pi-trophy", 14));
                diem.getStyleClass().add("pl-diem");
                setGraphic(diem);
            }
        });

        TableColumn<String[], Void> colThachDau = new TableColumn<>("THÁCH ĐẤU");
        colThachDau.setPrefWidth(140);
        colThachDau.setCellFactory(col -> new javafx.scene.control.TableCell<String[], Void>() {
            private final Button btn = taoNut("Thách đấu", "pi-bolt", "pl-btn-sm", "pl-btn-fight");
            {
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

        FilteredList<String[]> filteredData = new FilteredList<>(dsNguoiChoi, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(player -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return player[0].toLowerCase().contains(lowerCaseFilter);
            });
        });

        tblPlayers.getColumns().addAll(colTen, colTrangThai, colDiem, colThachDau);
        tblPlayers.setItems(filteredData);

        return taoTab("Danh sách người chơi", "pi-users", taoNoiDungTab(toolbar, tblPlayers));
    }

    // =================================================================== TAB 2: LỊCH SỬ ĐẤU

    private Tab taoTabLichSu() {
        Button btnRefresh = taoNut("Làm mới", "pi-refresh", "pl-btn-ghost");
        btnRefresh.setOnAction(e -> SocketClientManager.guiTinNhan(new MessageDTO("MATCH_HISTORY", null)));

        tblHistory = new TableView<>();
        tuyChinhBang(tblHistory);
        tblHistory.setPlaceholder(taoTrangTrong("pi-history", "Bạn chưa đánh trận nào", "Hãy thách đấu một người chơi để bắt đầu!"));

        TableColumn<String[], String> colId = new TableColumn<>("MÃ TRẬN");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colId.setMinWidth(90);
        colId.setMaxWidth(110);
        colId.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("pl-id");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("#" + item);
                    getStyleClass().add("pl-id");
                }
            }
        });

        TableColumn<String[], String> colDoiThu = new TableColumn<>("ĐỐI THỦ");
        colDoiThu.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colDoiThu.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : taoONguoiChoi(item));
            }
        });

        // Kết quả: thắng = huy hiệu xanh có cúp, thua = huy hiệu đỏ có cờ
        TableColumn<String[], String> colKetQua = new TableColumn<>("KẾT QUẢ");
        colKetQua.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colKetQua.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                boolean thang = item.equalsIgnoreCase("WIN") || item.equalsIgnoreCase("Thắng");
                String chu = thang ? "Chiến thắng" : (item.equalsIgnoreCase("LOSE") || item.equalsIgnoreCase("LOSS") ? "Thất bại" : item);
                Label pill = new Label(chu, taoIcon(thang ? "pi-trophy" : "pi-flag", 12));
                pill.getStyleClass().addAll("pl-pill", thang ? "pl-pill-win" : "pl-pill-lose");
                setGraphic(pill);
            }
        });

        TableColumn<String[], String> colThoiGian = new TableColumn<>("THỜI GIAN");
        colThoiGian.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        colThoiGian.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("pl-thoigian");
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Bỏ phần ".0" mili-giây ở cuối cho gọn
                    setText(item.length() > 16 ? item.substring(0, 16) : item);
                    getStyleClass().add("pl-thoigian");
                }
            }
        });

        // CỘT XEM LẠI TRẬN ĐẤU (REPLAY)
        TableColumn<String[], Void> colReplay = new TableColumn<>("XEM LẠI");
        colReplay.setPrefWidth(130);
        colReplay.setCellFactory(col -> new TableCell<String[], Void>() {
            private final Button btnReplay = taoNut("Xem lại", "pi-play", "pl-btn-sm", "pl-btn-replay");
            {
                btnReplay.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    String matchIdStr = row[0];
                    try {
                        int matchId = Integer.parseInt(matchIdStr);
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("matchId", matchId);
                        SocketClientManager.guiTinNhan(new MessageDTO("GET_REPLAY", payload));
                    } catch (NumberFormatException ex) {
                        System.err.println("Mã trận không hợp lệ: " + matchIdStr);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnReplay);
                }
            }
        });

        tblHistory.getColumns().addAll(colId, colDoiThu, colKetQua, colThoiGian, colReplay);
        tblHistory.setItems(dsLichSu);

        HBox toolbar = taoThanhCongCu("Lịch sử đấu", "Các trận bạn đã đánh — bấm \"Xem lại\" để phát lại từng lượt bắn", btnRefresh);
        return taoTab("Lịch sử đấu", "pi-history", taoNoiDungTab(toolbar, tblHistory));
    }

    // =================================================================== TAB 3: BẢNG XẾP HẠNG

    private Tab taoTabXepHang() {
        Button btnRefresh = taoNut("Làm mới", "pi-refresh", "pl-btn-ghost");
        btnRefresh.setOnAction(e -> SocketClientManager.guiTinNhan(new MessageDTO("LEADERBOARD", null)));

        tblRank = new TableView<>();
        tuyChinhBang(tblRank);
        tblRank.setPlaceholder(taoTrangTrong("pi-chart", "Chưa có bảng xếp hạng", "Bấm \"Làm mới\" để tải lại"));

        // Tô nền cam nhạt cho dòng của chính mình
        tblRank.setRowFactory(tv -> new TableRow<String[]>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("pl-row-me");
                if (!empty && item != null && item[1].equalsIgnoreCase(currentUsername)) {
                    getStyleClass().add("pl-row-me");
                }
            }
        });

        // Hạng: top 3 là huy chương vàng / bạc / đồng
        TableColumn<String[], String> colHang = new TableColumn<>("HẠNG");
        colHang.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colHang.setMinWidth(80);
        colHang.setMaxWidth(100);
        colHang.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                } else if (item.equals("1") || item.equals("2") || item.equals("3")) {
                    Label huyChuong = new Label(item);
                    huyChuong.getStyleClass().addAll("pl-medal", "pl-medal-" + item);
                    setGraphic(huyChuong);
                } else {
                    Label hang = new Label("#" + item);
                    hang.getStyleClass().add("pl-hang");
                    setGraphic(hang);
                }
            }
        });

        TableColumn<String[], String> colTen = new TableColumn<>("NGƯỜI CHƠI");
        colTen.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colTen.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : taoONguoiChoi(item));
            }
        });

        TableColumn<String[], String> colThang = new TableColumn<>("THẮNG");
        colThang.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> colThua = new TableColumn<>("THUA");
        colThua.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));

        TableColumn<String[], String> colDiem = new TableColumn<>("ĐIỂM");
        colDiem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[4]));
        colDiem.setCellFactory(col -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label diem = new Label(item, taoIcon("pi-trophy", 14));
                diem.getStyleClass().add("pl-diem");
                setGraphic(diem);
            }
        });

        tblRank.getColumns().addAll(colHang, colTen, colThang, colThua, colDiem);
        tblRank.setItems(dsXepHang);

        HBox toolbar = taoThanhCongCu("Bảng xếp hạng", "Top thuyền trưởng mạnh nhất server — dòng tô cam là bạn", btnRefresh);
        return taoTab("Bảng xếp hạng", "pi-chart", taoNoiDungTab(toolbar, tblRank));
    }

    public void capNhatDanhSachNguoiChoi(String jsonString) {
        dsNguoiChoi.clear();
        if (jsonString == null || jsonString.isEmpty()) return;
        try {
            JsonArray array = JsonParser.parseString(jsonString).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String name = obj.has("nickname") ? obj.get("nickname").getAsString() : "";
                String status = obj.has("status") ? obj.get("status").getAsString() : "";
                String score = obj.has("score") ? obj.get("score").getAsString() : "0";
                dsNguoiChoi.add(new String[]{name, status, score});
            }
            // Đếm số người không OFFLINE để hiện trên banner
            int online = 0;
            for (String[] ng : dsNguoiChoi) {
                if (!"OFFLINE".equalsIgnoreCase(ng[1])) online++;
            }
            final int soOnline = online;
            Platform.runLater(() -> lblBannerOnline.setText(String.valueOf(soOnline)));
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON Danh Sách Người Chơi: " + e.getMessage());
        }
    }

    public void capNhatLichSuDau(String jsonString) {
        dsLichSu.clear();
        if (jsonString == null || jsonString.isEmpty()) return;
        try {
            JsonArray array = JsonParser.parseString(jsonString).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.has("matchId") ? obj.get("matchId").getAsString() : "";
                String opp = obj.has("opponentNickname") ? obj.get("opponentNickname").getAsString() : "";
                String result = obj.has("result") ? obj.get("result").getAsString() : "";
                
                String time = "";
                if (obj.has("playedAt") && !obj.get("playedAt").isJsonNull()) {
                    time = obj.get("playedAt").getAsString();
                }
                dsLichSu.add(new String[]{id, opp, result, time});
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON Lịch Sử: " + e.getMessage());
        }
    }

    public void capNhatBangXepHang(String jsonString) {
        dsXepHang.clear();
        if (jsonString == null || jsonString.isEmpty()) return;
        try {
            JsonArray array = JsonParser.parseString(jsonString).getAsJsonArray();
            int rank = 1;
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String name = obj.has("nickname") ? obj.get("nickname").getAsString() : "";
                String wins = obj.has("wins") ? obj.get("wins").getAsString() : "0";
                String losses = obj.has("losses") ? obj.get("losses").getAsString() : "0";
                String score = obj.has("score") ? obj.get("score").getAsString() : "0";
                
                dsXepHang.add(new String[]{String.valueOf(rank), name, wins, losses, score});
                
                if (name.equalsIgnoreCase(currentUsername)) {
                    currentScore = score;
                    final int hangCuaMinh = rank;
                    Platform.runLater(() -> {
                        lblScore.setText(currentScore + " điểm");
                        lblBannerDiem.setText(currentScore);
                        lblBannerHang.setText("#" + hangCuaMinh);
                    });
                }
                rank++;
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON Bảng Xếp Hạng: " + e.getMessage());
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

        Map<String, Object> payload = new HashMap<>();
        payload.put("target", tenDoiThu);
        SocketClientManager.guiTinNhan(new MessageDTO("INVITE", payload));

        Optional<ButtonType> result = alertChoXacNhan.showAndWait();
        if (result.isPresent() && result.get() == btnHuy) {
            SocketClientManager.guiTinNhan(new MessageDTO("CANCEL_INVITE", payload));
            isWaiting = false;
        }
    }

    public void nhanLoiThachDau(String nguoiMoi) {
        boolean dangChoNguoiKhac = isWaiting || (alertChoXacNhan != null && alertChoXacNhan.isShowing());
        boolean dangXemLoiMoi = (alertNhanLoiMoi != null && alertNhanLoiMoi.isShowing());

        Map<String, Object> payload = new HashMap<>();
        payload.put("challenger", nguoiMoi);

        if (dangChoNguoiKhac || dangXemLoiMoi) {
            SocketClientManager.guiTinNhan(new MessageDTO("INVITE_REJECT", payload));
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
        
        if (biHuyBo) return; 

        if (ketQua.isPresent() && ketQua.get() == btnDongY) {
            SocketClientManager.guiTinNhan(new MessageDTO("INVITE_ACCEPT", payload));
        } else {
            SocketClientManager.guiTinNhan(new MessageDTO("INVITE_REJECT", payload));
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

    public void batDauTranDau() {
        batDauTranDau(new String[0]);
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