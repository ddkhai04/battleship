package com.mycompany.battleship.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.*;

public class ReplayGUI extends Stage {

    public static final String SHIP_SIZE_2 = "ship-size-2";
    public static final String SHIP_SIZE_3_A = "ship-size-3-a";
    public static final String SHIP_SIZE_3_B = "ship-size-3-b";
    public static final String SHIP_SIZE_4 = "ship-size-4";
    public static final String SHIP_SIZE_5 = "ship-size-5";

    private static final int GRID_SIZE = 10;
    private static final double CELL_SIZE = 36;
    
    private String winnerId = "";
    private String matchStatus = "FINISHED";
    private boolean daHienThongBaoKetThuc = false;
    // Cấu trúc cố định hình dạng tàu ban đầu, chống biến dạng khi bắn
    private static class ShipInfo {
        String shipType;
        boolean isHorizontal;
        List<int[]> cells = new ArrayList<>();
        List<String> shapeClasses = new ArrayList<>();
    }

    private int currentTurnIdx = 0;
    private JsonArray turns = new JsonArray();
    private JsonObject boards = new JsonObject();

    private String p1Id = "";
    private String p2Id = "";
    private String p1Name = "Người chơi 1";
    private String p2Name = "Người chơi 2";

    private Label lblP1Name;
    private Label lblP2Name;
    private Label lblP1Initial;
    private Label lblP2Initial;
    private Label lblHeaderP1;
    private Label lblHeaderP2;

    // Ma trận vị trí tàu gốc ban đầu
    private final char[][] baseGridP1 = new char[GRID_SIZE][GRID_SIZE];
    private final char[][] baseGridP2 = new char[GRID_SIZE][GRID_SIZE];

    // Danh sách 5 con tàu cố định của mỗi người chơi
    private List<ShipInfo> p1Ships = new ArrayList<>();
    private List<ShipInfo> p2Ships = new ArrayList<>();

    private final StackPane[][] p1Cells = new StackPane[GRID_SIZE][GRID_SIZE];
    private final StackPane[][] p2Cells = new StackPane[GRID_SIZE][GRID_SIZE];

    private Label lblTurnCounter;
    private Label lblTurnDescription;
    private Slider turnSlider;
    private Button btnPlayPause;
    private Timeline autoPlayTimer;

    public ReplayGUI(JsonObject replayData) {
        String matchId = replayData.has("matchId") ? replayData.get("matchId").getAsString() : "0";
        if (matchId.endsWith(".0")) matchId = matchId.substring(0, matchId.length() - 2);
        setTitle("Battleship - Xem lại trận đấu #" + matchId);

        if (replayData.has("turns") && !replayData.get("turns").isJsonNull()) {
            this.turns = replayData.getAsJsonArray("turns");
        }
        if (replayData.has("boards") && !replayData.get("boards").isJsonNull()) {
            this.boards = replayData.getAsJsonObject("boards");
        }

        // 1. Lấy thông tin người chơi & nạp bàn cờ gốc
        phanGiaiNguoiChoi(replayData);
        khoiTaoTauBanDau();

        // 2. Cố định vị trí và kiểu dáng 5 con tàu của 2 bên
        p1Ships = trichXuatDanhSachTau(baseGridP1);
        p2Ships = trichXuatDanhSachTau(baseGridP2);

        // 3. Khởi tạo giao diện
        BorderPane gameLayout = new BorderPane();
        gameLayout.getStyleClass().add("game-root");
        gameLayout.setTop(createTopBar(matchId));
        gameLayout.setCenter(createBoardsArea());
        gameLayout.setBottom(createControlBar());

        ImageView bgImageView = new ImageView();
        try {
            URL bgUrl = getClass().getResource("/ocean-bg.gif");
            if (bgUrl == null) bgUrl = getClass().getResource("/bg.png");
            if (bgUrl != null) {
                bgImageView.setImage(new Image(bgUrl.toExternalForm()));
                bgImageView.fitWidthProperty().bind(widthProperty());
                bgImageView.fitHeightProperty().bind(heightProperty());
            }
        } catch (Exception ignored) {}

        StackPane rootStack = new StackPane(bgImageView, gameLayout);
        rootStack.getStyleClass().add("app-background");

        Scene scene = new Scene(rootStack, 1000, 700);
        scene.getStylesheets().add(resolveCssPath());

        setScene(scene);
        setResizable(false);

        // Hiển thị trạng thái khởi đầu (Lượt 0)
        hienThiLuot(0);

        setOnCloseRequest(e -> dungVaDong());
    }

