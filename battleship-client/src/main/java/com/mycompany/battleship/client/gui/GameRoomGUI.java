package com.mycompany.battleship.client.gui;

import com.mycompany.battleship.common.model.GameView;
import com.mycompany.battleship.common.model.MissileType;
import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.ShotResult;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private Timeline turnTimer;
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

//    private final Map<String, Label> weaponBadges = new EnumMap<>(MissileType.class.getName().getClass().isEnum() ? null : null); // fallback
    private final Map<String, Label> badgeMap = new java.util.HashMap<>();
    private final Map<String, ToggleButton> buttonMap = new java.util.HashMap<>();

    private BiConsumer<Integer, Integer> onAttackCellClicked;
    private Consumer<String> onWeaponSelected;
    private Runnable onExitGameClicked;
    private Runnable onRematchClicked;

    private boolean isMyTurn = false;

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

        primaryStage.setTitle("Battleship - " + player1Name + " vs " + player2Name);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (onExitGameClicked != null) {
                onExitGameClicked.run();
            }
        });
        primaryStage.show();
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
        top.setPadding(new Insets(24, 28, 14, 28));

        HBox player1Box = createPlayerInfo(player1Name, player1Score, "avatar-color-1", true);
        player1Box.setPrefWidth(280);
        player1Box.setAlignment(Pos.CENTER_LEFT);

        HBox player2Box = createPlayerInfo(player2Name, player2Score, "avatar-color-2", false);
        Button exitBtn = new Button("Thoát ✖");
        exitBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");

        exitBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận thoát");
            alert.setHeaderText("Bạn có chắc chắn muốn thoát trận đấu?");
            alert.setContentText("Nếu thoát, bạn sẽ bị xử thua ván này!");

            ButtonType btnXacNhan = new ButtonType("Xác nhận");
            ButtonType btnHuy = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
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

        StackPane timerCircle = createTimerCircle();
        turnIndicatorLabel = new Label("Chờ trận đấu...");
        turnIndicatorLabel.getStyleClass().addAll("turn-indicator", "turn-indicator-waiting");

        VBox centerBox = new VBox(8, timerCircle, turnIndicatorLabel);
        centerBox.setAlignment(Pos.CENTER);

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
        Label scoreBadge = new Label("★ " + score);
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

    public void startTurnTimer(int seconds) {
        if (turnTimer != null) turnTimer.stop();
        currentSeconds = seconds;
        setTimerSeconds(currentSeconds);

        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            currentSeconds--;
            if (currentSeconds >= 0) {
                setTimerSeconds(currentSeconds);
            } else {
                turnTimer.stop();
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
        Label header = new Label("Hạm đội của bạn");
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
        Label header = new Label("Bàn cờ đối thủ");
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
                if (!isMyTurn) {
                    playSound("miss.mp3");
                    return;
                }
                if (onAttackCellClicked != null) {
                    String weapon = getSelectedWeapon();
                    switch (weapon) {
                        case "NUCLEAR": playSound("fire_nuke.mp3"); break;
                        case "BIG": playSound("fire_cross.mp3"); break;
                        case "RAIN": playSound("fire_cluster.mp3"); break;
                        default: playSound("fire_normal.mp3"); break;
                    }
                    onAttackCellClicked.accept(row, col);
                }
            });

            cell.setOnMouseEntered(e -> showAimPreview(row, col));
            cell.setOnMouseExited(e -> hideAimPreview());
        }
        return cell;
    }

    public String getSelectedWeapon() {
        if (weaponGroup != null && weaponGroup.getSelectedToggle() != null) {
            return (String) weaponGroup.getSelectedToggle().getUserData();
        }
        return "SIMPLE";
    }

    private void showAimPreview(int centerRow, int centerCol) {
        if (!isMyTurn || weaponGroup == null || weaponGroup.getSelectedToggle() == null) return;
        String weapon = (String) weaponGroup.getSelectedToggle().getUserData();

        hideAimPreview();

        int[][] offsets;
        switch (weapon) {
            case "BIG":
                offsets = new int[][]{{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                break;
            case "NUCLEAR":
                offsets = new int[][]{
                        {-2, 0},
                        {-1, -1}, {-1, 0}, {-1, 1},
                        {0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2},
                        {1, -1}, {1, 0}, {1, 1},
                        {2, 0}
                };
                break;
            default:
                offsets = new int[][]{{0, 0}};
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

        StackPane regular = createWeaponButton("SIMPLE", "dan-thuong.png", "Đạn thường", -1, true);
        StackPane cross = createWeaponButton("BIG", "dan-chu-thap.png", "Đạn chữ thập", 2, false);
        StackPane cluster = createWeaponButton("RAIN", "dan-rai-rac.png", "Đạn rải rác", 0, false);
        StackPane nuke = createWeaponButton("NUCLEAR", "dan-nguyen-tu.png", "Đạn nguyên tử", 0, false);

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

        buttonMap.put(weaponId, btn);

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
            badgeMap.put(weaponId, badge);

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

    public void showCountdown(int seconds) {
        countdownOverlay.setVisible(true);
        countdownOverlay.setManaged(true);
        countdownLabel.setText(seconds > 0 ? String.valueOf(seconds) : "Chiến đấu!");
        playPulse(countdownLabel);

        // Khi đếm tới 1, hẹn 1 giây sau hiện "Chiến đấu!" rồi biến mất hoàn toàn
        if (seconds <= 1) {
            Timeline hideTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), e -> {
                        countdownLabel.setText("Chiến đấu!");
                        playPulse(countdownLabel);
                    }),
                    new KeyFrame(Duration.millis(1600), e -> hideCountdown())
            );
            hideTimeline.play();
        }
    }

    public void hideCountdown() {
        countdownOverlay.setVisible(false);
        countdownOverlay.setManaged(false);
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

        lobbyBtn = new Button("Về sảnh");
        lobbyBtn.getStyleClass().addAll("dialog-button", "dialog-button-secondary");
        rematchBtn = new Button("Tái đấu");
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
        stopTurnTimer();

        if (isWinner) {
            resultIcon.setText("★");
            resultTitle.setText("Chiến thắng!");
            resultTitle.getStyleClass().add("dialog-title-win");
            if ("SURRENDER".equals(reason) || "LEFT".equals(reason)) {
                resultSubtitle.setText("Đối thủ đã bỏ chạy. Bạn được xử thắng!");
                rematchBtn.setVisible(false);
                rematchBtn.setManaged(false);
            } else if ("TIMEOUT".equals(reason)) {
                resultSubtitle.setText("Đối thủ hết thời gian suy nghĩ!");
                rematchBtn.setVisible(true);
                rematchBtn.setManaged(true);
            } else {
                resultSubtitle.setText("Bạn đã đánh chìm toàn bộ hạm đội đối phương.");
                rematchBtn.setVisible(true);
                rematchBtn.setManaged(true);
            }
        } else {
            resultIcon.setText("✖");
            resultTitle.setText("Thất bại");
            resultTitle.getStyleClass().add("dialog-title-lose");
            if ("SURRENDER".equals(reason) || "LEFT".equals(reason)) {
                resultSubtitle.setText("Bạn đã đầu hàng và rời trận đấu.");
                rematchBtn.setVisible(false);
                rematchBtn.setManaged(false);
            } else if ("TIMEOUT".equals(reason)) {
                resultSubtitle.setText("Bạn đã hết thời gian suy nghĩ!");
                rematchBtn.setVisible(true);
                rematchBtn.setManaged(true);
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
            rematchBtn.setDisable(true);
            rematchBtn.setText("Đang chờ...");
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

    public void setOnRematchClicked(Runnable listener) {
        this.onRematchClicked = listener;
    }

    public void setFleetCellState(int row, int col, String state, String shipType) {
        StackPane cell = fleetCells[row][col];
        clearCellStateClasses(cell);
        cell.getChildren().clear();

        if (state.equals("empty")) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createWaterDot());
        } else {
            cell.getStyleClass().addAll("cell-ship", shipType != null ? shipType : SHIP_SIZE_3_A);
            cell.setStyle("-fx-background-radius: 6;");

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
            cell.getStyleClass().addAll("cell-ship", shipType != null ? shipType : SHIP_SIZE_3_B);
            cell.getChildren().add(createSunkMark());
            playSound("sunk.mp3");
        } else if ("miss".equals(state)) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createMissDot());
            playSound("miss.mp3");
        } else if ("gift".equals(state)) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createGiftMark());
            playSound("gift.mp3");
        } else {
            cell.getStyleClass().addAll("water-cell", "cell-interactive");
            cell.getChildren().add(createWaterDot());
        }
    }

    private void clearCellStateClasses(StackPane cell) {
        cell.setStyle("");
        cell.getStyleClass().removeAll("water-cell", "cell-ship", "cell-hit",
                SHIP_SIZE_2, SHIP_SIZE_3_A, SHIP_SIZE_3_B, SHIP_SIZE_4, SHIP_SIZE_5,
                "cell-hover-aim", "cell-interactive");
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
        } catch (Exception ignored) {}
        return fire;
    }

    private javafx.scene.image.ImageView createSunkMark() {
        javafx.scene.image.ImageView skull = new javafx.scene.image.ImageView();
        try {
            skull.setImage(new javafx.scene.image.Image(getClass().getResource("/skull.png").toExternalForm()));
            skull.setFitWidth(24);
            skull.setFitHeight(24);
        } catch (Exception ignored) {}
        return skull;
    }

    private javafx.scene.image.ImageView createGiftMark() {
        javafx.scene.image.ImageView gift = new javafx.scene.image.ImageView();
        try {
            gift.setImage(new javafx.scene.image.Image(getClass().getResource("/gift.png").toExternalForm()));
            gift.setFitWidth(28);
            gift.setFitHeight(28);
            gift.setSmooth(false);
        } catch (Exception ignored) {}
        return gift;
    }

    public void setTurnIndicator(boolean isMyTurn) {
        this.isMyTurn = isMyTurn;
        hideCountdown(); // Đảm bảo tắt màn che mờ ngay khi vào ván

        turnIndicatorLabel.getStyleClass().removeAll("turn-indicator-active", "turn-indicator-waiting");
        if (isMyTurn) {
            turnIndicatorLabel.setText("Lượt của bạn");
            turnIndicatorLabel.getStyleClass().add("turn-indicator-active");
        } else {
            turnIndicatorLabel.setText("Lượt đối thủ");
            turnIndicatorLabel.getStyleClass().add("turn-indicator-waiting");
        }

        // Bật đếm ngược 45s cho CẢ HAI BÊN để người chờ cũng thấy thời gian trôi
        startTurnTimer(45);
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

    // ĐỒNG BỘ TOÀN BỘ BÀN CỜ VÀ ĐẠN TỪ SERVER
    public void renderGameView(GameView view) {
        if (view == null) return;

        // 1. Bàn cờ cá nhân: '.' = nước, 'S' = tàu, 'X' = trúng, 'o' = trượt
        char[][] own = view.ownGrid;
        if (own != null) {
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    char ch = own[r][c];
                    StackPane cell = fleetCells[r][c];
                    clearCellStateClasses(cell);
                    cell.getChildren().clear();

                    if (ch == 'S') {
                        cell.getStyleClass().addAll("cell-ship", SHIP_SIZE_3_A);
                        cell.setStyle("-fx-background-radius: 6;");
                    } else if (ch == 'X') {
                        cell.getStyleClass().addAll("cell-ship", SHIP_SIZE_3_A);
                        cell.getChildren().add(createHitMark());
                    } else if (ch == 'o') {
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createMissDot());
                    } else {
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createWaterDot());
                    }
                }
            }
        }

        // 2. Bàn cờ đối thủ: '.' = chưa bắn, 'X' = trúng, 'o' = trượt, '?' = hộp quà
        char[][] enemy = view.enemyGrid;
        if (enemy != null) {
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    char ch = enemy[r][c];
                    StackPane cell = enemyCells[r][c];
                    clearCellStateClasses(cell);
                    cell.getChildren().clear();

                    if (ch == 'X') {
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createHitMark());
                    } else if (ch == 'o') {
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createMissDot());
                    } else if (ch == '?') {
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createGiftMark());
                    } else {
                        cell.getStyleClass().addAll("water-cell", "cell-interactive");
                        cell.getChildren().add(createWaterDot());
                    }
                }
            }
        }

        // 3. Đánh dấu các tàu đối thủ đã bị đánh chìm
        if (view.sunkEnemyShips != null) {
            for (List<Point> shipCells : view.sunkEnemyShips) {
                for (Point p : shipCells) {
                    StackPane cell = enemyCells[p.getRow()][p.getCol()];
                    cell.getChildren().clear();
                    cell.getStyleClass().add("cell-ship");
                    cell.getChildren().add(createSunkMark());
                }
            }
        }

        // 4. Cập nhật kho đạn
        if (view.inventory != null) {
            for (Map.Entry<MissileType, Integer> entry : view.inventory.entrySet()) {
                String typeName = entry.getKey().name();
                int count = entry.getValue();

                Label badge = badgeMap.get(typeName);
                ToggleButton btn = buttonMap.get(typeName);

                if (badge != null) {
                    badge.setText(String.valueOf(count));
                }
                if (btn != null) {
                    btn.setDisable(count <= 0);
                    if (count <= 0 && btn.isSelected()) {
                        buttonMap.get("SIMPLE").setSelected(true);
                    }
                }
            }
        }
    }

    // CẬP NHẬT KẾT QUẢ PHÁT BẮN
    public void applyShotResult(ShotResult result) {
        if (result == null) return;

        if (result.isDirectHit()) {
            playSound("hit.mp3");
        } else {
            playSound("miss.mp3");
        }

        if (result.getSunkShips() != null && !result.getSunkShips().isEmpty()) {
            playSound("sunk.mp3");
        }

        for (ShotResult.CellResult cell : result.getCells()) {
            if (cell.isMysteryCollected()) {
                playSound("gift.mp3");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}