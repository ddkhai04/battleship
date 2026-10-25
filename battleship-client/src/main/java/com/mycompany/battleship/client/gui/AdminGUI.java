package com.mycompany.battleship.client.gui;

import com.mycompany.battleship.common.model.MessageDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Màn hình Quản trị (Admin Command Center).
 * Giao diện: header navy + 4 thẻ số liệu nhanh + khung tab (Tài khoản, Giám sát, Biểu đồ nhiệt, Lịch sử).
 * Toàn bộ màu sắc/kích thước nằm trong admin.css; icon vẽ bằng đường SVG (-fx-shape) trong CSS.
 *
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

    // Thêm danh sách ObservableList cho Finished Matches
    private TableView<String[]> tblFinishedMatches;
    private ObservableList<String[]> dsFinishedMatches = FXCollections.observableArrayList();

    // 4 thẻ số liệu nhanh trên đầu màn hình (chỉ để hiển thị, đếm từ các danh sách ở trên)
    private Label lblKpiTongTaiKhoan = new Label("0");
    private Label lblKpiDangOnline = new Label("0");
    private Label lblKpiDangDau = new Label("0");
    private Label lblKpiDaKetThuc = new Label("0");

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Admin Command Center");

        // Đăng ký màn hình Admin với SocketClientManager
        SocketClientManager.adminScreen = this;

        // Nạp font tiêu đề (đã có sẵn trong resources, SplashGUI cũng dùng)
        Font.loadFont(getClass().getResourceAsStream("/TitanOne-Regular.ttf"), 24);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("ad-root");
        root.setTop(taoThanhTieuDe());

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(taoTabQuanLyTaiKhoan());
        tabPane.getTabs().add(taoTabGiamSatTranDau());
        tabPane.getTabs().add(taoTabBieuDoNhiet());
        tabPane.getTabs().add(taoTabLichSuReplay());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("ad-tabs");

        // Lắng nghe sự kiện đổi tab để yêu cầu dữ liệu mới từ Server
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                yeuCauDuLieuAdmin();
                // Mở tab Biểu đồ nhiệt thì tự tải luôn dữ liệu heatmap toàn server
                if (newTab.getText() != null && newTab.getText().contains("Biểu đồ nhiệt")) {
                    SocketClientManager.guiTinNhan(new MessageDTO("GET_HEATMAP", null));
                }
            }
        });

        // Khung trắng bo góc bọc ngoài TabPane
        StackPane theTab = new StackPane(tabPane);
        theTab.getStyleClass().add("ad-card");

        HBox hangKpi = taoHangKpi();
        VBox noiDung = new VBox(18, hangKpi, theTab);
        noiDung.setPadding(new Insets(20, 28, 24, 28));
        VBox.setVgrow(theTab, Priority.ALWAYS);
        root.setCenter(noiDung);

        // Lấy dữ liệu lần đầu tiên khi mở màn hình Admin
        yeuCauDuLieuAdmin();

        Scene scene = new Scene(root, 1180, 740);
        scene.getStylesheets().add(duongDanCss());
        scene.getStylesheets().add(getClass().getResource("/profile.css").toExternalForm()); // dùng chung màu heatmap với màn Hồ sơ
        scene.getStylesheets().add(getClass().getResource("/admin.css").toExternalForm());   // giao diện riêng của màn Admin

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        hieuUngXuatHien(hangKpi);
    }

    private String duongDanCss() {
        return getClass().getResource("/game-style.css").toExternalForm();
    }

    private void apDungStyle(Alert alert) {
        DialogStyle.apply(alert);
    }

    // =================================================================== HÀM PHỤ DỰNG GIAO DIỆN

    /** Tạo 1 icon: Region có hình dạng lấy từ CSS (vd "ic-users"), kích thước co x co. */
    private Region taoIcon(String loai, double co) {
        Region icon = new Region();
        icon.getStyleClass().addAll("ad-icon", loai);
        icon.setMinSize(co, co);
        icon.setPrefSize(co, co);
        icon.setMaxSize(co, co);
        return icon;
    }

    /** Tạo nút có icon. kieu = "ad-btn-primary" (cam) / "ad-btn-ghost" (viền trắng) / ... */
    private Button taoNut(String chu, String icon, String kieu) {
        Button nut = new Button(chu, taoIcon(icon, 15));
        nut.getStyleClass().addAll("ad-btn", kieu);
        return nut;
    }

    private <T> void tuyChinhBang(TableView<T> bang) {
        bang.getStyleClass().add("ad-table");
        bang.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bang.setPlaceholder(taoTrangTrong("ic-refresh", "Đang tải dữ liệu từ trung tâm...", "Vui lòng chờ trong giây lát"));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(bang.widthProperty());
        clip.heightProperty().bind(bang.heightProperty());
        bang.setClip(clip);

        VBox.setVgrow(bang, Priority.ALWAYS);
    }

    /** Màn hình trống (bảng chưa có dữ liệu): icon tròn + tiêu đề + dòng mô tả. */
    private VBox taoTrangTrong(String icon, String tieuDe, String moTa) {
        StackPane khungIcon = new StackPane(taoIcon(icon, 34));
        khungIcon.getStyleClass().add("ad-empty-icon");
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("ad-empty-title");
        Label lblMoTa = new Label(moTa);
        lblMoTa.getStyleClass().add("ad-empty-sub");
        VBox box = new VBox(8, khungIcon, lblTieuDe, lblMoTa);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** Thanh đầu mỗi tab: tiêu đề + mô tả bên trái, các nút bên phải. */
    private HBox taoThanhCongCu(String tieuDe, String moTa, Node... cacNut) {
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("ad-section-title");
        Label lblMoTa = new Label(moTa);
        lblMoTa.getStyleClass().add("ad-section-sub");
        VBox chu = new VBox(2, lblTieuDe, lblMoTa);

        Region khoangTrong = new Region();
        HBox.setHgrow(khoangTrong, Priority.ALWAYS);

        HBox toolbar = new HBox(10);
        toolbar.getChildren().addAll(chu, khoangTrong);
        toolbar.getChildren().addAll(cacNut);
        toolbar.setPadding(new Insets(14, 20, 14, 20));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private VBox taoNoiDungTab(HBox toolbar, TableView<?> bang) {
        VBox.setMargin(bang, new Insets(0, 20, 20, 20));
        return new VBox(toolbar, bang);
    }

    /** Tạo Tab có icon bên trái chữ. */
    private Tab taoTab(String ten, String icon, Node noiDung) {
        Tab tab = new Tab(ten, noiDung);
        tab.setGraphic(taoIcon(icon, 16));
        return tab;
    }

    /** Huy hiệu (pill) cho trạng thái / quyền hạn: trả về {lớp CSS, chữ hiển thị}. */
    private String[] kieuHuyHieu(String giaTri) {
        String g = giaTri == null ? "" : giaTri.toUpperCase();
        switch (g) {
            case "ONLINE":    return new String[]{"ad-pill-online", "Trực tuyến"};
            case "OFFLINE":   return new String[]{"ad-pill-offline", "Ngoại tuyến"};
            case "IN_GAME":   return new String[]{"ad-pill-busy", "Đang thi đấu"};
            case "ADMIN":     return new String[]{"ad-pill-admin", "Quản trị viên"};
            case "USER":      return new String[]{"ad-pill-user", "Người chơi"};
            case "PLAYING":   return new String[]{"ad-pill-live", "Đang giao tranh"};
            case "COUNTDOWN": return new String[]{"ad-pill-busy", "Đang đếm ngược"};
            case "CREATED":   return new String[]{"ad-pill-offline", "Vừa tạo phòng"};
            case "FINISHED":  return new String[]{"ad-pill-offline", "Chờ tái đấu"};
            default:          return new String[]{"", giaTri};
        }
    }

    /** Màu avatar cố định theo tên (cùng 1 tên luôn ra cùng 1 màu). */
    private Label taoAvatarNho(String ten) {
        String t = (ten == null || ten.isEmpty()) ? "?" : ten;
        Label av = new Label(t.substring(0, 1).toUpperCase());
        av.getStyleClass().addAll("ad-avatar-sm", "ad-av-" + Math.abs(t.hashCode() % 6));
        return av;
    }

    /** Cột chữ thường (vd mã trận "#12") với lớp CSS cho chữ. */
    private TableColumn<String[], String> taoCotChu(String tieuDe, int viTri, String tienTo, String lopCss) {
        TableColumn<String[], String> cot = new TableColumn<>(tieuDe);
        cot.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[viTri]));
        cot.setCellFactory(c -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove(lopCss);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(tienTo + item);
                    getStyleClass().add(lopCss);
                }
            }
        });
        return cot;
    }

    /** Cột huy hiệu: ONLINE / OFFLINE / ADMIN ... hiển thị dạng viên thuốc có chấm màu hoặc icon. */
    private TableColumn<String[], String> taoCotHuyHieu(String tieuDe, int viTri, boolean coIcon) {
        TableColumn<String[], String> cot = new TableColumn<>(tieuDe);
        cot.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[viTri]));
        cot.setCellFactory(c -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                String[] kieu = kieuHuyHieu(item);
                Label pill = new Label(kieu[1]);
                pill.getStyleClass().add("ad-pill");
                if (!kieu[0].isEmpty()) pill.getStyleClass().add(kieu[0]);
                if (coIcon) {
                    pill.setGraphic(taoIcon("ADMIN".equalsIgnoreCase(item) ? "ic-shield" : "ic-person", 12));
                } else {
                    Circle cham = new Circle(4);
                    cham.getStyleClass().add("ad-dot");
                    pill.setGraphic(cham);
                }
                setGraphic(pill);
            }
        });
        return cot;
    }

    /** Hai người chơi trên 1 dòng: P1  VS  P2 (coAvatar = true thì thêm avatar tròn trước mỗi tên) */
    private TableColumn<String[], String[]> taoCotDoiDau(String tieuDe, boolean coAvatar) {
        TableColumn<String[], String[]> cot = new TableColumn<>(tieuDe);
        cot.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        cot.setCellFactory(c -> new TableCell<String[], String[]>() {
            @Override
            protected void updateItem(String[] dong, boolean empty) {
                super.updateItem(dong, empty);
                setText(null);
                if (empty || dong == null) {
                    setGraphic(null);
                    return;
                }
                Label p1 = new Label(dong[1]);
                p1.getStyleClass().add("ad-ten");
                Label vs = new Label("VS");
                vs.getStyleClass().add("ad-vs");
                Label p2 = new Label(dong[2]);
                p2.getStyleClass().add("ad-ten");
                HBox hang = coAvatar
                        ? new HBox(8, taoAvatarNho(dong[1]), p1, vs, taoAvatarNho(dong[2]), p2)
                        : new HBox(10, p1, vs, p2);
                hang.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hang);
            }
        });
        return cot;
    }

    // =================================================================== HEADER + THẺ SỐ LIỆU

    private HBox taoThanhTieuDe() {
        String ten = (currentAdminUsername == null || currentAdminUsername.isEmpty()) ? "Admin" : currentAdminUsername;

        // Logo: ô vuông cam bo góc + icon tâm ngắm
        StackPane logo = new StackPane(taoIcon("ic-target", 26));
        logo.getStyleClass().add("ad-logo");

        Label lblTitle = new Label("BATTLESHIP");
        lblTitle.getStyleClass().add("ad-brand");
        // JavaFX CSS không có letter-spacing nên chèn khoảng trắng thủ công
        Label lblSub = new Label("C O M M A N D   C E N T E R   ·   B Ả N G   Đ I Ề U   K H I Ể N");
        lblSub.getStyleClass().add("ad-brand-sub");
        VBox titleBox = new VBox(0, lblTitle, lblSub);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(14, logo, titleBox);
        brand.setAlignment(Pos.CENTER_LEFT);

        // Thẻ admin: avatar + tên + "QUẢN TRỊ VIÊN"
        Label avatar = new Label(ten.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("ad-avatar");

        lblUser = new Label(ten);
        lblUser.getStyleClass().add("ad-chip-name");
        Label lblBadge = new Label("QUẢN TRỊ VIÊN", taoIcon("ic-shield", 11));
        lblBadge.getStyleClass().add("ad-chip-role");
        VBox textBox = new VBox(1, lblUser, lblBadge);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox chip = new HBox(10, avatar, textBox);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("ad-chip");

        Button btnLogout = new Button("Đăng xuất", taoIcon("ic-logout", 15));
        btnLogout.getStyleClass().add("ad-btn-logout");
        btnLogout.setOnAction(e -> {
            SocketClientManager.guiTinNhan(new MessageDTO("LOGOUT", null));
            SocketClientManager.dongKetNoi();
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.start(primaryStage);
        });

        Region khoangTrong = new Region();
        HBox.setHgrow(khoangTrong, Priority.ALWAYS);

        Region iconLoa = taoIcon(SplashGUI.tatTieng ? "ic-mute" : "ic-volume", 18);
        Button btnAmThanh = new Button();
        btnAmThanh.setGraphic(iconLoa);
        btnAmThanh.getStyleClass().add("ad-btn-sound");
        btnAmThanh.setOnAction(e -> {
            SplashGUI.doiTieng();
            iconLoa.getStyleClass().removeAll("ic-volume", "ic-mute");
            iconLoa.getStyleClass().add(SplashGUI.tatTieng ? "ic-mute" : "ic-volume");
        });

        HBox box = new HBox(16, brand, khoangTrong, btnAmThanh, chip, btnLogout);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("ad-header");
        return box;
    }

    /** 1 thẻ số liệu: ô icon màu + con số lớn + nhãn. */
    private HBox taoTheKpi(String icon, String mauIcon, Label lblSo, String nhan, Node phuLuc) {
        StackPane khungIcon = new StackPane(taoIcon(icon, 24));
        khungIcon.getStyleClass().addAll("ad-kpi-icon", mauIcon);

        lblSo.getStyleClass().add("ad-kpi-so");
        HBox hangSo = new HBox(8, lblSo);
        hangSo.setAlignment(Pos.CENTER_LEFT);
        if (phuLuc != null) hangSo.getChildren().add(phuLuc);

        Label lblNhan = new Label(nhan);
        lblNhan.getStyleClass().add("ad-kpi-nhan");
        VBox chu = new VBox(0, hangSo, lblNhan);
        chu.setAlignment(Pos.CENTER_LEFT);

        HBox the = new HBox(14, khungIcon, chu);
        the.setAlignment(Pos.CENTER_LEFT);
        the.getStyleClass().add("ad-kpi");
        HBox.setHgrow(the, Priority.ALWAYS);
        the.setMaxWidth(Double.MAX_VALUE);
        return the;
    }

    private HBox taoHangKpi() {
        // Chấm đỏ nhấp nháy cạnh số "trận đang diễn ra" cho cảm giác LIVE
        Circle chamLive = new Circle(5);
        chamLive.getStyleClass().add("ad-live-dot");
        FadeTransition nhapNhay = new FadeTransition(Duration.millis(800), chamLive);
        nhapNhay.setFromValue(1.0);
        nhapNhay.setToValue(0.2);
        nhapNhay.setAutoReverse(true);
        nhapNhay.setCycleCount(FadeTransition.INDEFINITE);
        nhapNhay.play();

        HBox hang = new HBox(16,
                taoTheKpi("ic-users", "ad-kpi-xanh", lblKpiTongTaiKhoan, "TỔNG TÀI KHOẢN", null),
                taoTheKpi("ic-person", "ad-kpi-ngoc", lblKpiDangOnline, "ĐANG TRỰC TUYẾN", null),
                taoTheKpi("ic-target", "ad-kpi-cam", lblKpiDangDau, "TRẬN ĐANG DIỄN RA", chamLive),
                taoTheKpi("ic-trophy", "ad-kpi-tim", lblKpiDaKetThuc, "TRẬN ĐÃ KẾT THÚC", null));
        hang.setAlignment(Pos.TOP_LEFT);
        hang.setFillHeight(true);
        return hang;
    }

    /** Đếm lại 4 con số trên đầu từ dữ liệu vừa nhận (chỉ hiển thị, không gửi gì lên server). */
    private void capNhatKpi() {
        int online = 0;
        for (String[] u : dsUsers) {
            if (!"OFFLINE".equalsIgnoreCase(u[4])) online++;
        }
        lblKpiTongTaiKhoan.setText(String.valueOf(dsUsers.size()));
        lblKpiDangOnline.setText(String.valueOf(online));
        // Chỉ tính phòng đang đếm ngược / đang bắn; phòng FINISHED là đã đánh xong, đang chờ tái đấu
        int dangDau = 0;
        for (String[] m : dsMatches) {
            if ("PLAYING".equalsIgnoreCase(m[3]) || "COUNTDOWN".equalsIgnoreCase(m[3])) dangDau++;
        }
        lblKpiDangDau.setText(String.valueOf(dangDau));
        lblKpiDaKetThuc.setText(String.valueOf(dsFinishedMatches.size()));
    }

    /** Hiệu ứng mở màn: 4 thẻ số liệu lần lượt hiện dần (chỉ đổi độ mờ, không dịch chuyển vị trí). */
    private void hieuUngXuatHien(HBox hangKpi) {
        int i = 0;
        for (Node the : hangKpi.getChildren()) {
            the.setOpacity(0);
            FadeTransition hien = new FadeTransition(Duration.millis(450), the);
            hien.setToValue(1);
            hien.setDelay(Duration.millis(90 * i));
            hien.play();
            i++;
        }
    }

    // =================================================================== TAB 1: QUẢN LÝ TÀI KHOẢN

    private Tab taoTabQuanLyTaiKhoan() {
        Button btnRefresh = taoNut("Làm mới", "ic-refresh", "ad-btn-ghost");
        btnRefresh.setOnAction(e -> yeuCauDuLieuAdmin());

        Button btnHoSo = taoNut("Xem hồ sơ người chơi", "ic-person", "ad-btn-primary");
        btnHoSo.setOnAction(e -> moHoSoNguoiChoi());

        HBox toolbar = taoThanhCongCu("Quản lý tài khoản",
                "Chọn 1 người chơi trong bảng rồi bấm \"Xem hồ sơ\" để xem thống kê và heatmap cá nhân",
                btnRefresh, btnHoSo);

        tblUsers = new TableView<>();
        tuyChinhBang(tblUsers);
        tblUsers.setPlaceholder(taoTrangTrong("ic-users", "Chưa có tài khoản nào", "Bấm \"Làm mới\" để tải lại danh sách"));

        TableColumn<String[], String> colId = taoCotChu("ID", 0, "#", "ad-id");
        colId.setMinWidth(70);
        colId.setMaxWidth(80);

        // Cột người chơi: avatar + tên đăng nhập (đậm) + biệt danh (mờ) — gộp 2 cột cũ cho gọn
        TableColumn<String[], String[]> colUser = new TableColumn<>("NGƯỜI CHƠI");
        colUser.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colUser.setCellFactory(c -> new TableCell<String[], String[]>() {
            @Override
            protected void updateItem(String[] dong, boolean empty) {
                super.updateItem(dong, empty);
                setText(null);
                if (empty || dong == null) {
                    setGraphic(null);
                    return;
                }
                Label ten = new Label(dong[1]);
                ten.getStyleClass().add("ad-ten");
                Label biDanh = new Label("@" + dong[2]);
                biDanh.getStyleClass().add("ad-phu");
                VBox chu = new VBox(0, ten, biDanh);
                chu.setAlignment(Pos.CENTER_LEFT);
                HBox hang = new HBox(10, taoAvatarNho(dong[1]), chu);
                hang.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hang);
            }
        });
        colUser.setMinWidth(200);
        colUser.setPrefWidth(220);

        TableColumn<String[], String> colRole = taoCotHuyHieu("QUYỀN HẠN", 3, true);
        colRole.setMinWidth(150);
        TableColumn<String[], String> colStatus = taoCotHuyHieu("TRẠNG THÁI", 4, false);
        colStatus.setMinWidth(150);

        // Điểm: số đậm + cúp vàng
        TableColumn<String[], String> colScore = new TableColumn<>("ĐIỂM");
        colScore.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[5]));
        colScore.setCellFactory(c -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label diem = new Label(item, taoIcon("ic-trophy", 14));
                diem.getStyleClass().add("ad-diem");
                setGraphic(diem);
            }
        });
        colScore.setMinWidth(110);

        // Thắng / Thua: số thắng xanh, số thua đỏ
        TableColumn<String[], String[]> colRecord = new TableColumn<>("THẮNG / THUA");
        colRecord.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colRecord.setCellFactory(c -> new TableCell<String[], String[]>() {
            @Override
            protected void updateItem(String[] dong, boolean empty) {
                super.updateItem(dong, empty);
                setText(null);
                if (empty || dong == null) {
                    setGraphic(null);
                    return;
                }
                Label thang = new Label(dong[6] + " W");
                thang.getStyleClass().add("ad-thang");
                Label gach = new Label("/");
                gach.getStyleClass().add("ad-gach");
                Label thua = new Label(dong[7] + " L");
                thua.getStyleClass().add("ad-thua");
                HBox hang = new HBox(6, thang, gach, thua);
                hang.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hang);
            }
        });
        colRecord.setMinWidth(150);

        // Tỷ lệ thắng: thanh tiến độ nhỏ + phần trăm (tính từ thắng/thua để hiển thị)
        TableColumn<String[], String[]> colTyLe = new TableColumn<>("TỶ LỆ THẮNG");
        colTyLe.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colTyLe.setCellFactory(c -> new TableCell<String[], String[]>() {
            @Override
            protected void updateItem(String[] dong, boolean empty) {
                super.updateItem(dong, empty);
                setText(null);
                if (empty || dong == null) {
                    setGraphic(null);
                    return;
                }
                int thang = Integer.parseInt(dong[6]);
                int thua = Integer.parseInt(dong[7]);
                double tyLe = (thang + thua) == 0 ? 0 : (double) thang / (thang + thua);
                ProgressBar thanh = new ProgressBar(tyLe);
                thanh.getStyleClass().add("ad-bar");
                thanh.setPrefWidth(80);
                Label phanTram = new Label(Math.round(tyLe * 100) + "%");
                phanTram.getStyleClass().add("ad-phantram");
                HBox hang = new HBox(8, thanh, phanTram);
                hang.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hang);
            }
        });
        colTyLe.setMinWidth(150);

        tblUsers.getColumns().addAll(colId, colUser, colRole, colStatus, colScore, colRecord, colTyLe);
        tblUsers.setItems(dsUsers);

        return taoTab("Quản lý tài khoản", "ic-users", taoNoiDungTab(toolbar, tblUsers));
    }

    // =================================================================== TAB 3: BIỂU ĐỒ NHIỆT

    // 2 lưới hiển thị nhiệt độ (10x10 ô)
    private static final int SO_O = 10;
    private static final int SO_MUC = 5;
    private GridPane gridPlacementHeatmap = new GridPane();
    private GridPane gridShotHeatmap = new GridPane();

    private Tab taoTabBieuDoNhiet() {
        Button btnRefresh = taoNut("Làm mới biểu đồ", "ic-refresh", "ad-btn-primary");
        btnRefresh.setOnAction(e -> {
            SocketClientManager.guiTinNhan(new MessageDTO("GET_HEATMAP", null));
        });

        HBox toolbar = taoThanhCongCu("Biểu đồ nhiệt toàn server",
                "Tổng hợp vị trí đặt tàu và các ô bị nhắm bắn của tất cả người chơi",
                btnRefresh);

        VBox boxPlacement = taoKhungHeatmapAdmin("HEATMAP ĐẶT TÀU", "Ô đặt tàu nhiều → màu đậm · số = số lần đặt", "heat-ship", gridPlacementHeatmap);
        VBox boxShot = taoKhungHeatmapAdmin("HEATMAP NHẮM BẮN", "Ô bị bắn nhiều → màu đậm · số = số lần bắn", "heat-shot", gridShotHeatmap);

        HBox heatmapsBox = new HBox(18, boxPlacement, boxShot);
        heatmapsBox.setAlignment(Pos.TOP_CENTER);
        heatmapsBox.setPadding(new Insets(0, 20, 20, 20));

        ScrollPane scrollPane = new ScrollPane(heatmapsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("ad-scroll");

        VBox content = new VBox(toolbar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return taoTab("Biểu đồ nhiệt", "ic-grid", content);
    }

    /** Một khung heatmap giống màn Hồ sơ: tiêu đề, chú thích, lưới 10x10 (nhãn A-J, 1-10), chú giải màu. */
    private VBox taoKhungHeatmapAdmin(String tieuDe, String chuThich, String tienTo, GridPane luoi) {
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("profile-heat-tieude");
        Label lblChuThich = new Label(chuThich);
        lblChuThich.getStyleClass().add("profile-chu-nho");

        khoiTaoLuoiHeatmap(luoi, tienTo);

        HBox chuGiai = new HBox(4);
        chuGiai.setAlignment(Pos.CENTER);
        Label it = new Label("Ít");
        it.getStyleClass().add("profile-chu-nho");
        chuGiai.getChildren().add(it);
        for (int muc = 0; muc <= SO_MUC; muc++) {
            Label o = new Label("");
            o.getStyleClass().addAll("heat-chugiai", tienTo + "-" + muc);
            chuGiai.getChildren().add(o);
        }
        Label nhieu = new Label("Nhiều");
        nhieu.getStyleClass().add("profile-chu-nho");
        chuGiai.getChildren().add(nhieu);

        VBox khung = new VBox(8, lblTieuDe, lblChuThich, luoi, chuGiai);
        khung.getStyleClass().add("profile-heat-khung");
        khung.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(khung, Priority.ALWAYS);
        khung.setMaxWidth(Double.MAX_VALUE);
        return khung;
    }

    // Quy ước tọa độ của server: chữ = HÀNG (A-J), số = CỘT (1-10)
    private void khoiTaoLuoiHeatmap(GridPane grid, String tienTo) {
        grid.getChildren().clear();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setAlignment(Pos.CENTER);

        for (int c = 0; c < SO_O; c++) {
            Label nhan = new Label(String.valueOf(c + 1));
            nhan.getStyleClass().add("heat-nhan");
            grid.add(nhan, c + 1, 0);
        }
        for (int r = 0; r < SO_O; r++) {
            Label nhan = new Label(String.valueOf((char) ('A' + r)));
            nhan.getStyleClass().add("heat-nhan");
            grid.add(nhan, 0, r + 1);
        }
        for (int r = 0; r < SO_O; r++) {
            for (int c = 0; c < SO_O; c++) {
                Label o = new Label("");
                o.getStyleClass().addAll("heat-cell", tienTo + "-0");
                grid.add(o, c + 1, r + 1);
            }
        }
    }

    // =================================================================== TAB 2: GIÁM SÁT TRẬN ĐẤU (SPECTATE)

    private Tab taoTabGiamSatTranDau() {
        Button btnRefresh = taoNut("Làm mới phòng", "ic-refresh", "ad-btn-ghost");
        btnRefresh.setOnAction(e -> yeuCauDuLieuAdmin());

        HBox toolbar = taoThanhCongCu("Giám sát trận đấu",
                "Các phòng đang thi đấu theo thời gian thực — bấm \"Xem trực tiếp\" để vào xem",
                btnRefresh);

        tblMatches = new TableView<>();
        tuyChinhBang(tblMatches);
        tblMatches.setPlaceholder(taoTrangTrong("ic-eye", "Hiện không có trận nào đang diễn ra", "Khi người chơi vào trận, phòng sẽ hiện ở đây"));

        TableColumn<String[], String> colMatchId = taoCotChu("MÃ TRẬN", 0, "#", "ad-id");
        colMatchId.setMaxWidth(110);

        TableColumn<String[], String[]> colDoiDau = taoCotDoiDau("NGƯỜI CHƠI 1  VS  NGƯỜI CHƠI 2", true);
        colDoiDau.setPrefWidth(320);

        TableColumn<String[], String> colStatus = taoCotHuyHieu("TRẠNG THÁI", 3, false);

        TableColumn<String[], Void> colAction = new TableColumn<>("HÀNH ĐỘNG");
        colAction.setPrefWidth(160);
        colAction.setCellFactory(col -> new TableCell<String[], Void>() {
            private final Button btnSpectate = taoNut("Xem trực tiếp", "ic-eye", "ad-btn-live");
            {
                btnSpectate.getStyleClass().add("ad-btn-sm");
                btnSpectate.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    String roomId = row[0]; // Hoặc matchId tùy cách truyền từ server

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

        tblMatches.getColumns().addAll(colMatchId, colDoiDau, colStatus, colAction);
        tblMatches.setItems(dsMatches);

        return taoTab("Giám sát trận đấu", "ic-eye", taoNoiDungTab(toolbar, tblMatches));
    }

    // =================================================================== TAB 4: LỊCH SỬ TRẬN ĐẤU (REPLAY)

    private Tab taoTabLichSuReplay() {
        Button btnRefresh = taoNut("Làm mới danh sách", "ic-refresh", "ad-btn-ghost");
        btnRefresh.setOnAction(e -> yeuCauDuLieuAdmin());

        HBox toolbar = taoThanhCongCu("Lịch sử trận đấu",
                "Các trận đã kết thúc — bấm \"Xem lại\" để phát lại từng lượt bắn",
                btnRefresh);

        tblFinishedMatches = new TableView<>();
        tuyChinhBang(tblFinishedMatches);
        tblFinishedMatches.setPlaceholder(taoTrangTrong("ic-history", "Chưa có trận nào kết thúc", "Lịch sử sẽ xuất hiện sau khi có trận đấu hoàn tất"));

        TableColumn<String[], String> colId = taoCotChu("MÃ TRẬN", 0, "#", "ad-id");
        colId.setMaxWidth(100);

        TableColumn<String[], String[]> colDoiDau = taoCotDoiDau("TRẬN ĐẤU", false);
        colDoiDau.setPrefWidth(280);

        // Người thắng: huy hiệu vàng có cúp
        TableColumn<String[], String> colWin = new TableColumn<>("NGƯỜI THẮNG");
        colWin.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        colWin.setCellFactory(c -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(item, taoIcon("ic-trophy", 12));
                pill.getStyleClass().addAll("ad-pill", "ad-pill-win");
                setGraphic(pill);
            }
        });

        // Thời gian: bỏ phần ".0" mili-giây ở cuối cho gọn, kèm icon đồng hồ
        TableColumn<String[], String> colTime = new TableColumn<>("THỜI GIAN ĐẤU");
        colTime.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[4]));
        colTime.setCellFactory(c -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                String hienThi = item.length() > 16 ? item.substring(0, 16) : item;
                Label lbl = new Label(hienThi, taoIcon("ic-clock", 13));
                lbl.getStyleClass().add("ad-thoigian");
                setGraphic(lbl);
            }
        });

        TableColumn<String[], Void> colAction = new TableColumn<>("THAO TÁC");
        colAction.setPrefWidth(140);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnReplay = taoNut("Xem lại", "ic-play", "ad-btn-teal");
            {
                btnReplay.getStyleClass().add("ad-btn-sm");
                btnReplay.setOnAction(e -> {
                    try {
                    String[] row = getTableView().getItems().get(getIndex());
                    if (row == null || row.length == 0) return;

                    int matchId = (int) Double.parseDouble(row[0].trim());
                    System.out.println(">>> [CLIENT ADMIN] Da bam nut Xem lai tran #" + matchId);

                    java.util.Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("matchId", matchId);
                    SocketClientManager.guiTinNhan(new MessageDTO("GET_REPLAY_DATA", payload));
                    System.out.println(">>> [CLIENT ADMIN] Da gui lenh GET_REPLAY_DATA len Server");
                } catch (Exception ex) {
                    System.err.println(">>> [CLIENT ADMIN] Loi khi bam Xem lai: " + ex.getMessage());
                    ex.printStackTrace();
                }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnReplay);
            }
        });

        tblFinishedMatches.getColumns().addAll(colId, colDoiDau, colWin, colTime, colAction);
        tblFinishedMatches.setItems(dsFinishedMatches);

        return taoTab("Lịch sử trận đấu", "ic-history", taoNoiDungTab(toolbar, tblFinishedMatches));
    }

    // =================================================================== XỬ LÝ DỮ LIỆU (giữ nguyên logic cũ)

    // Mở màn Hồ sơ của người đang chọn trong bảng, rồi xin server dữ liệu (GET_PROFILE)
    private void moHoSoNguoiChoi() {
        String[] dong = tblUsers.getSelectionModel().getSelectedItem();
        if (dong == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Hãy chọn một người chơi trong bảng trước.");
            alert.setHeaderText("Chưa chọn người chơi");
            apDungStyle(alert);
            alert.showAndWait();
            return;
        }
        ProfileGUI.tenNguoiChoi = dong[2].isEmpty() ? dong[1] : dong[2];
        ProfileGUI.diemNguoiChoi = dong[5];
        ProfileGUI hoSo = new ProfileGUI();
        hoSo.start(new Stage());
        SocketClientManager.profileScreen = hoSo;
        java.util.Map<String, Object> goi = new java.util.HashMap<>();
        goi.put("userId", Integer.parseInt(dong[0]));
        SocketClientManager.guiTinNhan(new MessageDTO("GET_PROFILE", goi));
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
        String tienTo = isPlacement ? "heat-ship" : "heat-shot";
        int maxVal = 0;
        for (String key : dataMap.keySet()) {
            maxVal = Math.max(maxVal, dataMap.get(key).getAsInt());
        }

        for (javafx.scene.Node node : grid.getChildren()) {
            Integer c = GridPane.getColumnIndex(node);
            Integer r = GridPane.getRowIndex(node);
            // Hàng/cột 0 là nhãn A-J, 1-10 nên chỉ xử lý các ô heatmap (r >= 1, c >= 1)
            if (r == null || c == null || r < 1 || c < 1 || !(node instanceof Label)) continue;
            Label cell = (Label) node;
            String coord = "" + (char) ('A' + r - 1) + c;
            int count = dataMap.has(coord) ? dataMap.get(coord).getAsInt() : 0;

            for (int k = 0; k <= SO_MUC; k++) {
                cell.getStyleClass().remove(tienTo + "-" + k);
            }
            int muc = 0;
            if (count > 0 && maxVal > 0) {
                muc = Math.max(1, Math.min(SO_MUC, (int) Math.ceil((double) count / maxVal * SO_MUC)));
            }
            cell.getStyleClass().add(tienTo + "-" + muc);
            cell.setText(count > 0 ? String.valueOf(count) : "");
            cell.setTooltip(count > 0 ? new Tooltip(coord + ": " + count + " lần") : null);
        }
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

            if (payload.has("finishedMatches")) {
                dsFinishedMatches.clear();
                JsonArray arr = payload.getAsJsonArray("finishedMatches");
                for (JsonElement el : arr) {
                    JsonObject m = el.getAsJsonObject();
                    // Ép sang int trước khi chuyển sang chuỗi
                    String id = String.valueOf((int) m.get("matchId").getAsDouble());
                    String p1 = m.get("player1Name").getAsString();
                    String p2 = m.get("player2Name").getAsString();
                    String win = m.get("winnerName").getAsString();
                    String time = m.get("playedAt").getAsString();
                    dsFinishedMatches.add(new String[]{id, p1, p2, win, time});
                }
            }

            // 3. Đếm lại 4 thẻ số liệu trên đầu màn hình
            capNhatKpi();
        });
    }
}