    private String resolveCssPath() {
        URL cssUrl = getClass().getResource("/game-style.css");
        if (cssUrl != null) return cssUrl.toExternalForm();
        return new File("game-style.css").toURI().toString();
    }

    private void dungVaDong() {
        if (autoPlayTimer != null) autoPlayTimer.stop();
        close();
    }

    private void phanGiaiNguoiChoi(JsonObject data) {
        if (data.has("matchInfo") && !data.get("matchInfo").isJsonNull()) {
            JsonObject info = data.getAsJsonObject("matchInfo");
            if (info.has("player1Id")) p1Id = String.valueOf((int) info.get("player1Id").getAsDouble());
            if (info.has("player2Id")) p2Id = String.valueOf((int) info.get("player2Id").getAsDouble());
            if (info.has("player1Name")) p1Name = info.get("player1Name").getAsString();
            if (info.has("player2Name")) p2Name = info.get("player2Name").getAsString();
            
            if (info.has("winnerId") && !info.get("winnerId").isJsonNull()) {
                winnerId = String.valueOf((int) info.get("winnerId").getAsDouble());
            }
            if (info.has("status") && !info.get("status").isJsonNull()) {
                matchStatus = info.get("status").getAsString();
            }
        }
        
        if (p1Id.isEmpty() && turns.size() > 0) {
            JsonObject t0 = turns.get(0).getAsJsonObject();
            p1Id = String.valueOf(t0.get("playerId").getAsInt());
            p1Name = "Player " + p1Id;
            for (JsonElement el : turns) {
                String pid = String.valueOf(el.getAsJsonObject().get("playerId").getAsInt());
                if (!pid.equals(p1Id)) {
                    p2Id = pid;
                    p2Name = "Player " + p2Id;
                    break;
                }
            }
        }

        if ((p1Id.isEmpty() || p2Id.isEmpty()) && boards.size() > 0) {
            List<String> keys = new ArrayList<>(boards.keySet());
            if (keys.size() > 0 && p1Id.isEmpty()) p1Id = keys.get(0);
            if (keys.size() > 1 && p2Id.isEmpty()) p2Id = keys.get(1);
        }
        
    }

    private void khoiTaoTauBanDau() {
        for (int r = 0; r < GRID_SIZE; r++) {
            Arrays.fill(baseGridP1[r], '.');
            Arrays.fill(baseGridP2[r], '.');
        }

        if (boards.has(p1Id)) {
            napTauVaoMaTran(baseGridP1, boards.get(p1Id), true);
        }
        if (boards.has(p2Id)) {
            napTauVaoMaTran(baseGridP2, boards.get(p2Id), false);
        }
    }

