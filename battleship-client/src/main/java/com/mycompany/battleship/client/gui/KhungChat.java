package com.mycompany.battleship.client.gui;

import com.mycompany.battleship.common.model.MessageDTO;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Khung chat trong trận đấu, nổi ở góc dưới bên trái màn GameRoom.
 *  - Nút tròn icon chat: bấm để mở / thu gọn khung chat (thu gọn thì không che bàn cờ).
 *  - Khung đang thu gọn mà đối thủ nhắn tới: nút hiện chấm đỏ đếm số tin chưa đọc (giống Messenger).
 *  - Gửi:  lệnh CHAT, payload {text}.
 *  - Nhận: server gửi lại CHAT {senderId, text, timestamp} cho CẢ 2 người,
 *          SocketClientManager gọi gameRoomScreen.nhanTinNhanChat(...) -> themTinNhan(...) ở đây.
 *    (Tin của mình cũng chờ server gửi lại mới hiện, nên 2 bên luôn thấy cùng một nội dung.)
 */
public class KhungChat extends StackPane {

    private static final int DO_DAI_TOI_DA = 200;   // khớp chatMaxLength trong GameConfig của server
    private static final int SO_TIN_GIU_LAI = 100;   // giữ tối đa 100 dòng cho nhẹ giao diện
    private static final String[] CAU_NHANH = {"Chúc may mắn!", "Bắn hay đấy!", "Hụt rồi nhé!", "GG"};

    private final String tenCuaToi;
    private final String tenDoiThu;

    private VBox bangChat;
    private VBox danhSachTin;
    private ScrollPane cuonTin;
    private TextField oNhap;
    private Button nutMoChat;
    private Label lblSoTinMoi;

    private int soTinChuaDoc = 0;
    private boolean dangMo = false;

    public KhungChat(String tenCuaToi, String tenDoiThu) {
        this.tenCuaToi = tenCuaToi == null ? "" : tenCuaToi;
        this.tenDoiThu = tenDoiThu == null ? "Đối thủ" : tenDoiThu;

        getStyleClass().add("pl-chat-root");
        setPickOnBounds(false);                 // vùng trống quanh khung không chặn click xuống bàn cờ
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        bangChat = taoBangChat();
        bangChat.setVisible(false);
        bangChat.setManaged(false);

        StackPane nutCoSoDem = taoNutMoChat();

        VBox cot = new VBox(12, bangChat, nutCoSoDem);
        cot.setAlignment(Pos.BOTTOM_LEFT);
        cot.setFillWidth(false);                // nút tròn giữ đúng kích thước, không bị kéo rộng
        cot.setPickOnBounds(false);
        getChildren().add(cot);

        themDongHeThong("Kênh liên lạc đã mở. Hãy chào " + this.tenDoiThu + "!");
    }

    // ------------------------------------------------------------------ DỰNG GIAO DIỆN

    private StackPane taoNutMoChat() {
        nutMoChat = new Button();
        nutMoChat.setGraphic(taoIcon("pi-chat", 24));
        nutMoChat.getStyleClass().add("pl-chat-fab");
        nutMoChat.setOnAction(e -> {
            if (dangMo) dongChat();
            else moChat();
        });

        lblSoTinMoi = new Label("0");
        lblSoTinMoi.getStyleClass().add("pl-chat-badge");
        lblSoTinMoi.setMouseTransparent(true);  // bấm trúng chấm đỏ vẫn tính là bấm nút
        lblSoTinMoi.setVisible(false);
        lblSoTinMoi.setTranslateX(6);
        lblSoTinMoi.setTranslateY(-6);

        StackPane khung = new StackPane(nutMoChat, lblSoTinMoi);
        StackPane.setAlignment(lblSoTinMoi, Pos.TOP_RIGHT);
        khung.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        khung.setPickOnBounds(false);
        return khung;
    }

