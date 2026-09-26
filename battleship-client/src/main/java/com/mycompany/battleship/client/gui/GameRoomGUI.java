package com.mycompany.battleship.client.gui;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

// THƯ VIỆN ÂM THANH
import javafx.scene.media.AudioClip;
import java.net.URL;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GameRoomGUI extends Application {

    public static final String SHIP_SIZE_2 = "ship-size-2";     
    public static final String SHIP_SIZE_3_A = "ship-size-3-a"; 
    public static final String SHIP_SIZE_3_B = "ship-size-3-b"; 
    public static final String SHIP_SIZE_4 = "ship-size-4";     
    public static final String SHIP_SIZE_5 = "ship-size-5";     

    private static final int GRID_SIZE = 10;
    private static final double CELL_SIZE = 36;

    private String player1Name = "Người chơi 1";
    private String player2Name = "Đối thủ";
    private int player1Score = 0;
    private int player2Score = 0;

    private final StackPane[][] fleetCells = new StackPane[GRID_SIZE][GRID_SIZE];
    private final StackPane[][] enemyCells = new StackPane[GRID_SIZE][GRID_SIZE];

    private Label timerLabel;
    private Timeline turnTimer; // Bộ đếm thời gian
    private int currentSeconds = 45;

    private Label turnIndicatorLabel;
    private Label countdownLabel;
    private ToggleGroup weaponGroup;
    private StackPane countdownOverlay;
    private StackPane resultOverlay;
    private Label resultIcon;
    private Label resultTitle;
    private Label resultSubtitle;
    private Button lobbyBtn;
    private Button rematchBtn;

    private BiConsumer<Integer, Integer> onAttackCellClicked;
    private Consumer<String> onWeaponSelected;
    private Runnable onExitGameClicked;

    private void playSound(String fileName) {
        try {
            URL audioUrl = getClass().getResource("/" + fileName);
            if (audioUrl != null) {
                AudioClip clip = new AudioClip(audioUrl.toExternalForm());
                clip.play();
            }
        } catch (Exception e) {
            System.out.println("Không thể phát âm thanh: " + fileName + " - Lỗi: " + e.getMessage());
        }
    }

    public void setPlayerNames(String p1, String p2) {
        this.player1Name = p1;
        this.player2Name = p2;
    }

    public void setPlayerScores(int p1Score, int p2Score) {
        this.player1Score = p1Score;
        this.player2Score = p2Score;
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane gameLayout = createRootLayout();
        countdownOverlay = createCountdownOverlayContainer();
        resultOverlay = createResultOverlayContainer();

        javafx.scene.image.ImageView bgImageView = new javafx.scene.image.ImageView();
        try {
            String bgPath = getClass().getResource("/ocean-bg.gif").toExternalForm();
            bgImageView.setImage(new javafx.scene.image.Image(bgPath));
            bgImageView.fitWidthProperty().bind(primaryStage.widthProperty());
            bgImageView.fitHeightProperty().bind(primaryStage.heightProperty());
        } catch (Exception e) {
            System.out.println("Lỗi: Không tìm thấy ảnh nền ocean-bg.gif");
        }

        StackPane rootStack = new StackPane(bgImageView, gameLayout, countdownOverlay, resultOverlay);
        rootStack.getStyleClass().add("app-background");

        Scene scene = new Scene(rootStack, 1000, 680);
        scene.getStylesheets().add(resolveCssPath());

        primaryStage.setTitle("Battleship - Phong choi");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadDemoPreviewState();
    }

    private String resolveCssPath() {
        URL cssUrl = getClass().getResource("/game-style.css");
        if (cssUrl != null) {
            return cssUrl.toExternalForm();
        }
        return new File("game-style.css").toURI().toString();
    }

    private BorderPane createRootLayout() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("game-root");
        layout.setTop(createTopBar()); 
        layout.setCenter(createBoardsArea());
        layout.setBottom(createInventoryBar());
        return layout;
    }

    private HBox createTopBar() {
        HBox top = new HBox();
        top.getStyleClass().add("top-bar");
        top.setAlignment(Pos.CENTER);
        // Padding đẩy thanh tụt xuống để bảo vệ hình tròn
        top.setPadding(new Insets(24, 28, 14, 28));

        // KHỐI 1: Bên trái (Ép cứng rộng 280px)
        HBox player1Box = createPlayerInfo(player1Name, player1Score, "avatar-color-1", true);
        player1Box.setPrefWidth(280); 
        player1Box.setAlignment(Pos.CENTER_LEFT);

        // KHỐI 2: Bên phải (Cũng ép cứng rộng 280px)
        HBox player2Box = createPlayerInfo(player2Name, player2Score, "avatar-color-2", false);
        Button exitBtn = new Button("Thoát \u2716");
        exitBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        
        exitBtn.setOnAction(e -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận thoát");
            alert.setHeaderText("Bạn có chắc chắn muốn thoát trận đấu?");
            alert.setContentText("Nếu thoát, bạn sẽ bị xử thua ván này!");

            javafx.scene.control.ButtonType btnXacNhan = new javafx.scene.control.ButtonType("Xác nhận");
            javafx.scene.control.ButtonType btnHuy = new javafx.scene.control.ButtonType("Hủy", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnXacNhan, btnHuy);

            alert.showAndWait().ifPresent(type -> {
                if (type == btnXacNhan) {
                    if (onExitGameClicked != null) {
                        onExitGameClicked.run(); 
                    } else {
                        Stage stage = (Stage) exitBtn.getScene().getWindow();
                        stage.close();
                    }
                }
            });
        });

        HBox rightGroup = new HBox(20, player2Box, exitBtn);
        rightGroup.setPrefWidth(280);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);
        
        // KHỐI 3: Đồng hồ (Chính giữa)
        StackPane timerCircle = createTimerCircle();
        turnIndicatorLabel = new Label("Luot cua ban");
        turnIndicatorLabel.getStyleClass().addAll("turn-indicator", "turn-indicator-active");

        VBox centerBox = new VBox(8, timerCircle, turnIndicatorLabel);
        centerBox.setAlignment(Pos.CENTER);

        // KHỐI 4: Vùng không gian đẩy giãn
        Region spacerLeft = new Region();
        Region spacerRight = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        top.getChildren().addAll(player1Box, spacerLeft, centerBox, spacerRight, rightGroup);
        return top;
    }

    private HBox createPlayerInfo(String name, int score, String avatarColorClass, boolean isLeftPlayer) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        StackPane avatar = createAvatar(name, avatarColorClass);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("player-name");
        Label scoreBadge = new Label("\u2605 " + score);
        scoreBadge.getStyleClass().add("score-badge");
        VBox textBox = new VBox(3, nameLabel, scoreBadge);
        textBox.setAlignment(isLeftPlayer ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        if (isLeftPlayer) {
            box.getChildren().addAll(textBox, avatar);
        } else {
            box.getChildren().addAll(avatar, textBox);
        }
        return box;
    }

    private StackPane createAvatar(String name, String colorStyleClass) {
        Circle avatarBg = new Circle(20);
        avatarBg.getStyleClass().addAll("player-avatar", colorStyleClass);
        Label initial = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("player-avatar-initial");
        return new StackPane(avatarBg, initial);
    }

    private StackPane createTimerCircle() {
        StackPane timerPane = new StackPane();
        timerPane.getStyleClass().add("timer-circle");
        timerPane.setMinSize(64, 64);
        timerPane.setMaxSize(64, 64);
        Circle bg = new Circle(30);
        bg.getStyleClass().add("timer-circle-bg");
        timerLabel = new Label("45");
        timerLabel.getStyleClass().add("timer-label");
        timerPane.getChildren().addAll(bg, timerLabel);
        return timerPane;
    }

    // BỘ ĐẾM THỜI GIAN 45s (Hiển thị UI)
    public void startTurnTimer() {
        if (turnTimer != null) turnTimer.stop();
        currentSeconds = 45;
        setTimerSeconds(currentSeconds);

        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            currentSeconds--;
            if (currentSeconds >= 0) {
                setTimerSeconds(currentSeconds);
            } else {
                turnTimer.stop();
                // Chỉ đếm đến 0 rồi dừng. Việc chuyển lượt sẽ do Server quyết định và báo về.
            }
        }));
        turnTimer.setCycleCount(Timeline.INDEFINITE);
        turnTimer.play();
    }

    public void stopTurnTimer() {
        if (turnTimer != null) turnTimer.stop();
    }

    private HBox createBoardsArea() {
        HBox boards = new HBox(28);
        boards.getStyleClass().add("boards-area");
        boards.setAlignment(Pos.CENTER);
        boards.setPadding(new Insets(24));
        boards.getChildren().addAll(createFleetPanel(), createEnemyPanel());
        return boards;
    }

    private VBox createFleetPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");
        Label header = new Label("Your boats");
        header.getStyleClass().addAll("board-header", "board-header-fleet");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        GridPane grid = buildGrid(fleetCells, false);
        panel.getChildren().addAll(header, grid);
        return panel;
    }

    private VBox createEnemyPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");
        Label header = new Label("Attack your opponent!");
        header.getStyleClass().addAll("board-header", "board-header-enemy");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        GridPane grid = buildGrid(enemyCells, true);
        panel.getChildren().addAll(header, grid);
        return panel;
    }

    private GridPane buildGrid(StackPane[][] cellStore, boolean interactive) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("grid-board");
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                StackPane cell = createCell(row, col, interactive);
                cellStore[row][col] = cell;
                grid.add(cell, col, row);
            }
        }
        return grid;
    }

    private StackPane createCell(int row, int col, boolean interactive) {
        StackPane cell = new StackPane();
        cell.getStyleClass().addAll("grid-cell", "water-cell");
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);
        cell.getChildren().add(createWaterDot());
        
        if (interactive) {
            cell.getStyleClass().add("cell-interactive");
            cell.setOnMouseClicked(e -> {
                if (onAttackCellClicked != null) {
                    if (weaponGroup != null && weaponGroup.getSelectedToggle() != null) {
                        String weapon = (String) weaponGroup.getSelectedToggle().getUserData();
                        switch (weapon) {
                            case "NUKE": playSound("fire_nuke.mp3"); break;
                            case "CROSS": playSound("fire_cross.mp3"); break;
                            case "CLUSTER": playSound("fire_cluster.mp3"); break;
                            default: playSound("fire_normal.mp3"); break;
                        }
                    }
                    onAttackCellClicked.accept(row, col);
                }
            });
            
            cell.setOnMouseEntered(e -> showAimPreview(row, col));
            cell.setOnMouseExited(e -> hideAimPreview());
        }
        return cell;
    }

    private void showAimPreview(int centerRow, int centerCol) {
        if (weaponGroup == null || weaponGroup.getSelectedToggle() == null) return;
        String weapon = (String) weaponGroup.getSelectedToggle().getUserData();

        hideAimPreview(); 

        int[][] offsets;
        switch (weapon) {
            case "CROSS": 
                offsets = new int[][]{{0,0}, {-1,0}, {1,0}, {0,-1}, {0,1}};
                break;
            case "NUKE": 
                offsets = new int[][]{
                                      {-2, 0},
                            {-1,-1},  {-1, 0},  {-1, 1},
                  { 0,-2},  { 0,-1},  { 0, 0},  { 0, 1},  { 0, 2},
                            { 1,-1},  { 1, 0},  { 1, 1},
                                      { 2, 0}
                };
                break;
            default: 
                offsets = new int[][]{{0,0}};
                break;
        }

        for (int[] offset : offsets) {
            int r = centerRow + offset[0];
            int c = centerCol + offset[1];
            if (r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE) {
                if (!enemyCells[r][c].getStyleClass().contains("cell-hover-aim")) {
                    enemyCells[r][c].getStyleClass().add("cell-hover-aim");
                }
            }
        }
    }

    private void hideAimPreview() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                enemyCells[r][c].getStyleClass().remove("cell-hover-aim");
            }
        }
    }

    private HBox createInventoryBar() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("inventory-bar");
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(16, 20, 20, 20));
        weaponGroup = new ToggleGroup();
        StackPane regular = createWeaponButton("NORMAL", "dan-thuong.png", "Đạn thường", -1, true);
        StackPane cross = createWeaponButton("CROSS", "dan-chu-thap.png", "Đạn chữ thập", 2, false);
        StackPane nuke = createWeaponButton("NUKE", "dan-nguyen-tu.png", "Đạn nguyên tử", 1, false);
        StackPane cluster = createWeaponButton("CLUSTER", "dan-rai-rac.png", "Đạn rải rác", 1, false);
        bar.getChildren().addAll(regular, cross, cluster, nuke);
        weaponGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null && onWeaponSelected != null) {
                onWeaponSelected.accept((String) newT.getUserData());
            }
        });
        return bar;
    }

    private StackPane createWeaponButton(String weaponId, String imageFileName, String name, int count, boolean selected) {
        ToggleButton btn = new ToggleButton();
        btn.setToggleGroup(weaponGroup);
        btn.setUserData(weaponId);
        btn.getStyleClass().add("weapon-button");
        btn.setSelected(selected);
        javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView();
        try {
            String imagePath = getClass().getResource("/" + imageFileName).toExternalForm();
            iconView.setImage(new javafx.scene.image.Image(imagePath));
            iconView.setFitWidth(32); 
            iconView.setFitHeight(32);
            iconView.setPreserveRatio(true); 
            iconView.setSmooth(false);
        } catch (Exception e) {
            System.out.println("Lỗi: Không tìm thấy ảnh " + imageFileName);
        }
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("weapon-name");
        VBox content = new VBox(4, iconView, nameLabel);
        content.setAlignment(Pos.CENTER);
        btn.setGraphic(content);
        StackPane wrapper = new StackPane(btn);
        if (count >= 0) {
            Label badge = new Label(String.valueOf(count));
            badge.getStyleClass().add("weapon-count-badge");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(-6, -8, 0, 0));
            wrapper.getChildren().add(badge);
            if (count == 0) {
                btn.setDisable(true);
            }
        }
        return wrapper;
    }

    private StackPane createCountdownOverlayContainer() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("overlay-dim");
        overlay.setVisible(false);
        overlay.setManaged(false);
        countdownLabel = new Label("3");
        countdownLabel.getStyleClass().add("countdown-label");
        overlay.getChildren().add(countdownLabel);
        return overlay;
    }

    public void showCountdownOverlay(Runnable onFinished) {
        countdownOverlay.setVisible(true);
        countdownOverlay.setManaged(true);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0), e -> setCountdownText("3")),
                new KeyFrame(Duration.seconds(1), e -> setCountdownText("2")),
                new KeyFrame(Duration.seconds(2), e -> setCountdownText("1")),
                new KeyFrame(Duration.seconds(3), e -> setCountdownText("Bat dau!")),
                new KeyFrame(Duration.seconds(3.6), e -> {
                    countdownOverlay.setVisible(false);
                    countdownOverlay.setManaged(false);
                    if (onFinished != null) {
                        onFinished.run();
                    }
                })
        );
        timeline.play();
    }

    private void setCountdownText(String text) {
        countdownLabel.setText(text);
        playPulse(countdownLabel);
    }

    private void playPulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(220), node);
        st.setFromX(0.55);
        st.setFromY(0.55);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    private StackPane createResultOverlayContainer() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("overlay-dim");
        overlay.setVisible(false);
        overlay.setManaged(false);

        resultIcon = new Label();
        resultIcon.getStyleClass().add("dialog-icon");
        resultTitle = new Label();
        resultTitle.getStyleClass().add("dialog-title");
        resultSubtitle = new Label();
        resultSubtitle.getStyleClass().add("dialog-subtitle");
        resultSubtitle.setWrapText(true);
        resultSubtitle.setAlignment(Pos.CENTER);

        lobbyBtn = new Button("Ve sanh");
        lobbyBtn.getStyleClass().addAll("dialog-button", "dialog-button-secondary");
        rematchBtn = new Button("Tai dau");
        rematchBtn.getStyleClass().addAll("dialog-button", "dialog-button-primary");

        HBox buttonBar = new HBox(12, lobbyBtn, rematchBtn);
        buttonBar.setAlignment(Pos.CENTER);

        VBox dialogBox = new VBox(14, resultIcon, resultTitle, resultSubtitle, buttonBar);
        dialogBox.getStyleClass().add("dialog-box");
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setPadding(new Insets(32));
        dialogBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        overlay.getChildren().add(dialogBox);
        return overlay;
    }

    public void showResultPopup(boolean isWinner, String reason, Runnable onBackToLobby, Runnable onRematch) {
        resultTitle.getStyleClass().removeAll("dialog-title-win", "dialog-title-lose");
        stopTurnTimer(); // Dừng đồng hồ khi có kết quả

        if (isWinner) {
            resultIcon.setText("\u2605");
            resultTitle.setText("Chiến thắng!");
            resultTitle.getStyleClass().add("dialog-title-win");
            
            if ("SURRENDER".equals(reason)) {
                resultSubtitle.setText("Đối thủ đã bỏ chạy. Bạn được xử thắng!");
                rematchBtn.setVisible(false);
                rematchBtn.setManaged(false);
            } else {
                resultSubtitle.setText("Bạn đã đánh chìm toàn bộ hạm đội đối phương.");
                rematchBtn.setVisible(true);
                rematchBtn.setManaged(true);
            }
        } else {
            resultIcon.setText("\u2716");
            resultTitle.setText("Thất bại");
            resultTitle.getStyleClass().add("dialog-title-lose");
            
            if ("SURRENDER".equals(reason)) {
                resultSubtitle.setText("Bạn đã đầu hàng và rời trận đấu.");
                rematchBtn.setVisible(false);
                rematchBtn.setManaged(false);
            } else {
                resultSubtitle.setText("Hạm đội của bạn đã bị đánh chìm hoàn toàn.");
                rematchBtn.setVisible(true);
                rematchBtn.setManaged(true);
            }
        }

        lobbyBtn.setOnAction(e -> {
            hideResultPopup();
            if (onBackToLobby != null) onBackToLobby.run();
        });
        rematchBtn.setOnAction(e -> {
            hideResultPopup();
            if (onRematch != null) onRematch.run();
        });

        resultOverlay.setVisible(true);
        resultOverlay.setManaged(true);
    }

    public void hideResultPopup() {
        resultOverlay.setVisible(false);
        resultOverlay.setManaged(false);
    }

    public void setOnAttackCellClicked(BiConsumer<Integer, Integer> listener) {
        this.onAttackCellClicked = listener;
    }

    public void setOnWeaponSelected(Consumer<String> listener) {
        this.onWeaponSelected = listener;
    }

    public void setOnExitGameClicked(Runnable listener) {
        this.onExitGameClicked = listener;
    }

    public void placeShip(int[][] coordinates, String shipType) {
        int length = coordinates.length;
        if (length == 0) return;

        boolean isHorizontal = true;
        if (length > 1) {
            isHorizontal = (coordinates[0][0] == coordinates[1][0]);
        }

        for (int i = 0; i < length; i++) {
            int row = coordinates[i][0];
            int col = coordinates[i][1];
            
            String shapeClass;
            if (i == 0) {
                shapeClass = isHorizontal ? "ship-horizontal-head" : "ship-vertical-head";
            } else if (i == length - 1) {
                shapeClass = isHorizontal ? "ship-horizontal-tail" : "ship-vertical-tail";
            } else {
                shapeClass = isHorizontal ? "ship-horizontal-body" : "ship-vertical-body";
            }
            setFleetCellState(row, col, "ship", shipType, shapeClass);
        }
    }

    public void setFleetCellState(int row, int col, String state, String shipType) {
        setFleetCellState(row, col, state, shipType, null);
    }

    public void setFleetCellState(int row, int col, String state, String shipType, String shapeClass) {
        StackPane cell = fleetCells[row][col];
        clearCellStateClasses(cell);
        cell.getChildren().clear();

        if (state.equals("empty")) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createWaterDot());
        } else {
            cell.getStyleClass().addAll("cell-ship", shipType);
            
            if (shapeClass != null) {
                cell.getStyleClass().add(shapeClass);
            } else {
                cell.setStyle("-fx-background-radius: 12;"); 
            }
            
            if (state.equals("hit")) {
                cell.getChildren().add(createHitMark());
            } else if (state.equals("sunk")) {
                cell.getChildren().add(createSunkMark());
            }
        }
    }

    public void setEnemyCellState(int row, int col, String state, String shipType) {
        StackPane cell = enemyCells[row][col];
        clearCellStateClasses(cell);
        cell.getChildren().clear();

        if ("hit".equals(state)) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createHitMark());
            playSound("hit.mp3"); 
            
        } else if ("sunk".equals(state)) {
            cell.getStyleClass().addAll("cell-ship", shipType);
            cell.getChildren().add(createSunkMark());
            playSound("sunk.mp3"); 
            
        } else if ("miss".equals(state)) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createMissDot());
            playSound("miss.mp3"); 
            
        } else if ("gift".equals(state)) {
            // HIỆU ỨNG NHẶT HỘP QUÀ
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createGiftMark());
            playSound("gift.mp3"); // Đảm bảo bạn tải âm thanh gift.mp3 vào resources
            
        } else {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createWaterDot());
        }
    }

    private void clearCellStateClasses(StackPane cell) {
        cell.setStyle(""); 
        cell.getStyleClass().removeAll("water-cell", "cell-ship", "cell-hit",
                SHIP_SIZE_2, SHIP_SIZE_3_A, SHIP_SIZE_3_B, SHIP_SIZE_4, SHIP_SIZE_5,
                "ship-horizontal-head", "ship-horizontal-body", "ship-horizontal-tail",
                "ship-vertical-head", "ship-vertical-body", "ship-vertical-tail",
                "cell-hover-aim"); 
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

    private javafx.scene.image.ImageView createHitMark() {
        javafx.scene.image.ImageView fire = new javafx.scene.image.ImageView();
        try {
            fire.setImage(new javafx.scene.image.Image(getClass().getResource("/fire2.gif").toExternalForm()));
            fire.setFitWidth(34); 
            fire.setFitHeight(34);
        } catch (Exception e) {
            System.out.println("Loi load fire.gif");
        }
        return fire;
    }

    private javafx.scene.image.ImageView createSunkMark() {
        javafx.scene.image.ImageView skull = new javafx.scene.image.ImageView();
        try {
            skull.setImage(new javafx.scene.image.Image(getClass().getResource("/skull.png").toExternalForm()));
            skull.setFitWidth(24); 
            skull.setFitHeight(24);
        } catch (Exception e) {
            System.out.println("Loi load skull.png");
        }
        return skull;
    }

    // TẠO HÌNH ẢNH HỘP QUÀ
    private javafx.scene.image.ImageView createGiftMark() {
        javafx.scene.image.ImageView gift = new javafx.scene.image.ImageView();
        try {
            gift.setImage(new javafx.scene.image.Image(getClass().getResource("/gift.png").toExternalForm()));
            gift.setFitWidth(28); 
            gift.setFitHeight(28);
            gift.setSmooth(false); 
        } catch (Exception e) {
            System.out.println("Loi load gift.png");
        }
        return gift;
    }

    public void setTurnIndicator(boolean isMyTurn) {
        turnIndicatorLabel.getStyleClass().removeAll("turn-indicator-active", "turn-indicator-waiting");
        if (isMyTurn) {
            turnIndicatorLabel.setText("Luot cua ban");
            turnIndicatorLabel.getStyleClass().add("turn-indicator-active");
            startTurnTimer(); // Reset timer khi tới lượt
        } else {
            turnIndicatorLabel.setText("Luot doi thu");
            turnIndicatorLabel.getStyleClass().add("turn-indicator-waiting");
            stopTurnTimer(); // Dừng timer khi hết lượt
            setTimerSeconds(45);
        }
    }

    public void setTimerSeconds(int seconds) {
        timerLabel.setText(String.valueOf(seconds));
        if (seconds <= 10) {
            if (!timerLabel.getStyleClass().contains("timer-label-warn")) {
                timerLabel.getStyleClass().add("timer-label-warn");
            }
        } else {
            timerLabel.getStyleClass().remove("timer-label-warn");
        }
    }

    private void loadDemoPreviewState() {
        placeShip(new int[][]{{1, 1}, {2, 1}}, SHIP_SIZE_2);                                   
        placeShip(new int[][]{{1, 4}, {1, 5}, {1, 6}}, SHIP_SIZE_3_A);                          
        placeShip(new int[][]{{3, 6}, {3, 7}, {3, 8}}, SHIP_SIZE_3_B);                          
        placeShip(new int[][]{{4, 0}, {5, 0}, {6, 0}, {7, 0}}, SHIP_SIZE_4);                    
        placeShip(new int[][]{{8, 1}, {8, 2}, {8, 3}, {8, 4}, {8, 5}}, SHIP_SIZE_5);            

        setFleetCellState(1, 4, "sunk", SHIP_SIZE_3_A); 
        setFleetCellState(1, 5, "sunk", SHIP_SIZE_3_A);
        setFleetCellState(1, 6, "sunk", SHIP_SIZE_3_A); 

        setEnemyCellState(2, 3, "miss", null);
        setEnemyCellState(6, 7, "miss", null);
        setEnemyCellState(4, 4, "hit", null);
        setEnemyCellState(8, 2, "hit", null);
        setEnemyCellState(5, 6, "sunk", SHIP_SIZE_3_B);
        setEnemyCellState(5, 7, "sunk", SHIP_SIZE_3_B);
        setEnemyCellState(5, 8, "sunk", SHIP_SIZE_3_B);
        
        // Demo ô hộp quà
        setEnemyCellState(7, 4, "gift", null); 

        setFleetCellState(1, 1, "hit", SHIP_SIZE_2);
        
        setTurnIndicator(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}