    private void napTauVaoMaTran(char[][] grid, JsonElement boardElement, boolean isP1) {
        if (boardElement == null || boardElement.isJsonNull()) return;
        try {
            JsonElement parsed = boardElement;
            if (boardElement.isJsonPrimitive()) {
                parsed = JsonParser.parseString(boardElement.getAsString());
            }

            if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                if (obj.has("myId") && !obj.get("myId").isJsonNull()) {
                    String myName = obj.get("myId").getAsString();
                    if (isP1) p1Name = myName;
                    else p2Name = myName;
                }

                if (obj.has("ownGrid")) {
                    JsonArray ownGridArr = obj.getAsJsonArray("ownGrid");
                    for (int r = 0; r < GRID_SIZE && r < ownGridArr.size(); r++) {
                        JsonArray rowArr = ownGridArr.get(r).getAsJsonArray();
                        for (int c = 0; c < GRID_SIZE && c < rowArr.size(); c++) {
                            String val = rowArr.get(c).getAsString();
                            char ch = (!val.isEmpty()) ? val.charAt(0) : '.';
                            if (ch == 'S' || ch == 'X') {
                                grid[r][c] = 'S';
                            }
                        }
                    }
                    return;
                }
                if (obj.has("ships")) parsed = obj.get("ships");
            }

            if (parsed != null && parsed.isJsonArray()) {
                JsonArray shipsArr = parsed.getAsJsonArray();
                for (JsonElement sEl : shipsArr) {
                    JsonObject sObj = sEl.getAsJsonObject();
                    if (sObj.has("cells")) {
                        for (JsonElement cEl : sObj.getAsJsonArray("cells")) {
                            JsonObject c = cEl.getAsJsonObject();
                            int r = c.get("row").getAsInt();
                            int col = c.get("col").getAsInt();
                            if (r >= 0 && r < GRID_SIZE && col >= 0 && col < GRID_SIZE) {
                                grid[r][col] = 'S';
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Trích xuất và định hình cố định 5 con tàu ban đầu
     */
    private List<ShipInfo> trichXuatDanhSachTau(char[][] baseGrid) {
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
        List<List<int[]>> rawShips = new ArrayList<>();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (baseGrid[r][c] == 'S' && !visited[r][c]) {
                    List<int[]> comp = new ArrayList<>();
                    Queue<int[]> queue = new LinkedList<>();
                    queue.add(new int[]{r, c});
                    visited[r][c] = true;

                    while (!queue.isEmpty()) {
                        int[] cur = queue.poll();
                        comp.add(cur);
                        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                        for (int[] d : dirs) {
                            int nr = cur[0] + d[0];
                            int nc = cur[1] + d[1];
                            if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                                if (baseGrid[nr][nc] == 'S' && !visited[nr][nc]) {
                                    visited[nr][nc] = true;
                                    queue.add(new int[]{nr, nc});
                                }
                            }
                        }
                    }
                    rawShips.add(comp);
                }
            }
        }

        rawShips.sort((a, b) -> Integer.compare(b.size(), a.size()));

        List<ShipInfo> result = new ArrayList<>();
        boolean hasAssigned3A = false;

        for (List<int[]> comp : rawShips) {
            int len = comp.size();
            String sType;
            if (len == 5) sType = SHIP_SIZE_5;
            else if (len == 4) sType = SHIP_SIZE_4;
            else if (len == 3) {
                if (!hasAssigned3A) {
                    sType = SHIP_SIZE_3_A;
                    hasAssigned3A = true;
                } else {
                    sType = SHIP_SIZE_3_B;
                }
            } else if (len == 2) {
                sType = SHIP_SIZE_2;
            } else {
                sType = SHIP_SIZE_3_A;
            }

            comp.sort((a, b) -> (a[0] == b[0]) ? Integer.compare(a[1], b[1]) : Integer.compare(a[0], b[0]));
            boolean isHoriz = (len > 1 && comp.get(0)[0] == comp.get(1)[0]);

            ShipInfo info = new ShipInfo();
            info.shipType = sType;
            info.isHorizontal = isHoriz;
            info.cells = comp;

            for (int i = 0; i < len; i++) {
                if (len == 1) {
                    info.shapeClasses.add(null);
                } else if (i == 0) {
                    info.shapeClasses.add(isHoriz ? "ship-horizontal-head" : "ship-vertical-head");
                } else if (i == len - 1) {
                    info.shapeClasses.add(isHoriz ? "ship-horizontal-tail" : "ship-vertical-tail");
                } else {
                    info.shapeClasses.add(isHoriz ? "ship-horizontal-body" : "ship-vertical-body");
                }
            }
            result.add(info);
        }
        return result;
    }

    private HBox createTopBar(String matchId) {
        HBox top = new HBox();
        top.getStyleClass().add("top-bar");
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(18, 28, 12, 28));

        HBox player1Box = createPlayerBox(p1Name, "avatar-color-1", true);
        player1Box.setPrefWidth(280);
        player1Box.setAlignment(Pos.CENTER_LEFT);

        HBox player2Box = createPlayerBox(p2Name, "avatar-color-2", false);

        Button exitBtn = new Button("Thoát ✖");
        exitBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> dungVaDong());

        HBox rightGroup = new HBox(20, player2Box, exitBtn);
        rightGroup.setPrefWidth(280);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        Label lblTitle = new Label("REPLAY TRẬN #" + matchId);
        lblTitle.getStyleClass().add("player-name");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #00E5FF;");

        lblTurnDescription = new Label("Bắt đầu trận đấu - Sẵn sàng");
        lblTurnDescription.getStyleClass().addAll("turn-indicator", "turn-indicator-active");
        lblTurnDescription.setStyle("-fx-font-size: 13px;");

        VBox centerBox = new VBox(4, lblTitle, lblTurnDescription);
        centerBox.setAlignment(Pos.CENTER);

        Region spacerLeft = new Region();
        Region spacerRight = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        top.getChildren().addAll(player1Box, spacerLeft, centerBox, spacerRight, rightGroup);
        return top;
    }

    private HBox createPlayerBox(String name, String avatarColorClass, boolean isLeft) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);