    private VBox taoBangChat() {
        // --- Đầu khung: icon + tiêu đề + tên đối thủ + nút thu gọn
        StackPane oIcon = new StackPane(taoIcon("pi-anchor", 16));
        oIcon.getStyleClass().add("pl-chat-header-icon");

        Label lblTieuDe = new Label("KÊNH LIÊN LẠC");
        lblTieuDe.getStyleClass().add("pl-chat-title");

        Region chamXanh = new Region();
        chamXanh.getStyleClass().add("pl-chat-dot");
        Label lblPhuDe = new Label("Đối thủ: " + tenDoiThu);
        lblPhuDe.getStyleClass().add("pl-chat-sub");
        HBox hangPhuDe = new HBox(6, chamXanh, lblPhuDe);
        hangPhuDe.setAlignment(Pos.CENTER_LEFT);

        VBox khoiChu = new VBox(2, lblTieuDe, hangPhuDe);

        Region khoangTrong = new Region();
        HBox.setHgrow(khoangTrong, Priority.ALWAYS);

        Button nutThuGon = new Button();
        nutThuGon.setGraphic(taoIcon("pi-minus", 14));
        nutThuGon.getStyleClass().add("pl-chat-icon-btn");
        nutThuGon.setOnAction(e -> dongChat());

        HBox dauKhung = new HBox(10, oIcon, khoiChu, khoangTrong, nutThuGon);
        dauKhung.setAlignment(Pos.CENTER_LEFT);
        dauKhung.getStyleClass().add("pl-chat-header");

        // --- Danh sách tin nhắn (tự cuộn xuống tin mới nhất)
        danhSachTin = new VBox(8);
        danhSachTin.setPadding(new Insets(12, 12, 12, 12));
        danhSachTin.getStyleClass().add("pl-chat-list");

        cuonTin = new ScrollPane(danhSachTin);
        cuonTin.setFitToWidth(true);
        cuonTin.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuonTin.setPrefHeight(250);
        cuonTin.getStyleClass().add("pl-chat-scroll");
        danhSachTin.heightProperty().addListener((obs, cu, moi) -> cuonTin.setVvalue(1.0));

        // --- Câu nhắn nhanh
        FlowPane hangCauNhanh = new FlowPane(6, 6);
        hangCauNhanh.setPadding(new Insets(8, 12, 0, 12));
        for (String cau : CAU_NHANH) {
            Button chip = new Button(cau);
            chip.getStyleClass().add("pl-chat-chip");
            chip.setOnAction(e -> guiTin(cau));
            hangCauNhanh.getChildren().add(chip);
        }

        // --- Ô nhập + nút gửi
        oNhap = new TextField();
        oNhap.setPromptText("Nhập tin nhắn...");
        oNhap.getStyleClass().add("pl-chat-input");
        oNhap.setOnAction(e -> guiTinTuONhap());          // Enter để gửi
        oNhap.textProperty().addListener((obs, cu, moi) -> {
            if (moi != null && moi.length() > DO_DAI_TOI_DA) {
                oNhap.setText(cu);                       // không cho gõ quá 200 ký tự
            }
        });
        HBox.setHgrow(oNhap, Priority.ALWAYS);

        Button nutGui = new Button();
        nutGui.setGraphic(taoIcon("pi-send", 16));
        nutGui.getStyleClass().add("pl-chat-send");
        nutGui.setOnAction(e -> guiTinTuONhap());

        HBox hangNhap = new HBox(8, oNhap, nutGui);
        hangNhap.setAlignment(Pos.CENTER);
        hangNhap.setPadding(new Insets(8, 12, 12, 12));

        VBox bang = new VBox(dauKhung, cuonTin, hangCauNhanh, hangNhap);
        bang.getStyleClass().add("pl-chat-panel");
        bang.setPrefWidth(320);
        bang.setMaxWidth(320);
        return bang;
    }

    // ------------------------------------------------------------------ MỞ / THU GỌN

    private void moChat() {
        dangMo = true;
        bangChat.setVisible(true);
        bangChat.setManaged(true);
        nutMoChat.getStyleClass().add("pl-chat-fab-open");

        // Đã mở xem thì xóa số tin chưa đọc
        soTinChuaDoc = 0;
        capNhatSoTinMoi();

        // Hiệu ứng: khung phóng nhẹ từ dưới lên + hiện dần
        bangChat.setOpacity(0);
        bangChat.setScaleX(0.92);
        bangChat.setScaleY(0.92);
        FadeTransition hienDan = new FadeTransition(Duration.millis(180), bangChat);
        hienDan.setToValue(1);
        ScaleTransition phongTo = new ScaleTransition(Duration.millis(180), bangChat);
        phongTo.setToX(1);
        phongTo.setToY(1);
        new ParallelTransition(hienDan, phongTo).play();

        cuonTin.setVvalue(1.0);
        oNhap.requestFocus();
    }

    private void dongChat() {
        dangMo = false;
        bangChat.setVisible(false);
        bangChat.setManaged(false);
        nutMoChat.getStyleClass().remove("pl-chat-fab-open");
        // Chuyển focus ra khỏi ô nhập để phím R (xoay tàu) hoạt động lại bình thường
        nutMoChat.requestFocus();
    }

