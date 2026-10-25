package com.mycompany.battleship.client.gui;

import com.mycompany.battleship.common.model.ProfileDTO;
import com.mycompany.battleship.common.model.UserCombatStats;
import com.mycompany.battleship.common.model.UserShipPlacement;
import com.mycompany.battleship.common.model.UserShotTarget;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Màn hình hồ sơ người chơi (Profile): chiến tích cá nhân + 2 heatmap 10x10.
 *  - Heatmap đặt tàu: ô đặt tàu càng nhiều lần thì màu càng đậm.
 *  - Heatmap nhắm bắn: màu đậm = ô hay bắn, con số trong ô = tỷ lệ trúng (%).
 * Dữ liệu do server gửi về, màn này chỉ hiển thị (xem các hàm capNhat...).
 */
public class ProfileGUI extends Application {

    private static final int SO_O = 10;          // bàn cờ 10x10
    private static final int SO_MUC = 5;         // số mức đậm nhạt của heatmap
    private static final String CHU_COT = "ABCDEFGHIJ";

    public static String tenNguoiChoi = "";
    public static String diemNguoiChoi = "0";

    private Stage primaryStage;

    // Các nhãn thống kê
    private Label lblAvatar;
    private Label lblTen;
    private Label lblDiem;
    private Label lblTyLe;
    private ProgressBar barTyLe;
    private Label lblTyLeChiTiet;
    private Label lblWinHienTai;
    private Label lblWinCaoNhat;
    private Label lblLossHienTai;
    private Label lblLossCaoNhat;

    // Hai lưới heatmap: mỗi ô là 1 Label (chỉ số [hàng][cột], bắt đầu từ 0)
    private Label[][] oDatTau = new Label[SO_O][SO_O];
    private Label[][] oNhamBan = new Label[SO_O][SO_O];

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Battleship Online - Hồ sơ người chơi");

        VBox root = new VBox(18);
        root.getStyleClass().add("profile-root");
        root.setPadding(new Insets(22, 28, 24, 28));
        root.getChildren().addAll(taoThanhTieuDe(), taoHangThongKe(), taoHangHeatmap());