        Circle avatarBg = new Circle(18);
        avatarBg.getStyleClass().addAll("player-avatar", avatarColorClass);
        String initialLetter = (!name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
        Label initial = new Label(initialLetter);
        initial.getStyleClass().add("player-avatar-initial");
        StackPane avatar = new StackPane(avatarBg, initial);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("player-name");

        if (isLeft) {
            lblP1Name = nameLabel;
            lblP1Initial = initial;
        } else {
            lblP2Name = nameLabel;
            lblP2Initial = initial;
        }

        Label badge = new Label("REPLAY");
        badge.getStyleClass().add("score-badge");

        VBox textBox = new VBox(2, nameLabel, badge);
        textBox.setAlignment(isLeft ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        if (isLeft) box.getChildren().addAll(textBox, avatar);
        else box.getChildren().addAll(avatar, textBox);
        return box;
    }

    private HBox createBoardsArea() {
        HBox boardsBox = new HBox(28);
        boardsBox.getStyleClass().add("boards-area");
        boardsBox.setAlignment(Pos.CENTER);
        boardsBox.setPadding(new Insets(10, 24, 10, 24));

        boardsBox.getChildren().addAll(
                createBoardPanel("Hạm đội của " + p1Name, p1Cells, true),
                createBoardPanel("Hạm đội của " + p2Name, p2Cells, false)
        );
        return boardsBox;
    }

    private VBox createBoardPanel(String title, StackPane[][] cells, boolean isP1) {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");

        Label header = new Label(title);
        header.getStyleClass().addAll("board-header", "board-header-fleet");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);

        if (isP1) lblHeaderP1 = header;
        else lblHeaderP2 = header;

        GridPane grid = new GridPane();
        grid.getStyleClass().add("grid-board");
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                StackPane cell = new StackPane();
                cell.getStyleClass().addAll("grid-cell", "water-cell");
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);
                cell.getChildren().add(createWaterDot());