    // ------------------------------------------------------------------ GỬI / NHẬN TIN

    private void guiTinTuONhap() {
        guiTin(oNhap.getText());
        oNhap.clear();
    }

    private void guiTin(String noiDung) {
        if (noiDung == null) return;
        String tin = noiDung.trim();
        if (tin.isEmpty()) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", tin);
        SocketClientManager.guiTinNhan(new MessageDTO("CHAT", payload));
    }

    /** Thêm 1 tin (của mình hoặc của đối thủ) vào khung. Gọi trên luồng giao diện (Platform.runLater). */
    public void themTinNhan(String nguoiGui, String noiDung, long thoiGian) {
        boolean cuaToi = nguoiGui != null && nguoiGui.equalsIgnoreCase(tenCuaToi);

        Label bongChat = new Label(noiDung);
        bongChat.setWrapText(true);
        bongChat.setMaxWidth(220);
        bongChat.getStyleClass().addAll("pl-chat-bubble", cuaToi ? "pl-chat-bubble-me" : "pl-chat-bubble-them");

        long moc = thoiGian > 0 ? thoiGian : System.currentTimeMillis();
        Label lblGio = new Label(new SimpleDateFormat("HH:mm").format(new Date(moc)));
        lblGio.getStyleClass().add("pl-chat-time");

        VBox khoi = new VBox(3);
        if (!cuaToi) {
            Label lblTen = new Label(tenDoiThu);
            lblTen.getStyleClass().add("pl-chat-name");
            khoi.getChildren().add(lblTen);
        }
        khoi.getChildren().addAll(bongChat, lblGio);
        khoi.setAlignment(cuaToi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox dong = new HBox(khoi);
        dong.setAlignment(cuaToi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        themDong(dong, cuaToi ? 16 : -16);

        // Khung đang thu gọn mà đối thủ nhắn -> tăng số tin chưa đọc + rung nút
        if (!cuaToi && !dangMo) {
            soTinChuaDoc++;
            capNhatSoTinMoi();
            rungNut();
        }
    }

    /** Dòng thông báo của hệ thống (vd: gửi quá nhanh, tin rỗng...). */
    public void themDongHeThong(String noiDung) {
        Label lbl = new Label(noiDung);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("pl-chat-system");
        HBox dong = new HBox(lbl);
        dong.setAlignment(Pos.CENTER);
        themDong(dong, 0);
    }

    private void themDong(Node dong, double lechNgang) {
        danhSachTin.getChildren().add(dong);
        if (danhSachTin.getChildren().size() > SO_TIN_GIU_LAI) {
            danhSachTin.getChildren().remove(0);
        }

        // Hiệu ứng tin mới trượt nhẹ vào
        dong.setOpacity(0);
        dong.setTranslateX(lechNgang);
        FadeTransition hienDan = new FadeTransition(Duration.millis(200), dong);
        hienDan.setToValue(1);
        TranslateTransition truot = new TranslateTransition(Duration.millis(200), dong);
        truot.setToX(0);
        new ParallelTransition(hienDan, truot).play();
    }

    // ------------------------------------------------------------------ SỐ TIN CHƯA ĐỌC

    private void capNhatSoTinMoi() {
        if (soTinChuaDoc <= 0) {
            lblSoTinMoi.setVisible(false);
            return;
        }
        lblSoTinMoi.setText(soTinChuaDoc > 9 ? "9+" : String.valueOf(soTinChuaDoc));
        lblSoTinMoi.setVisible(true);

        // chấm đỏ "nảy" lên mỗi khi có tin mới
        lblSoTinMoi.setScaleX(0.4);
        lblSoTinMoi.setScaleY(0.4);
        ScaleTransition nay = new ScaleTransition(Duration.millis(220), lblSoTinMoi);
        nay.setToX(1);
        nay.setToY(1);
        nay.play();
    }

    private void rungNut() {
        ScaleTransition rung = new ScaleTransition(Duration.millis(140), nutMoChat);
        rung.setFromX(1);
        rung.setFromY(1);
        rung.setToX(1.15);
        rung.setToY(1.15);
        rung.setAutoReverse(true);
        rung.setCycleCount(4);
        rung.play();
    }

    /** Icon vẽ bằng đường SVG (hình dạng nằm trong player.css). */
    private Region taoIcon(String loai, double co) {
        Region icon = new Region();
        icon.getStyleClass().addAll("pl-icon", loai);
        icon.setMinSize(co, co);
        icon.setPrefSize(co, co);
        icon.setMaxSize(co, co);
        return icon;
    }
}