        Scene scene = new Scene(root, 960, 690);
        scene.getStylesheets().add(getClass().getResource("/profile.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        capNhatTieuDe(tenNguoiChoi, diemNguoiChoi);
        // Chạy riêng màn này (chưa có người chơi thật) thì hiện dữ liệu mẫu để xem giao diện
        if (tenNguoiChoi.isEmpty()) {
            napDuLieuMau();
        }
    }

    // ------------------------------------------------------------------ dựng giao diện

    private HBox taoThanhTieuDe() {
        lblAvatar = new Label("?");
        lblAvatar.getStyleClass().add("profile-avatar");

        lblTen = new Label("Người chơi");
        lblTen.getStyleClass().add("profile-ten");
        lblDiem = new Label("Điểm: 0");
        lblDiem.getStyleClass().add("profile-diem");
        VBox thongTin = new VBox(2, lblTen, lblDiem);
        thongTin.setAlignment(Pos.CENTER_LEFT);

        Region khoang = new Region();
        HBox.setHgrow(khoang, Priority.ALWAYS);

        Button btnQuayLai = new Button("← Quay lại");
        btnQuayLai.getStyleClass().add("profile-btn-back");
        btnQuayLai.setOnAction(e -> primaryStage.close());

        HBox hang = new HBox(14, lblAvatar, thongTin, khoang, btnQuayLai);
        hang.setAlignment(Pos.CENTER_LEFT);
        return hang;
    }

    private HBox taoHangThongKe() {
        // Thẻ 1: tỷ lệ bắn trúng
        lblTyLe = new Label("0%");
        lblTyLe.getStyleClass().add("profile-so-lon");
        barTyLe = new ProgressBar(0);
        barTyLe.getStyleClass().add("profile-bar");
        barTyLe.setMaxWidth(Double.MAX_VALUE);
        lblTyLeChiTiet = new Label("0 trúng / 0 phát bắn");
        lblTyLeChiTiet.getStyleClass().add("profile-chu-nho");
        VBox theTyLe = taoThe("TỶ LỆ BẮN TRÚNG", "profile-the-tyle", lblTyLe, barTyLe, lblTyLeChiTiet);

        // Thẻ 2: chuỗi thắng
        lblWinHienTai = new Label("0");
        lblWinHienTai.getStyleClass().add("profile-so-lon");
        lblWinCaoNhat = new Label("Cao nhất: 0");
        lblWinCaoNhat.getStyleClass().add("profile-chu-nho");
        VBox theThang = taoThe("CHUỖI THẮNG HIỆN TẠI", "profile-the-thang", lblWinHienTai, lblWinCaoNhat);

        // Thẻ 3: chuỗi thua
        lblLossHienTai = new Label("0");
        lblLossHienTai.getStyleClass().add("profile-so-lon");
        lblLossCaoNhat = new Label("Cao nhất: 0");
        lblLossCaoNhat.getStyleClass().add("profile-chu-nho");
        VBox theThua = taoThe("CHUỖI THUA HIỆN TẠI", "profile-the-thua", lblLossHienTai, lblLossCaoNhat);

        HBox hang = new HBox(16, theTyLe, theThang, theThua);
        HBox.setHgrow(theTyLe, Priority.ALWAYS);
        HBox.setHgrow(theThang, Priority.ALWAYS);
        HBox.setHgrow(theThua, Priority.ALWAYS);
        theTyLe.setMaxWidth(Double.MAX_VALUE);
        theThang.setMaxWidth(Double.MAX_VALUE);
        theThua.setMaxWidth(Double.MAX_VALUE);
        return hang;
    }

    private VBox taoThe(String tieuDe, String lopMau, javafx.scene.Node... noiDung) {
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("profile-the-tieude");
        VBox the = new VBox(6);
        the.getStyleClass().addAll("profile-the", lopMau);
        the.getChildren().add(lblTieuDe);
        the.getChildren().addAll(noiDung);
        return the;
    }

    private HBox taoHangHeatmap() {
        VBox boDatTau = taoKhungHeatmap("HEATMAP ĐẶT TÀU", "Ô đặt tàu nhiều → màu đậm", "heat-ship", oDatTau);
        VBox boNhamBan = taoKhungHeatmap("HEATMAP NHẮM BẮN", "Màu đậm = hay bắn · số = tỷ lệ trúng (%)", "heat-shot", oNhamBan);
        HBox hang = new HBox(16, boDatTau, boNhamBan);
        HBox.setHgrow(boDatTau, Priority.ALWAYS);
        HBox.setHgrow(boNhamBan, Priority.ALWAYS);
        boDatTau.setMaxWidth(Double.MAX_VALUE);
        boNhamBan.setMaxWidth(Double.MAX_VALUE);
        return hang;
    }

    /** Một khung gồm tiêu đề, chú thích, lưới 10x10 (có nhãn A-J, 1-10) và thanh chú giải màu. */
    private VBox taoKhungHeatmap(String tieuDe, String chuThich, String tienTo, Label[][] cacO) {
        Label lblTieuDe = new Label(tieuDe);
        lblTieuDe.getStyleClass().add("profile-heat-tieude");
        Label lblChuThich = new Label(chuThich);
        lblChuThich.getStyleClass().add("profile-chu-nho");

        GridPane luoi = new GridPane();
        luoi.setHgap(2);
        luoi.setVgap(2);
        luoi.setAlignment(Pos.CENTER);

        // Quy ước server: chữ = HÀNG (A-J), số = CỘT (1-10). Nhãn cột 1-10 ở trên, nhãn hàng A-J bên trái
        for (int c = 0; c < SO_O; c++) {
            luoi.add(taoNhan(String.valueOf(c + 1)), c + 1, 0);
        }
        for (int r = 0; r < SO_O; r++) {
            luoi.add(taoNhan(String.valueOf(CHU_COT.charAt(r))), 0, r + 1);
        }
        // 100 ô heatmap
        for (int r = 0; r < SO_O; r++) {
            for (int c = 0; c < SO_O; c++) {
                Label o = new Label("");
                o.getStyleClass().addAll("heat-cell", tienTo + "-0");
                cacO[r][c] = o;
                luoi.add(o, c + 1, r + 1);
            }
        }

        HBox chuGiai = taoChuGiai(tienTo);

        VBox khung = new VBox(8, lblTieuDe, lblChuThich, luoi, chuGiai);
        khung.getStyleClass().add("profile-heat-khung");
        khung.setAlignment(Pos.TOP_CENTER);
        return khung;
    }

    private Label taoNhan(String chu) {
        Label l = new Label(chu);
        l.getStyleClass().add("heat-nhan");
        return l;
    }

    private HBox taoChuGiai(String tienTo) {
        HBox hang = new HBox(4);
        hang.setAlignment(Pos.CENTER);
        Label it = new Label("Ít");
        it.getStyleClass().add("profile-chu-nho");
        hang.getChildren().add(it);
        for (int muc = 0; muc <= SO_MUC; muc++) {
            Label o = new Label("");
            o.getStyleClass().addAll("heat-chugiai", tienTo + "-" + muc);
            hang.getChildren().add(o);
        }
        Label nhieu = new Label("Nhiều");
        nhieu.getStyleClass().add("profile-chu-nho");
        hang.getChildren().add(nhieu);
        return hang;
    }

    // ------------------------------------------------------------------ nhận dữ liệu từ server

    /** Hiển thị tên + điểm ở thanh tiêu đề. */
    public void capNhatTieuDe(String ten, String diem) {
        String tenHienThi = (ten == null || ten.isEmpty()) ? "Người chơi" : ten;
        lblTen.setText(tenHienThi);
        lblDiem.setText("Điểm: " + diem);
        lblAvatar.setText(tenHienThi.substring(0, 1).toUpperCase());
    }

    /** Nhận gói PROFILE_DATA từ server: cập nhật thống kê + 2 heatmap cùng lúc. */
    public void hienHoSo(ProfileDTO dto) {
        if (dto.thongKe != null) capNhatThongKe(dto.thongKe);
        capNhatHeatmapDatTau(dto.datTau != null ? dto.datTau : new java.util.ArrayList<>());
        capNhatHeatmapNhamBan(dto.oBan != null ? dto.oBan : new java.util.ArrayList<>());
    }

    /** Cập nhật 3 thẻ thống kê: tỷ lệ bắn trúng, chuỗi thắng, chuỗi thua. */
    public void capNhatThongKe(UserCombatStats s) {
        double tyLe = s.getAccuracy();
        lblTyLe.setText(String.format("%.1f%%", tyLe));
        barTyLe.setProgress(tyLe / 100.0);
        lblTyLeChiTiet.setText(s.getHits() + " trúng / " + s.getTotalShots() + " phát bắn");

        lblWinHienTai.setText(String.valueOf(s.getCurrentWinStreak()));
        lblWinCaoNhat.setText("Cao nhất: " + s.getLongestWinStreak());
        lblLossHienTai.setText(String.valueOf(s.getCurrentLossStreak()));
        lblLossCaoNhat.setText("Cao nhất: " + s.getLongestLossStreak());
    }

    /** Vẽ heatmap đặt tàu: placement_count càng lớn ô càng đậm. */
    public void capNhatHeatmapDatTau(List<UserShipPlacement> danhSach) {
        xoaHeatmap(oDatTau, "heat-ship");
        int lonNhat = 0;
        for (UserShipPlacement p : danhSach) {
            lonNhat = Math.max(lonNhat, p.getPlacementCount());
        }
        for (UserShipPlacement p : danhSach) {
            int[] viTri = docToaDo(p.getCoordinate());
            if (viTri == null) continue;
            Label o = oDatTau[viTri[0]][viTri[1]];
            doiMuc(o, "heat-ship", tinhMuc(p.getPlacementCount(), lonNhat));
            o.setTooltip(new Tooltip(p.getCoordinate() + ": đặt tàu " + p.getPlacementCount() + " lần"));
        }
    }

    /** Vẽ heatmap nhắm bắn: màu theo số lần bắn, chữ trong ô là tỷ lệ trúng (%). */
    public void capNhatHeatmapNhamBan(List<UserShotTarget> danhSach) {
        xoaHeatmap(oNhamBan, "heat-shot");
        int lonNhat = 0;
        for (UserShotTarget t : danhSach) {
            lonNhat = Math.max(lonNhat, t.getShotCount());
        }
        for (UserShotTarget t : danhSach) {
            int[] viTri = docToaDo(t.getCoordinate());
            if (viTri == null) continue;
            Label o = oNhamBan[viTri[0]][viTri[1]];
            doiMuc(o, "heat-shot", tinhMuc(t.getShotCount(), lonNhat));
            int phanTram = t.getShotCount() == 0 ? 0 : Math.round(t.getHitCount() * 100f / t.getShotCount());
            o.setText(phanTram + "%");
            o.setTooltip(new Tooltip(t.getCoordinate() + ": bắn " + t.getShotCount()
                    + " lần, trúng " + t.getHitCount() + " (" + phanTram + "%)"));
        }
    }

    // ------------------------------------------------------------------ hàm phụ

    /** Đổi chuỗi "E3" thành {hàng, cột} bắt đầu từ 0 (chữ = hàng, số = cột). Sai định dạng thì trả về null. */
    private int[] docToaDo(String toaDo) {
        if (toaDo == null || toaDo.length() < 2) return null;
        int hang = CHU_COT.indexOf(Character.toUpperCase(toaDo.charAt(0)));
        int cot;
        try {
            cot = Integer.parseInt(toaDo.substring(1)) - 1;
        } catch (NumberFormatException e) {
            return null;
        }
        if (hang < 0 || cot < 0 || cot >= SO_O) return null;
        return new int[]{hang, cot};
    }

    /** Đổi số lần thành mức đậm 1..5 (0 lần = mức 0, không tô). */
    private int tinhMuc(int soLan, int lonNhat) {
        if (soLan <= 0 || lonNhat <= 0) return 0;
        int muc = (int) Math.ceil((double) soLan / lonNhat * SO_MUC);
        return Math.max(1, Math.min(SO_MUC, muc));
    }

    /** Thay class màu cũ của ô bằng class của mức mới (ví dụ heat-ship-3). */
    private void doiMuc(Label o, String tienTo, int muc) {
        for (int i = 0; i <= SO_MUC; i++) {
            o.getStyleClass().remove(tienTo + "-" + i);
        }
        o.getStyleClass().add(tienTo + "-" + muc);
    }

    private void xoaHeatmap(Label[][] cacO, String tienTo) {
        for (int r = 0; r < SO_O; r++) {
            for (int c = 0; c < SO_O; c++) {
                doiMuc(cacO[r][c], tienTo, 0);
                cacO[r][c].setText("");
                cacO[r][c].setTooltip(null);
            }
        }
    }

    /** Dữ liệu mẫu để xem thử giao diện khi chưa nối server. Chạy trực tiếp file này để xem. */
    public void napDuLieuMau() {
        capNhatTieuDe("linh", "1250");
        capNhatThongKe(new UserCombatStats(1, 240, 118, 3, 7, 0, 4, null));
        java.util.ArrayList<UserShipPlacement> tau = new java.util.ArrayList<>();
        java.util.ArrayList<UserShotTarget> ban = new java.util.ArrayList<>();
        java.util.Random rd = new java.util.Random(7);
        for (int r = 0; r < SO_O; r++) {
            for (int c = 0; c < SO_O; c++) {
                String toaDo = "" + CHU_COT.charAt(r) + (c + 1);
                boolean gan = (r + c) % 3 != 0;
                int soTau = gan ? rd.nextInt(14) : rd.nextInt(3);
                if (soTau > 0) tau.add(new UserShipPlacement(0, 1, toaDo, soTau));
                int soBan = (r >= 3 && r <= 6 && c >= 3 && c <= 6) ? 4 + rd.nextInt(16) : rd.nextInt(6);
                if (soBan > 0) ban.add(new UserShotTarget(0, 1, toaDo, soBan, rd.nextInt(soBan + 1)));
            }
        }
        capNhatHeatmapDatTau(tau);
        capNhatHeatmapNhamBan(ban);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