                cells[r][c] = cell;
                grid.add(cell, c, r);
            }
        }

        panel.getChildren().addAll(header, grid);
        return panel;
    }

    private VBox createControlBar() {
        VBox bar = new VBox(10);
        bar.getStyleClass().add("inventory-bar");
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 20, 16, 20));

        int maxTurns = turns.size();
        turnSlider = new Slider(0, Math.max(1, maxTurns), 0);
        turnSlider.setMajorTickUnit(Math.max(1, maxTurns / 10));
        turnSlider.setBlockIncrement(1);
        turnSlider.setMaxWidth(680);
        turnSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int targetTurn = newVal.intValue();
            if (targetTurn != currentTurnIdx) {
                hienThiLuot(targetTurn);
            }
        });

        lblTurnCounter = new Label("Lượt: 0 / " + maxTurns);
        lblTurnCounter.setStyle("-fx-text-fill: #E0E6ED; -fx-font-weight: bold; -fx-font-size: 13px;");

        Button btnFirst = taoNutDieuKhien("⏮️ Đầu trận", e -> hienThiLuot(0));
        Button btnPrev = taoNutDieuKhien("◀️ Lùi 1 lượt", e -> {
            if (currentTurnIdx > 0) hienThiLuot(currentTurnIdx - 1);
        });

        btnPlayPause = taoNutDieuKhien("▶️ Tự động phát", e -> batTatTuDongPhat());
        btnPlayPause.setStyle("-fx-background-color: #00ACC1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");

        Button btnNext = taoNutDieuKhien("Tiến 1 lượt ▶️", e -> {
            if (currentTurnIdx < turns.size()) hienThiLuot(currentTurnIdx + 1);
        });
        Button btnLast = taoNutDieuKhien("Cuối trận ⏭️", e -> hienThiLuot(turns.size()));

        HBox btnBox = new HBox(12, btnFirst, btnPrev, btnPlayPause, btnNext, btnLast);
        btnBox.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(lblTurnCounter, turnSlider, btnBox);
        return bar;
    }

    private Button taoNutDieuKhien(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #1E3A5F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        btn.setOnAction(handler);
        return btn;
    }

    private void batTatTuDongPhat() {
        if (autoPlayTimer != null && autoPlayTimer.getStatus() == Timeline.Status.RUNNING) {
            autoPlayTimer.stop();
            btnPlayPause.setText("▶️ Tự động phát");
            btnPlayPause.setStyle("-fx-background-color: #00ACC1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        } else {
            if (currentTurnIdx >= turns.size()) hienThiLuot(0);
            autoPlayTimer = new Timeline(new KeyFrame(Duration.millis(800), e -> {
                if (currentTurnIdx < turns.size()) {
                    hienThiLuot(currentTurnIdx + 1);
                } else {
                    autoPlayTimer.stop();
                    btnPlayPause.setText("▶️ Tự động phát");
                    btnPlayPause.setStyle("-fx-background-color: #00ACC1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
                }
            }));
            autoPlayTimer.setCycleCount(Timeline.INDEFINITE);
            autoPlayTimer.play();
            btnPlayPause.setText("⏸️ Tạm dừng");
            btnPlayPause.setStyle("-fx-background-color: #E65100; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        }
    }

    /**
     * Hàm phân tích toạ độ chuẩn hóa: Trừ 1 cho chỉ số cột (1-indexed -> 0-indexed)
     */
    private int[] parseCoordinate(String coord) {
        if (coord == null || coord.trim().isEmpty()) return null;
        coord = coord.trim().toUpperCase();

        // 1. Trường hợp dạng "r,c" (ví dụ: "4,1")
        if (coord.contains(",")) {
            try {
                String clean = coord.replace("(", "").replace(")", "").replace("[", "").replace("]", "");
                String[] parts = clean.split(",");
                int r = Integer.parseInt(parts[0].trim());
                int c = Integer.parseInt(parts[1].trim());
                if (r >= 0 && r < 10 && c >= 0 && c < 10) return new int[]{r, c};
            } catch (Exception ignored) {}
        }

        // 2. Trường hợp dạng Chữ + Số: "E2", "B4", "A1", "J10"
        char firstChar = coord.charAt(0);
        if (firstChar >= 'A' && firstChar <= 'Z') {
            try {
                int r = firstChar - 'A';
                int rawCol = Integer.parseInt(coord.substring(1).trim());
                // CỘT TỪ 1..10 PHẢI TRỪ ĐI 1 ĐỂ THÀNH CHỈ SỐ MẢNG 0..9
                int c = (rawCol >= 1 && rawCol <= 10) ? (rawCol - 1) : rawCol;
                if (r >= 0 && r < 10 && c >= 0 && c < 10) return new int[]{r, c};
            } catch (Exception ignored) {}
        }

        return null;
    }

    private char[][] cloneGrid(char[][] src) {
        char[][] dst = new char[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(src[i], 0, dst[i], 0, GRID_SIZE);
        }
        return dst;
    }

    /**
     * Tái tạo chính xác nước đi tại lượt targetStep
     */
private String tenLoaiDan(String missileType) {
        if (missileType == null) return "";
        switch (missileType.toUpperCase()) {
            case "BIG": return " [Đạn Chữ Thập ✚]";
            case "NUCLEAR": return " [Đạn Nguyên Tử ☢️]";
            case "RAIN": return " [Đạn Rải Rác 🌧️]";
            default: return "";
        }
    }

    /**
     * Lấy danh sách độ lệch (dRow, dCol) theo loại đạn (dùng khi cần tự bung vùng nổ từ tâm)
     */
    private List<int[]> layHinhDangDan(String missileType) {
        List<int[]> offsets = new ArrayList<>();
        if ("BIG".equalsIgnoreCase(missileType)) {
            offsets.add(new int[]{0, 0});
            offsets.add(new int[]{-1, 0});
            offsets.add(new int[]{1, 0});
            offsets.add(new int[]{0, -1});
            offsets.add(new int[]{0, 1});
        } else if ("NUCLEAR".equalsIgnoreCase(missileType)) {
            for (int dr = -2; dr <= 2; dr++) {
                for (int dc = -2; dc <= 2; dc++) {
                    if (Math.abs(dr) + Math.abs(dc) <= 2) {
                        offsets.add(new int[]{dr, dc});
                    }
                }
            }
        } else {
            offsets.add(new int[]{0, 0});
        }
        return offsets;
    }

    /**
     * Tái tạo chính xác nước đi tại lượt targetStep (Hiện đầy đủ mọi ô trúng và trượt trong 1 lượt)
     */
    private void hienThiLuot(int targetStep) {
        this.currentTurnIdx = targetStep;
        if (turnSlider != null) turnSlider.setValue(targetStep);
        if (lblTurnCounter != null) lblTurnCounter.setText("Lượt: " + targetStep + " / " + turns.size());

        char[][] curGridP1 = cloneGrid(baseGridP1);
        char[][] curGridP2 = cloneGrid(baseGridP2);

        String lastAction = "Bắt đầu trận đấu - Sẵn sàng";

        for (int i = 0; i < targetStep; i++) {
            JsonObject t = turns.get(i).getAsJsonObject();
            int shooterId = t.get("playerId").getAsInt();
            String coord = t.has("coordinate") && !t.get("coordinate").isJsonNull() ? t.get("coordinate").getAsString() : "";
            String res = t.has("result") && !t.get("result").isJsonNull() ? t.get("result").getAsString() : "MISS";
            String missileType = t.has("missileType") && !t.get("missileType").isJsonNull()
                    ? t.get("missileType").getAsString()
                    : (t.has("missile_type") && !t.get("missile_type").isJsonNull() ? t.get("missile_type").getAsString() : "SIMPLE");

            boolean isP1Shooting = String.valueOf(shooterId).equals(String.valueOf(p1Id));
            char[][] targetGrid = isP1Shooting ? curGridP2 : curGridP1;
            char[][] targetBaseGrid = isP1Shooting ? baseGridP2 : baseGridP1;

            // Đọc mảng affectedCells từ JSON
            JsonArray affectedArr = null;
            String affectedKey = t.has("affectedCells") ? "affectedCells" : (t.has("affected_cells") ? "affected_cells" : null);
            if (affectedKey != null && !t.get(affectedKey).isJsonNull()) {
                try {
                    JsonElement el = t.get(affectedKey);
                    if (el.isJsonArray()) {
                        affectedArr = el.getAsJsonArray();
                    } else if (el.isJsonPrimitive()) {
                        String rawJson = el.getAsString();
                        if (rawJson != null && !rawJson.trim().isEmpty()) {
                            affectedArr = JsonParser.parseString(rawJson).getAsJsonArray();
                        }
                    }
                } catch (Exception ignored) {}
            }

            int hitCount = 0;
            int totalCells = 0;
            boolean anySunk = "SUNK".equalsIgnoreCase(res);

            if (affectedArr != null && affectedArr.size() > 0) {
                // TRƯỜNG HỢP 1: Có danh sách chi tiết các ô bị bắn từ DB -> Vẽ từng ô (Trúng = 'X', Trượt = 'o')
                totalCells = affectedArr.size();
                for (JsonElement cellEl : affectedArr) {
                    JsonObject cellObj = cellEl.getAsJsonObject();
                    int r = -1, c = -1;
                    if (cellObj.has("row") && cellObj.has("col")) {
                        r = cellObj.get("row").getAsInt();
                        c = cellObj.get("col").getAsInt();
                    } else if (cellObj.has("coord")) {
                        int[] rc = parseCoordinate(cellObj.get("coord").getAsString());
                        if (rc != null) { r = rc[0]; c = rc[1]; }
                    }

                    if (r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE) {
                        String cellRes = cellObj.has("result") ? cellObj.get("result").getAsString() : "MISS";
                        boolean cellHit = "HIT".equalsIgnoreCase(cellRes)
                                || "SUNK".equalsIgnoreCase(cellRes)
                                || targetBaseGrid[r][c] == 'S';
                        if ("SUNK".equalsIgnoreCase(cellRes)) anySunk = true;
                        if (cellHit) hitCount++;
                        // Ô có tàu thì đánh dấu 'X' (trúng), ô nước thì đánh dấu 'o' (trượt)
                        targetGrid[r][c] = cellHit ? 'X' : 'o';
                    }
                }
            } else {
                // TRƯỜNG HỢP 2: Dữ liệu cũ chưa có affectedCells -> Tự bung vùng nổ theo loại đạn từ tâm
                int[] center = parseCoordinate(coord);
                if (center != null) {
                    for (int[] d : layHinhDangDan(missileType)) {
                        int r = center[0] + d[0];
                        int c = center[1] + d[1];
                        if (r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE) {
                            totalCells++;
                            boolean cellHit = (targetBaseGrid[r][c] == 'S');
                            if (d[0] == 0 && d[1] == 0 && ("HIT".equalsIgnoreCase(res) || "SUNK".equalsIgnoreCase(res))) {
                                cellHit = true;
                            }
                            if (cellHit) hitCount++;
                            targetGrid[r][c] = cellHit ? 'X' : 'o';
                        }
                    }
                }
            }

            if (i == targetStep - 1) {
                String shooterName = isP1Shooting ? p1Name : p2Name;
                String targetName = isP1Shooting ? p2Name : p1Name;
                String weaponTag = tenLoaiDan(missileType);
                String resStr;
                if (totalCells > 1) {
                    resStr = anySunk
                            ? String.format("ĐÁNH CHÌM TÀU! 💀 (Trúng %d/%d ô)", hitCount, totalCells)
                            : (hitCount > 0 ? String.format("TRÚNG %d/%d Ô! 🔥", hitCount, totalCells) : String.format("TRƯỢT CẢ %d Ô ⚪", totalCells));
                } else {
                    boolean isHit = hitCount > 0 || "HIT".equalsIgnoreCase(res) || "SUNK".equalsIgnoreCase(res);
                    resStr = isHit ? (anySunk ? "ĐÁNH CHÌM TÀU! 💀" : "TRÚNG ĐÍCH! 🔥") : "BẮN TRƯỢT ⚪";
                }
                lastAction = String.format("Lượt %d: %s bắn %s (%s)%s ➔ %s", (i + 1), shooterName, targetName, coord, weaponTag, resStr);
            }
        }

        if (lblTurnDescription != null) lblTurnDescription.setText(lastAction);

        // Vẽ bàn cờ chuẩn xác theo danh sách 5 con tàu cố định
        renderFleetBoard(p1Cells, p1Ships, curGridP1);
        renderFleetBoard(p2Cells, p2Ships, curGridP2);
        if (targetStep < turns.size()) {
            daHienThongBaoKetThuc = false;
        } else if (targetStep == turns.size() && !daHienThongBaoKetThuc) {
            daHienThongBaoKetThuc = true;
            if (autoPlayTimer != null && autoPlayTimer.getStatus() == Timeline.Status.RUNNING) {
                autoPlayTimer.stop();
                btnPlayPause.setText("▶️ Tự động phát");
                btnPlayPause.setStyle("-fx-background-color: #00ACC1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
            }

            // Kiểm tra xem có đội nào bị bắn chìm toàn bộ 5 tàu không
            boolean p1AllSunk = !p1Ships.isEmpty() && p1Ships.stream().allMatch(s -> s.cells.stream().allMatch(p -> curGridP1[p[0]][p[1]] == 'X'));
            boolean p2AllSunk = !p2Ships.isEmpty() && p2Ships.stream().allMatch(s -> s.cells.stream().allMatch(p -> curGridP2[p[0]][p[1]] == 'X'));

            String tenNguoiThang;
            String tenNguoiThua;
            if (p2AllSunk || winnerId.equals(p1Id)) {
                tenNguoiThang = p1Name;
                tenNguoiThua = p2Name;
            } else if (p1AllSunk || winnerId.equals(p2Id)) {
                tenNguoiThang = p2Name;
                tenNguoiThua = p1Name;
            } else if (turns.size() > 0) {
                int lastShooter = turns.get(turns.size() - 1).getAsJsonObject().get("playerId").getAsInt();
                boolean lastWasP1 = String.valueOf(lastShooter).equals(p1Id);
                tenNguoiThang = lastWasP1 ? p1Name : p2Name;
                tenNguoiThua = lastWasP1 ? p2Name : p1Name;
            } else {
                tenNguoiThang = p1Name;
                tenNguoiThua = p2Name;
            }

            String lyDoThua;
            if (p1AllSunk || p2AllSunk) {
                lyDoThua = "Toàn bộ hạm đội của " + tenNguoiThua + " đã bị đánh chìm!";
            } else if ("ABORTED".equalsIgnoreCase(matchStatus)) {
                lyDoThua = "Người chơi " + tenNguoiThua + " đã đầu hàng / rời khỏi trận đấu!";
            } else {
                lyDoThua = "Người chơi " + tenNguoiThua + " đã hết thời gian suy nghĩ (Timeout) hoặc bỏ cuộc!";
            }

            lblTurnDescription.setText("🏁 Kết thúc: " + tenNguoiThang + " thắng (" + tenNguoiThua + " thua)");

            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Kết thúc trận đấu");
                alert.setHeaderText("🏆 Chiến thắng: " + tenNguoiThang);
                alert.setContentText("❌ Thất bại: " + tenNguoiThua + "\n📌 Lý do: " + lyDoThua);
                DialogStyle.apply(alert);
                alert.show();
            });
        }
    }

    private boolean baseGridHasShip(boolean isP1Shooting, int r, int c) {
        char[][] base = isP1Shooting ? baseGridP2 : baseGridP1;
        return base[r][c] == 'S';
    }

    private void renderFleetBoard(StackPane[][] cells, List<ShipInfo> shipsList, char[][] currentGrid) {
        // 1. Vẽ ô nước và các ô bắn trượt
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                StackPane cell = cells[r][c];
                clearCellStateClasses(cell);
                cell.getChildren().clear();

                if (currentGrid[r][c] == 'o') {
                    cell.getStyleClass().add("water-cell");
                    cell.getChildren().add(createMissDot());
                } else {
                    cell.getStyleClass().add("water-cell");
                    cell.getChildren().add(createWaterDot());
                }
            }
        }
        for (ShipInfo ship : shipsList) {
            boolean allSunk = true;
            for (int[] p : ship.cells) {
                if (currentGrid[p[0]][p[1]] != 'X') {
                    allSunk = false;
                    break;
                }
            }

            for (int i = 0; i < ship.cells.size(); i++) {
                int[] p = ship.cells.get(i);
                int r = p[0];
                int c = p[1];
                String shapeClass = ship.shapeClasses.get(i);

                String state;
                if (currentGrid[r][c] == 'X') {
                    state = allSunk ? "sunk" : "hit";
                } else {
                    state = "ship";
                }
                setFleetCellState(cells, r, c, state, ship.shipType, shapeClass);
            }
        }
    }

    private void setFleetCellState(StackPane[][] cells, int row, int col, String state, String shipType, String shapeClass) {
        StackPane cell = cells[row][col];
        clearCellStateClasses(cell);
        cell.getChildren().clear();

        cell.getStyleClass().addAll("cell-ship", shipType != null ? shipType : SHIP_SIZE_3_A);
        if (shapeClass != null) {
            cell.getStyleClass().add(shapeClass);
        } else {
            cell.setStyle("-fx-background-radius: 6;");
        }

        if ("hit".equals(state)) {
            cell.getChildren().add(createHitMark());
        } else if ("sunk".equals(state)) {
            cell.getChildren().add(createSunkMark());
        }
    }

    private void clearCellStateClasses(StackPane cell) {
        cell.setStyle("");
        cell.getStyleClass().removeAll("water-cell", "cell-ship", "cell-hit",
                SHIP_SIZE_2, SHIP_SIZE_3_A, SHIP_SIZE_3_B, SHIP_SIZE_4, SHIP_SIZE_5,
                "ship-horizontal-head", "ship-horizontal-tail", "ship-horizontal-body",
                "ship-vertical-head", "ship-vertical-tail", "ship-vertical-body");
    }

    private Circle createWaterDot() {
        Circle dot = new Circle(3);
        dot.getStyleClass().add("water-dot");
        return dot;
    }

    private Circle createMissDot() {
        Circle dot = new Circle(7);
        dot.getStyleClass().add("miss-dot");
        return dot;
    }

    private ImageView createHitMark() {
        ImageView fire = new ImageView();
        try {
            fire.setImage(new Image(getClass().getResource("/fire2.gif").toExternalForm()));
            fire.setFitWidth(34);
            fire.setFitHeight(34);
        } catch (Exception ignored) {}
        return fire;
    }

    private ImageView createSunkMark() {
        ImageView skull = new ImageView();
        try {
            skull.setImage(new Image(getClass().getResource("/skull.png").toExternalForm()));
            skull.setFitWidth(24);
            skull.setFitHeight(24);
        } catch (Exception ignored) {}
        return skull;
    }
}