package com.mycompany.battleship.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mycompany.battleship.common.model.MessageDTO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SpectateGUI extends Stage {

    public static final String SHIP_SIZE_2 = "ship-size-2";
    public static final String SHIP_SIZE_3_A = "ship-size-3-a";
    public static final String SHIP_SIZE_3_B = "ship-size-3-b";
    public static final String SHIP_SIZE_4 = "ship-size-4";
    public static final String SHIP_SIZE_5 = "ship-size-5";
    
    private static final int GRID_SIZE = 10;
    private static final double CELL_SIZE = 36;

    // Khung chuẩn 1000x680 giống màn trận đấu; cửa sổ to/nhỏ thì phóng/thu theo tỷ lệ
    private static final double KHUNG_RONG = 1000;
    private static final double KHUNG_CAO = 680;

    private static SpectateGUI instance;

    private final StackPane[][] p1Cells = new StackPane[GRID_SIZE][GRID_SIZE];
    private final StackPane[][] p2Cells = new StackPane[GRID_SIZE][GRID_SIZE];

    private Label lblP1Name;
    private Label lblP2Name;
    private Label lblP1Initial;
    private Label lblP2Initial;
    private Label lblHeaderP1;
    private Label lblHeaderP2;
    private Label turnIndicatorLabel;
    private Label timerLabel;
    private boolean daHienThongBaoKetThuc = false;
    public static void moHoacCapNhat(JsonObject data) {
        Platform.runLater(() -> {
            if (instance == null || !instance.isShowing()) {
                instance = new SpectateGUI();
                instance.show();
            }
            instance.capNhat(data);
        });
    }

    public SpectateGUI() {
        setTitle("Battleship - Giám sát trận đấu (God View)");

        BorderPane gameLayout = createRootLayout();
        gameLayout.setMinSize(KHUNG_RONG, KHUNG_CAO);
        gameLayout.setPrefSize(KHUNG_RONG, KHUNG_CAO);
        gameLayout.setMaxSize(KHUNG_RONG, KHUNG_CAO);
        Group nhomCoGian = new Group(gameLayout);

        StackPane rootStack = new StackPane(nhomCoGian);
        rootStack.getStyleClass().addAll("app-background", "pl-game");

        Scene scene = new Scene(rootStack, KHUNG_RONG, KHUNG_CAO);
        scene.getStylesheets().add(resolveCssPath());
        scene.getStylesheets().add(getClass().getResource("/player.css").toExternalForm());

        NumberBinding tiLe = Bindings.min(scene.widthProperty().divide(KHUNG_RONG),
                scene.heightProperty().divide(KHUNG_CAO));
        gameLayout.scaleXProperty().bind(tiLe);
        gameLayout.scaleYProperty().bind(tiLe);
        setMinWidth(760);
        setMinHeight(560);

        setScene(scene);
        setOnCloseRequest(e -> thoatCheDoXem());
    }

    private String resolveCssPath() {
        URL cssUrl = getClass().getResource("/game-style.css");
        if (cssUrl != null) {
            return cssUrl.toExternalForm();
        }
        return new File("game-style.css").toURI().toString();
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

    private void thoatCheDoXem() {
        SocketClientManager.guiTinNhan(new MessageDTO("SPECTATE_LEAVE", null));
        instance = null;
        close();
    }

    private BorderPane createRootLayout() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("game-root");
        layout.setTop(createTopBar());
        layout.setCenter(createBoardsArea());
        return layout;
    }

    private HBox createTopBar() {
        HBox top = new HBox();
        top.getStyleClass().add("top-bar");
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(24, 28, 14, 28));

        HBox player1Box = createPlayerInfo("P1", "avatar-color-1", true);
        player1Box.setPrefWidth(280);
        player1Box.setAlignment(Pos.CENTER_LEFT);

        HBox player2Box = createPlayerInfo("P2", "avatar-color-2", false);

        Button exitBtn = new Button("Thoát", taoIcon("pi-flag", 14));
        exitBtn.getStyleClass().add("pl-btn-exit");
        exitBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Rời chế độ giám sát");
            alert.setHeaderText("Bạn có chắc chắn muốn thoát xem trực tiếp?");
            alert.setContentText("Bạn sẽ quay về màn hình quản trị.");

            ButtonType btnXacNhan = new ButtonType("Thoát");
            ButtonType btnHuy = new ButtonType("Ở lại", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnXacNhan, btnHuy);
            DialogStyle.apply(alert);

            alert.showAndWait().ifPresent(type -> {
                if (type == btnXacNhan) {
                    thoatCheDoXem();
                }
            });
        });

        Region iconLoa = taoIcon(SplashGUI.tatTieng ? "pi-mute" : "pi-volume", 18);
        Button btnAmThanh = new Button();
        btnAmThanh.setGraphic(iconLoa);
        btnAmThanh.getStyleClass().add("pl-btn-sound");
        btnAmThanh.setOnAction(e -> {
            SplashGUI.doiTieng();
            iconLoa.getStyleClass().removeAll("pi-volume", "pi-mute");
            iconLoa.getStyleClass().add(SplashGUI.tatTieng ? "pi-mute" : "pi-volume");
        });

        HBox rightGroup = new HBox(20, player2Box, btnAmThanh, exitBtn);
        rightGroup.setPrefWidth(280);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        StackPane timerCircle = createTimerCircle();
        turnIndicatorLabel = new Label("Đang theo dõi...");
        turnIndicatorLabel.getStyleClass().addAll("turn-indicator", "turn-indicator-active");

        VBox centerBox = new VBox(8, timerCircle, turnIndicatorLabel);
        centerBox.setAlignment(Pos.CENTER);

        Region spacerLeft = new Region();
        Region spacerRight = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        top.getChildren().addAll(player1Box, spacerLeft, centerBox, spacerRight, rightGroup);
        return top;
    }

    private HBox createPlayerInfo(String defaultName, String avatarColorClass, boolean isLeftPlayer) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);

        Circle avatarBg = new Circle(20);
        avatarBg.getStyleClass().addAll("player-avatar", avatarColorClass);
        Label initial = new Label(defaultName.substring(0, 1));
        initial.getStyleClass().add("player-avatar-initial");
        StackPane avatar = new StackPane(avatarBg, initial);

        Label nameLabel = new Label(defaultName);
        nameLabel.getStyleClass().add("player-name");

        Label badge = new Label("ĐANG XEM");
        badge.getStyleClass().add("score-badge");

        VBox textBox = new VBox(3, nameLabel, badge);
        textBox.setAlignment(isLeftPlayer ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        if (isLeftPlayer) {
            lblP1Name = nameLabel;
            lblP1Initial = initial;
            box.getChildren().addAll(textBox, avatar);
        } else {
            lblP2Name = nameLabel;
            lblP2Initial = initial;
            box.getChildren().addAll(avatar, textBox);
        }
        return box;
    }

    private StackPane createTimerCircle() {
        StackPane timerPane = new StackPane();
        timerPane.getStyleClass().add("timer-circle");
        timerPane.setMinSize(64, 64);
        timerPane.setMaxSize(64, 64);
        Circle bg = new Circle(30);
        bg.getStyleClass().add("pl-timer-bg");
        Region iconMat = taoIcon("pi-eye", 26);
        iconMat.getStyleClass().add("pl-eye-icon");
        timerLabel = new Label();
        timerPane.getChildren().addAll(bg, iconMat);
        return timerPane;
    }

    private HBox createBoardsArea() {
        HBox boards = new HBox(28);
        boards.getStyleClass().add("boards-area");
        boards.setAlignment(Pos.CENTER);
        boards.setPadding(new Insets(24));
        boards.setFillHeight(false);   // khung ôm vừa lưới 10x10

        boards.getChildren().addAll(
                createBoardPanel("Hạm đội của P1", "board-header-fleet", p1Cells, true),
                createBoardPanel("Hạm đội của P2", "board-header-fleet", p2Cells, false)
        );
        return boards;
    }

    private VBox createBoardPanel(String title, String headerClass, StackPane[][] cells, boolean isP1) {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");
        Label header = new Label(title, taoIcon("pi-boat", 16));
        header.getStyleClass().addAll("board-header", headerClass);
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



    public void capNhat(JsonObject data) {
        String p1 = data.has("player1") ? data.get("player1").getAsString() : "Player 1";
        String p2 = data.has("player2") ? data.get("player2").getAsString() : "Player 2";
        String turn = data.has("currentTurn") && !data.get("currentTurn").isJsonNull() ? data.get("currentTurn").getAsString() : "";
        String phase = data.has("phase") && !data.get("phase").isJsonNull() ? data.get("phase").getAsString() : "";

        lblP1Name.setText(p1);
        lblP2Name.setText(p2);
        if (!p1.isEmpty()) lblP1Initial.setText(p1.substring(0, 1).toUpperCase());
        if (!p2.isEmpty()) lblP2Initial.setText(p2.substring(0, 1).toUpperCase());

        lblHeaderP1.setText("Hạm đội của " + p1);
        lblHeaderP2.setText("Hạm đội của " + p2);

        if (data.has("grid1")) renderFleetBoard(p1Cells, parseGrid(data.getAsJsonArray("grid1")));
        if (data.has("grid2")) renderFleetBoard(p2Cells, parseGrid(data.getAsJsonArray("grid2")));

        if ("PLAYING".equalsIgnoreCase(phase) || "COUNTDOWN".equalsIgnoreCase(phase)) {
            daHienThongBaoKetThuc = false;
        }

        if (!turn.isEmpty() && !"FINISHED".equalsIgnoreCase(phase)) {
            turnIndicatorLabel.setText("Lượt bắn: " + turn);
        } else {
            turnIndicatorLabel.setText("Trận đấu đã kết thúc");
            if (!daHienThongBaoKetThuc && ("FINISHED".equalsIgnoreCase(phase) || turn.isEmpty())) {
                daHienThongBaoKetThuc = true;
                String winner = data.has("winner") && !data.get("winner").isJsonNull() ? data.get("winner").getAsString() : "";
                String loser = data.has("loser") && !data.get("loser").isJsonNull() ? data.get("loser").getAsString() : "";
                String reason = data.has("reason") && !data.get("reason").isJsonNull() ? data.get("reason").getAsString() : "";

                String lyDoText;
                if ("ALL_SHIPS_SUNK".equalsIgnoreCase(reason)) {
                    lyDoText = "Toàn bộ hạm đội của " + loser + " đã bị bắn chìm hoàn toàn!";
                } else if ("TIMEOUT".equalsIgnoreCase(reason)) {
                    lyDoText = "Người chơi " + loser + " đã hết thời gian suy nghĩ (Timeout)!";
                } else if ("LEFT".equalsIgnoreCase(reason)) {
                    lyDoText = "Người chơi " + loser + " đã đầu hàng / rời khỏi trận đấu!";
                } else {
                    lyDoText = "Một người chơi đã nhận thất bại.";
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Kết thúc trận đấu");
                    alert.setHeaderText(winner.isEmpty() ? "Trận đấu đã kết thúc!" : "Chiến thắng: " + winner);
                    alert.setContentText((!loser.isEmpty() ? "Thất bại: " + loser + "\n" : "") + "Lý do: " + lyDoText);
                    DialogStyle.apply(alert);
                    alert.show();
                });
            }
        }
    }

    private char[][] parseGrid(JsonArray arr) {
        char[][] grid = new char[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            if (r < arr.size()) {
                JsonElement rowEl = arr.get(r);
                if (rowEl.isJsonArray()) {
                    JsonArray rowArr = rowEl.getAsJsonArray();
                    for (int c = 0; c < GRID_SIZE; c++) {
                        if (c < rowArr.size()) {
                            String s = rowArr.get(c).getAsString();
                            grid[r][c] = (!s.isEmpty()) ? s.charAt(0) : '.';
                        } else {
                            grid[r][c] = '.';
                        }
                    }
                } else if (rowEl.isJsonPrimitive()) {
                    String rowStr = rowEl.getAsString();
                    for (int c = 0; c < GRID_SIZE; c++) {
                        grid[r][c] = (c < rowStr.length()) ? rowStr.charAt(c) : '.';
                    }
                }
            }
        }
        return grid;
    }

    /**
     * Thuật toán gom cụm và bo tròn tàu tương tự renderGameView trong GameRoomGUI
     */
    private void renderFleetBoard(StackPane[][] cells, char[][] grid) {
        // 1. Vẽ ô nước và ô bắn trượt
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                char ch = grid[r][c];
                StackPane cell = cells[r][c];
                clearCellStateClasses(cell);
                cell.getChildren().clear();

                if (ch == 'o') {
                    cell.getStyleClass().add("water-cell");
                    cell.getChildren().add(createMissDot());
                } else if (ch == '?') {
                    cell.getStyleClass().add("water-cell");
                    cell.getChildren().add(createGiftMark());
                } else if (ch != 'S' && ch != 'X') {
                    cell.getStyleClass().add("water-cell");
                    cell.getChildren().add(createWaterDot());
                }
            }
        }

        // 2. Gom nhóm các ô tàu bằng BFS
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
        List<List<int[]>> ships = new ArrayList<>();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                char ch = grid[r][c];
                if ((ch == 'S' || ch == 'X') && !visited[r][c]) {
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
                                char nch = grid[nr][nc];
                                if ((nch == 'S' || nch == 'X') && !visited[nr][nc]) {
                                    visited[nr][nc] = true;
                                    queue.add(new int[]{nr, nc});
                                }
                            }
                        }
                    }
                    ships.add(comp);
                }
            }
        }

        // Sắp xếp tàu dài trước: 5, 4, 3, 3, 2
        ships.sort((a, b) -> Integer.compare(b.size(), a.size()));

        boolean hasAssigned3A = false;
        for (List<int[]> shipCellsList : ships) {
            int len = shipCellsList.size();
            String shipType;
            if (len == 5) shipType = SHIP_SIZE_5;
            else if (len == 4) shipType = SHIP_SIZE_4;
            else if (len == 3) {
                if (!hasAssigned3A) {
                    shipType = SHIP_SIZE_3_A;
                    hasAssigned3A = true;
                } else {
                    shipType = SHIP_SIZE_3_B;
                }
            } else if (len == 2) shipType = SHIP_SIZE_2;
            else shipType = SHIP_SIZE_3_A;

            boolean isShipSunk = true;
            for (int[] p : shipCellsList) {
                if (grid[p[0]][p[1]] != 'X') {
                    isShipSunk = false;
                    break;
                }
            }

            shipCellsList.sort((a, b) -> (a[0] == b[0]) ? Integer.compare(a[1], b[1]) : Integer.compare(a[0], b[0]));
            boolean isHorizontal = (len > 1 && shipCellsList.get(0)[0] == shipCellsList.get(1)[0]);

            for (int i = 0; i < len; i++) {
                int[] p = shipCellsList.get(i);
                int r = p[0];
                int c = p[1];
                char ch = grid[r][c];

                String shapeClass;
                if (len == 1) {
                    shapeClass = null;
                } else if (i == 0) {
                    shapeClass = isHorizontal ? "ship-horizontal-head" : "ship-vertical-head";
                } else if (i == len - 1) {
                    shapeClass = isHorizontal ? "ship-horizontal-tail" : "ship-vertical-tail";
                } else {
                    shapeClass = isHorizontal ? "ship-horizontal-body" : "ship-vertical-body";
                }

                String state = (ch == 'X') ? (isShipSunk ? "sunk" : "hit") : "ship";
                setFleetCellState(cells, r, c, state, shipType, shapeClass);
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

    private ImageView createGiftMark() {
        ImageView gift = new ImageView();
        try {
            gift.setImage(new Image(getClass().getResource("/gift.png").toExternalForm()));
            gift.setFitWidth(28);
            gift.setFitHeight(28);
            gift.setSmooth(false);
        } catch (Exception ignored) {}
        return gift;
    }
}