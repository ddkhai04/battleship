package com.mycompany.battleship.client.gui;

import com.mycompany.battleship.common.model.GameView;
import com.mycompany.battleship.common.model.MissileType;
import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.ShotResult;
import javafx.animation.KeyFrame;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    // Kích thước "chuẩn" của màn trận đấu. Khi cửa sổ to/nhỏ hơn, toàn bộ nội dung
    // được phóng to/thu nhỏ theo tỷ lệ (giữ nguyên hình dạng) thay vì để lại khoảng trắng.
    private static final double KHUNG_RONG = 1000;
    private static final double KHUNG_CAO = 680;
    private static final int[] SETUP_SHIP_LENGTHS = {5, 4, 3, 3, 2};
    private static final String[] SETUP_SHIP_NAMES = {
        "Tàu Sân Bay (5 ô)",
        "Thiết Giáp Hạm (4 ô)",
        "Tàu Tuần Dương (3 ô)",
        "Tàu Ngầm (3 ô)",
        "Tàu Khu Trục (2 ô)"
    };
    private static final String[] SETUP_SHIP_STYLES = {
        SHIP_SIZE_5, SHIP_SIZE_4, SHIP_SIZE_3_A, SHIP_SIZE_3_B, SHIP_SIZE_2
    };
    private static final String[] DIRECTION_LABELS = {
        "🔄 Hướng: Sang phải ➔",
        "🔄 Hướng: Xuống dưới ⬇",
        "🔄 Hướng: Sang trái ⬅",
        "🔄 Hướng: Lên trên ⬆"
    };

    private String player1Name = "Người chơi 1";
    private String player2Name = "Đối thủ";
    private int player1Score = 0;
    private int player2Score = 0;

    private final StackPane[][] fleetCells = new StackPane[GRID_SIZE][GRID_SIZE];
    private final StackPane[][] enemyCells = new StackPane[GRID_SIZE][GRID_SIZE];

    private Label timerLabel;
    private Timeline turnTimer;
    private int currentSeconds = 45;
    private int tongGiayLuot = 45;      // tổng số giây của lượt hiện tại (để tính độ dài vòng tròn)
    private Arc vongDemNguoc;           // vòng tròn chạy quanh đồng hồ

    // Giữ lại 2 khung bàn cờ để tô viền sáng khi đến lượt
    private VBox khungHamDoi;
    private VBox khungDoiThu;

    private Label turnIndicatorLabel;
    private Label countdownLabel;
    private ToggleGroup weaponGroup;
    private HBox inventoryBar;
    private StackPane countdownOverlay;
    private StackPane resultOverlay;
    private Label resultIcon;
    private Label resultTitle;
    private Label resultSubtitle;
    private Button lobbyBtn;
    private Button rematchBtn;

    private final Map<String, Label> badgeMap = new HashMap<>();
    private final Map<String, ToggleButton> buttonMap = new HashMap<>();

    private boolean isSetupPhase = false;
    private boolean isSetupLocked = false;
    // 0 = Sang phải (➔), 1 = Xuống dưới (⬇), 2 = Sang trái (⬅), 3 = Lên trên (⬆)
    private int placementDirection = 0;
    private int selectedShipIndex = 0;
    private int lastHoverRow = -1;
    private int lastHoverCol = -1;

    private final List<List<Point>> placedShips = new ArrayList<>();
    private final int[][] setupShipGrid = new int[GRID_SIZE][GRID_SIZE];
    private final HBox[] shipSelectorCards = new HBox[5];
    private final Label[] shipStatusBadges = new Label[5];

    private StackPane rightBoardContainer;
    private VBox enemyBoardPanel;
    private VBox setupControlPanel;
    private Button btnRotateShip;
    private Button btnRandomShips;
    private Button btnClearShips;
    private Button btnReadySetup;
    private Label lblSetupHint;

    private BiConsumer<List<List<Map<String, Integer>>>, Boolean> onShipsPlaced;
    private BiConsumer<Integer, Integer> onAttackCellClicked;
    private Consumer<String> onWeaponSelected;
    private Runnable onExitGameClicked;
    private Runnable onRematchClicked;

    private boolean isMyTurn = false;

    public GameRoomGUI() {
        for (int i = 0; i < 5; i++) {
            placedShips.add(null);
        }
        resetSetupGridArray();
    }

    private void resetSetupGridArray() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                setupShipGrid[r][c] = -1;
            }
        }
    }

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

    public void setOnShipsPlaced(BiConsumer<List<List<Map<String, Integer>>>, Boolean> listener) {
        this.onShipsPlaced = listener;
    }

    @Override
    public void start(Stage primaryStage) {
        LobbyGUI.dongSanhChoHienTai();
        
        
        // Nạp font tiêu đề (dùng cho số đếm ngược và hộp kết quả)
        Font.loadFont(getClass().getResourceAsStream("/TitanOne-Regular.ttf"), 24);

        BorderPane gameLayout = createRootLayout();
        countdownOverlay = createCountdownOverlayContainer();
        resultOverlay = createResultOverlayContainer();

        // Khóa bố cục trận đấu ở kích thước chuẩn 1000x680 ...
        gameLayout.setMinSize(KHUNG_RONG, KHUNG_CAO);
        gameLayout.setPrefSize(KHUNG_RONG, KHUNG_CAO);
        gameLayout.setMaxSize(KHUNG_RONG, KHUNG_CAO);
        // ... rồi bọc trong Group để khi phóng to/thu nhỏ (scale) StackPane vẫn căn giữa đúng
        Group nhomCoGian = new Group(gameLayout);

        StackPane rootStack = new StackPane(nhomCoGian, countdownOverlay, resultOverlay);
        rootStack.getStyleClass().addAll("app-background", "pl-game");

        Scene scene = new Scene(rootStack, KHUNG_RONG, KHUNG_CAO);
        scene.getStylesheets().add(resolveCssPath());
        scene.getStylesheets().add(getClass().getResource("/player.css").toExternalForm());

        // Tỷ lệ phóng = min(rộng cửa sổ / 1000, cao cửa sổ / 680) -> mọi thứ to lên đều nhau,
        // phần dư 2 bên là nền biển (không còn khoảng trắng)
        NumberBinding tiLe = Bindings.min(scene.widthProperty().divide(KHUNG_RONG),
                scene.heightProperty().divide(KHUNG_CAO));
        gameLayout.scaleXProperty().bind(tiLe);
        gameLayout.scaleYProperty().bind(tiLe);
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(560);

        // Dùng EventFilter để bắt phím R mọi lúc không bị nút bấm chiếm Focus
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (isSetupPhase && !isSetupLocked && e.getCode() == KeyCode.R) {
                handleRotateAction();
                e.consume();
            }
        });

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
        inventoryBar = createInventoryBar();
        layout.setBottom(inventoryBar);
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
        Button exitBtn = new Button("Thoát", taoIcon("pi-flag", 14));
        exitBtn.getStyleClass().add("pl-btn-exit");

        exitBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận thoát");
            alert.setHeaderText("Bạn có chắc chắn muốn thoát trận đấu?");
            alert.setContentText("Nếu thoát, bạn sẽ bị xử thua ván này!");

            ButtonType btnXacNhan = new ButtonType("Xác nhận");
            ButtonType btnHuy = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnXacNhan, btnHuy);
            DialogStyle.apply(alert);

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
        timerPane.setMinSize(68, 68);
        timerPane.setMaxSize(68, 68);

        // Vòng nền + vòng tiến độ đặt trong Pane cố định 68x68 (tọa độ tuyệt đối)
        // để khi vòng ngắn dần thì đồng hồ không bị xê dịch
        Circle bg = new Circle(34, 34, 30);
        bg.getStyleClass().add("pl-timer-bg");
        vongDemNguoc = new Arc(34, 34, 30, 30, 90, -360);
        vongDemNguoc.setType(ArcType.OPEN);
        vongDemNguoc.getStyleClass().add("pl-timer-arc");
        Pane vong = new Pane(bg, vongDemNguoc);
        vong.setMinSize(68, 68);
        vong.setMaxSize(68, 68);

        timerLabel = new Label("45");
        timerLabel.getStyleClass().add("timer-label");
        timerPane.getChildren().addAll(vong, timerLabel);
        return timerPane;
    }

    /** Tạo icon vẽ bằng đường SVG (hình dạng nằm trong player.css, ví dụ "pi-boat"). */
    private Region taoIcon(String loai, double co) {
        Region icon = new Region();
        icon.getStyleClass().addAll("pl-icon", loai);
        icon.setMinSize(co, co);
        icon.setPrefSize(co, co);
        icon.setMaxSize(co, co);
        return icon;
    }

    public void startTurnTimer(int seconds) {
        if (turnTimer != null) turnTimer.stop();
        currentSeconds = seconds;
        tongGiayLuot = seconds > 0 ? seconds : 45;
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
        // Không kéo dãn 2 khung bàn cờ theo chiều cao vùng giữa -> khung ôm vừa lưới 10x10
        boards.setFillHeight(false);
        boards.getChildren().addAll(createFleetPanel(), createEnemyPanel());

        VBox fleetPanel = createFleetPanel();
        enemyBoardPanel = createEnemyPanel();
        setupControlPanel = createSetupControlPanel();

        rightBoardContainer = new StackPane(enemyBoardPanel, setupControlPanel);
        setupControlPanel.setVisible(false);
        setupControlPanel.setManaged(false);

        boards.getChildren().addAll(fleetPanel, rightBoardContainer);
        return boards;
    }

    private VBox createFleetPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");
        khungHamDoi = panel;
        Label header = new Label("Hạm đội của bạn", taoIcon("pi-boat", 16));
        header.getStyleClass().addAll("board-header", "board-header-fleet");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        GridPane grid = buildFleetGrid();
        panel.getChildren().addAll(header, grid);
        return panel;
    }

    private VBox createEnemyPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");
        khungDoiThu = panel;
        Label header = new Label("Bàn cờ đối thủ", taoIcon("pi-target", 16));
        header.getStyleClass().addAll("board-header", "board-header-enemy");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        GridPane grid = buildGrid(enemyCells, true);
        panel.getChildren().addAll(header, grid);
        return panel;
    }

    private VBox createSetupControlPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("board-panel");
        panel.setPrefWidth(407);
        panel.setMinWidth(407);
        panel.setMaxWidth(407);

        Label header = new Label("Bố trí Hạm đội (Cách nhau ít nhất 1 ô)");
        header.getStyleClass().addAll("board-header", "board-header-fleet");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);

        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 16, 16));
        body.setAlignment(Pos.TOP_CENTER);

        lblSetupHint = new Label("Click trái để đặt/nhấc tàu. Bấm phím R hoặc Click phải để xoay 4 hướng (hoặc xoay trực tiếp tàu đang trỏ).");
        lblSetupHint.setWrapText(true);
        lblSetupHint.setStyle("-fx-text-fill: #455A64; -fx-font-size: 12px;");

        VBox shipListBox = new VBox(7);
        for (int i = 0; i < 5; i++) {
            final int shipIdx = i;
            HBox card = createShipSelectorCard(shipIdx);
            shipSelectorCards[i] = card;
            shipListBox.getChildren().add(card);
        }

        btnRotateShip = new Button(DIRECTION_LABELS[0]);
        btnRotateShip.getStyleClass().addAll("dialog-button", "dialog-button-secondary");
        btnRotateShip.setOnAction(e -> toggleShipOrientation());

        btnRandomShips = new Button("🎲 Ngẫu nhiên");
        btnRandomShips.getStyleClass().addAll("dialog-button", "dialog-button-secondary");
        btnRandomShips.setOnAction(e -> randomizeSetupShips());

        btnClearShips = new Button("🗑 Xóa bàn");
        btnClearShips.getStyleClass().addAll("dialog-button", "dialog-button-secondary");
        btnClearShips.setOnAction(e -> clearAllSetupShips());

        HBox toolRow = new HBox(8, btnRotateShip, btnRandomShips, btnClearShips);
        toolRow.setAlignment(Pos.CENTER);

        btnReadySetup = new Button("✔ Sẵn sàng chiến đấu");
        btnReadySetup.getStyleClass().addAll("dialog-button", "dialog-button-primary");
        btnReadySetup.setMaxWidth(Double.MAX_VALUE);
        btnReadySetup.setDisable(true);
        btnReadySetup.setOnAction(e -> lockAndSubmitShips());

        body.getChildren().addAll(lblSetupHint, shipListBox, toolRow, btnReadySetup);
        panel.getChildren().addAll(header, body);
        return panel;
    }

    private HBox createShipSelectorCard(int idx) {
        HBox card = new HBox(10);
        card.getStyleClass().add("setup-ship-item");
        card.setAlignment(Pos.CENTER_LEFT);

        HBox previewPill = new HBox(2);
        previewPill.setAlignment(Pos.CENTER_LEFT);
        int len = SETUP_SHIP_LENGTHS[idx];
        for (int k = 0; k < len; k++) {
            Region seg = new Region();
            seg.setPrefSize(16, 16);
            seg.setMinSize(16, 16);
            seg.setMaxSize(16, 16);
            seg.getStyleClass().addAll("cell-ship", SETUP_SHIP_STYLES[idx]);
            seg.setStyle("-fx-background-radius: 5;");
            previewPill.getChildren().add(seg);
        }
        previewPill.setMinWidth(95);

        Label nameLbl = new Label(SETUP_SHIP_NAMES[idx]);
        nameLbl.setStyle("-fx-text-fill: #37474F; -fx-font-weight: 700; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label("Chưa đặt");
        statusBadge.getStyleClass().addAll("setup-status-badge", "setup-status-pending");
        shipStatusBadges[idx] = statusBadge;

        card.getChildren().addAll(previewPill, nameLbl, spacer, statusBadge);
        card.setOnMouseClicked(e -> {
            if (!isSetupPhase || isSetupLocked) return;
            if (placedShips.get(idx) != null) {
                removePlacedShip(idx);
                sendShipsToServer(false);
            }
            selectShipToPlace(idx);
        });
        return card;
    }

    public void startSetupPhase(int seconds) {
        this.isSetupPhase = true;
        this.isSetupLocked = false;
        this.isMyTurn = false;
        this.placementDirection = 0;
        hideCountdown();
        hideResultPopup();

        enemyBoardPanel.setVisible(false);
        enemyBoardPanel.setManaged(false);
        setupControlPanel.setVisible(true);
        setupControlPanel.setManaged(true);
        if (inventoryBar != null) {
            inventoryBar.setDisable(true);
        }

        btnRotateShip.setDisable(false);
        btnRotateShip.setText(DIRECTION_LABELS[0]);
        btnRandomShips.setDisable(false);
        btnClearShips.setDisable(false);
        btnReadySetup.setText("✔ Sẵn sàng chiến đấu");
        lblSetupHint.setText("Click trái để đặt/nhấc tàu. Bấm phím R hoặc Click phải để xoay 4 hướng (hoặc xoay trực tiếp tàu đang trỏ).");

        turnIndicatorLabel.getStyleClass().removeAll("turn-indicator-active", "turn-indicator-waiting");
        turnIndicatorLabel.setText("Đặt tàu (" + seconds + "s)");
        turnIndicatorLabel.getStyleClass().add("turn-indicator-active");

        clearAllSetupShips();
        startTurnTimer(seconds);
    }

    public void onOpponentReady() {
        if (isSetupPhase) {
            if (isSetupLocked) {
                turnIndicatorLabel.setText("Đối thủ đã sẵn sàng!");
            } else {
                lblSetupHint.setText("⚡ Đối thủ đã xếp tàu xong! Hãy hoàn tất hạm đội và bấm Sẵn sàng.");
            }
        }
    }

    private void exitSetupPhase() {
        if (!isSetupPhase) return;
        isSetupPhase = false;
        isSetupLocked = true;
        clearSetupHoverPreview();

        setupControlPanel.setVisible(false);
        setupControlPanel.setManaged(false);
        enemyBoardPanel.setVisible(true);
        enemyBoardPanel.setManaged(true);
        if (inventoryBar != null) {
            inventoryBar.setDisable(false);
        }
    }

    private GridPane buildFleetGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("grid-board");
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                final int r = row;
                final int c = col;
                StackPane cell = createCell(r, c, false);
                cell.setOnMouseEntered(e -> {
                    lastHoverRow = r;
                    lastHoverCol = c;
                    if (isSetupPhase && !isSetupLocked) {
                        showSetupHoverPreview(r, c);
                    }
                });
                cell.setOnMouseExited(e -> {
                    lastHoverRow = -1;
                    lastHoverCol = -1;
                    if (isSetupPhase && !isSetupLocked) {
                        clearSetupHoverPreview();
                    }
                });
                cell.setOnMouseClicked(e -> {
                    if (!isSetupPhase || isSetupLocked) return;
                    if (e.getButton() == MouseButton.SECONDARY) {
                        handleRotateAction();
                    } else if (e.getButton() == MouseButton.PRIMARY) {
                        handleFleetSetupClick(r, c);
                    }
                });
                fleetCells[row][col] = cell;
                grid.add(cell, col, row);
            }
        }
        return grid;
    }

    /**
     * Xử lý khi bấm phím R hoặc Click chuột phải:
     * - Nếu chuột đang trỏ vào 1 tàu đã đặt trên bàn -> xoay trực tiếp tàu đó quanh ô đang trỏ.
     * - Ngược lại -> đổi hướng đặt tàu qua 4 hướng (Phải -> Xuống -> Trái -> Lên).
     */
    private void handleRotateAction() {
        if (!isSetupPhase || isSetupLocked) return;

        if (lastHoverRow >= 0 && lastHoverCol >= 0) {
            int shipIdx = setupShipGrid[lastHoverRow][lastHoverCol];
            if (shipIdx != -1 && placedShips.get(shipIdx) != null) {
                if (tryRotatePlacedShipInPlace(shipIdx, lastHoverRow, lastHoverCol)) {
                    playSound("fire_normal.mp3");
                    refreshSetupBoardView();
                    refreshSetupShipCards();
                    sendShipsToServer(false);
                    return;
                } else {
                    playSound("miss.mp3");
                }
            }
        }
        toggleShipOrientation();
    }

    /** Thử xoay một con tàu đã đặt quanh tâm (pivotRow, pivotCol) hoặc đầu tàu. */
    private boolean tryRotatePlacedShipInPlace(int shipIdx, int pivotRow, int pivotCol) {
        List<Point> currentCells = placedShips.get(shipIdx);
        if (currentCells == null || currentCells.size() <= 1) return false;

        int len = currentCells.size();
        boolean currentlyHorizontal = (currentCells.get(0).getRow() == currentCells.get(1).getRow());

        // Thử các hướng vuông góc trước, sau đó thử các hướng còn lại quanh ô chuột đang trỏ
        int[] tryDirs = currentlyHorizontal ? new int[]{1, 3, 2, 0} : new int[]{0, 2, 1, 3};
        for (int dir : tryDirs) {
            List<Point> candidate = buildCandidateCells(pivotRow, pivotCol, len, dir);
            if (!candidate.equals(currentCells) && canPlaceShipOnSetupGrid(candidate, shipIdx)) {
                // Xóa vị trí cũ và cập nhật vị trí mới
                for (Point p : currentCells) {
                    if (setupShipGrid[p.getRow()][p.getCol()] == shipIdx) {
                        setupShipGrid[p.getRow()][p.getCol()] = -1;
                    }
                }
                placeShipOnSetupGrid(shipIdx, candidate);
                placementDirection = dir;
                btnRotateShip.setText(DIRECTION_LABELS[placementDirection]);
                return true;
            }
        }
        return false;
    }

    private void toggleShipOrientation() {
        if (!isSetupPhase || isSetupLocked) return;
        placementDirection = (placementDirection + 1) % 4;
        btnRotateShip.setText(DIRECTION_LABELS[placementDirection]);
        if (lastHoverRow >= 0 && lastHoverCol >= 0) {
            showSetupHoverPreview(lastHoverRow, lastHoverCol);
        }
    }

    private void selectShipToPlace(int idx) {
        selectedShipIndex = idx;
        refreshSetupShipCards();
    }

    private void refreshSetupShipCards() {
        int placedCount = 0;
        for (int i = 0; i < 5; i++) {
            HBox card = shipSelectorCards[i];
            Label badge = shipStatusBadges[i];
            card.getStyleClass().removeAll("setup-ship-item-selected", "setup-ship-item-placed");
            badge.getStyleClass().removeAll("setup-status-placed", "setup-status-pending");

            if (placedShips.get(i) != null) {
                placedCount++;
                card.getStyleClass().add("setup-ship-item-placed");
                badge.setText("Đã đặt ✓");
                badge.getStyleClass().add("setup-status-placed");
            } else {
                badge.setText("Chưa đặt");
                badge.getStyleClass().add("setup-status-pending");
            }

            if (i == selectedShipIndex && !isSetupLocked) {
                card.getStyleClass().add("setup-ship-item-selected");
            }
        }
        if (!isSetupLocked) {
            btnReadySetup.setDisable(placedCount < 5);
        }
    }

    private void handleFleetSetupClick(int row, int col) {
        int existingShipIdx = setupShipGrid[row][col];
        if (existingShipIdx != -1) {
            removePlacedShip(existingShipIdx);
            selectShipToPlace(existingShipIdx);
            showSetupHoverPreview(row, col);
            sendShipsToServer(false);
            return;
        }

        if (selectedShipIndex < 0 || selectedShipIndex >= 5 || placedShips.get(selectedShipIndex) != null) {
            selectedShipIndex = findNextUnplacedShipIndex();
            if (selectedShipIndex == -1) return;
        }

        int len = SETUP_SHIP_LENGTHS[selectedShipIndex];
        List<Point> candidate = buildCandidateCells(row, col, len, placementDirection);
        if (canPlaceShipOnSetupGrid(candidate, selectedShipIndex)) {
            placeShipOnSetupGrid(selectedShipIndex, candidate);
            playSound("fire_normal.mp3");
            int nextIdx = findNextUnplacedShipIndex();
            selectedShipIndex = (nextIdx != -1) ? nextIdx : selectedShipIndex;
            refreshSetupBoardView();
            refreshSetupShipCards();

            // Gửi ngay trạng thái tàu hiện tại lên Server để Admin xem trực tiếp (real-time)
            sendShipsToServer(false);
        } else {
            playSound("miss.mp3");
        }
    }

    private int findNextUnplacedShipIndex() {
        for (int i = 0; i < 5; i++) {
            if (placedShips.get(i) == null) return i;
        }
        return -1;
    }

    private boolean isAllShipsPlaced() {
        for (int i = 0; i < 5; i++) {
            if (placedShips.get(i) == null) return false;
        }
        return true;
    }

    /**
     * Tạo danh sách các ô của tàu theo 4 hướng:
     * dir = 0: Sang phải (col + k)
     * dir = 1: Xuống dưới (row + k)
     * dir = 2: Sang trái (col - k)
     * dir = 3: Lên trên (row - k)
     */
    private List<Point> buildCandidateCells(int startRow, int startCol, int len, int dir) {
        List<Point> cells = new ArrayList<>(len);
        for (int k = 0; k < len; k++) {
            int r = startRow;
            int c = startCol;
            if (dir == 0) c = startCol + k;
            else if (dir == 1) r = startRow + k;
            else if (dir == 2) c = startCol - k;
            else if (dir == 3) r = startRow - k;
            cells.add(new Point(r, c));
        }
        // Sắp xếp lại từ trái sang phải / từ trên xuống dưới để vẽ đầu tàu và đuôi tàu chuẩn xác
        cells.sort((a, b) -> (a.getRow() == b.getRow())
                ? Integer.compare(a.getCol(), b.getCol())
                : Integer.compare(a.getRow(), b.getRow()));
        return cells;
    }

    private boolean canPlaceShipOnSetupGrid(List<Point> cells, int ignoreShipIdx) {
        for (Point p : cells) {
            int r = p.getRow();
            int c = p.getCol();
            if (r < 0 || r >= GRID_SIZE || c < 0 || c >= GRID_SIZE) return false;

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                        int occupant = setupShipGrid[nr][nc];
                        if (occupant != -1 && occupant != ignoreShipIdx) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void placeShipOnSetupGrid(int shipIdx, List<Point> cells) {
        placedShips.set(shipIdx, new ArrayList<>(cells));
        for (Point p : cells) {
            setupShipGrid[p.getRow()][p.getCol()] = shipIdx;
        }
    }

    private void removePlacedShip(int shipIdx) {
        List<Point> cells = placedShips.get(shipIdx);
        if (cells != null) {
            for (Point p : cells) {
                if (setupShipGrid[p.getRow()][p.getCol()] == shipIdx) {
                    setupShipGrid[p.getRow()][p.getCol()] = -1;
                }
            }
            placedShips.set(shipIdx, null);
        }
        refreshSetupBoardView();
        refreshSetupShipCards();
    }

    private void clearAllSetupShips() {
        if (!isSetupPhase || isSetupLocked) return;
        for (int i = 0; i < 5; i++) {
            placedShips.set(i, null);
        }
        resetSetupGridArray();
        selectedShipIndex = 0;
        refreshSetupBoardView();
        refreshSetupShipCards();
        sendShipsToServer(false);
    }

    private void randomizeSetupShips() {
        if (!isSetupPhase || isSetupLocked) return;
        Random rng = new Random();
        while (true) {
            for (int i = 0; i < 5; i++) {
                placedShips.set(i, null);
            }
            resetSetupGridArray();

            boolean allPlaced = true;
            for (int i = 0; i < 5; i++) {
                int len = SETUP_SHIP_LENGTHS[i];
                boolean placed = false;
                for (int attempt = 0; attempt < 200 && !placed; attempt++) {
                    int dir = rng.nextBoolean() ? 0 : 1;
                    int r = rng.nextInt(dir == 0 ? GRID_SIZE : GRID_SIZE - len + 1);
                    int c = rng.nextInt(dir == 0 ? GRID_SIZE - len + 1 : GRID_SIZE);
                    List<Point> candidate = buildCandidateCells(r, c, len, dir);
                    if (canPlaceShipOnSetupGrid(candidate, i)) {
                        placeShipOnSetupGrid(i, candidate);
                        placed = true;
                    }
                }
                if (!placed) {
                    allPlaced = false;
                    break;
                }
            }
            if (allPlaced) break;
        }

        selectedShipIndex = 0;
        refreshSetupBoardView();
        refreshSetupShipCards();
        sendShipsToServer(false);
    }

    private void lockAndSubmitShips() {
        if (!isSetupPhase || isSetupLocked || !isAllShipsPlaced()) return;
        isSetupLocked = true;
        clearSetupHoverPreview();

        btnRotateShip.setDisable(true);
        btnRandomShips.setDisable(true);
        btnClearShips.setDisable(true);
        btnReadySetup.setDisable(true);
        btnReadySetup.setText("⏳ Đang chờ đối thủ...");
        turnIndicatorLabel.setText("Đã sẵn sàng! Chờ đối thủ...");
        refreshSetupShipCards();

        sendShipsToServer(true);
    }

    /** Gửi toàn bộ các tàu đang có trên bàn cờ (kể cả khi mới đặt 1..4 tàu) để Admin xem real-time. */
    private void sendShipsToServer(boolean isReady) {
        if (onShipsPlaced == null) return;
        List<List<Map<String, Integer>>> payloadShips = new ArrayList<>();
        for (List<Point> ship : placedShips) {
            if (ship == null || ship.isEmpty()) continue;
            List<Map<String, Integer>> shipCoords = new ArrayList<>();
            for (Point p : ship) {
                Map<String, Integer> pt = new HashMap<>();
                pt.put("row", p.getRow());
                pt.put("col", p.getCol());
                shipCoords.add(pt);
            }
            payloadShips.add(shipCoords);
        }
        onShipsPlaced.accept(payloadShips, isReady);
    }

    private void refreshSetupBoardView() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                setFleetCellState(r, c, "empty", null);
            }
        }

        for (int i = 0; i < 5; i++) {
            List<Point> cells = placedShips.get(i);
            if (cells == null || cells.isEmpty()) continue;

            int len = cells.size();
            String shipType = SETUP_SHIP_STYLES[i];
            boolean isHoriz = (len > 1 && cells.get(0).getRow() == cells.get(1).getRow());

            for (int k = 0; k < len; k++) {
                Point p = cells.get(k);
                String shapeClass;
                if (len == 1) {
                    shapeClass = null;
                } else if (k == 0) {
                    shapeClass = isHoriz ? "ship-horizontal-head" : "ship-vertical-head";
                } else if (k == len - 1) {
                    shapeClass = isHoriz ? "ship-horizontal-tail" : "ship-vertical-tail";
                } else {
                    shapeClass = isHoriz ? "ship-horizontal-body" : "ship-vertical-body";
                }
                setFleetCellState(p.getRow(), p.getCol(), "ship", shipType, shapeClass);
            }
        }
    }

    private void showSetupHoverPreview(int row, int col) {
        clearSetupHoverPreview();
        if (!isSetupPhase || isSetupLocked) return;

        // Nếu đang trỏ vào tàu đã đặt thì không đè vùng preview lên tàu đó
        if (setupShipGrid[row][col] != -1) return;

        int targetIdx = selectedShipIndex;
        if (targetIdx < 0 || targetIdx >= 5 || placedShips.get(targetIdx) != null) {
            targetIdx = findNextUnplacedShipIndex();
            if (targetIdx == -1) return;
        }

        int len = SETUP_SHIP_LENGTHS[targetIdx];
        List<Point> candidate = buildCandidateCells(row, col, len, placementDirection);
        boolean valid = canPlaceShipOnSetupGrid(candidate, targetIdx);
        String previewClass = valid ? "cell-place-valid" : "cell-place-invalid";

        for (Point p : candidate) {
            int r = p.getRow();
            int c = p.getCol();
            if (r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE) {
                fleetCells[r][c].getStyleClass().add(previewClass);
            }
        }
    }

    private void clearSetupHoverPreview() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                fleetCells[r][c].getStyleClass().removeAll("cell-place-valid", "cell-place-invalid");
            }
        }
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
        exitSetupPhase();
        countdownOverlay.setVisible(true);
        countdownOverlay.setManaged(true);
        countdownLabel.setText(seconds > 0 ? String.valueOf(seconds) : "Chiến đấu!");
        playPulse(countdownLabel);

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
        stopTurnTimer();

        if (resultOverlay.isVisible() && ("LEFT".equals(reason) || "SURRENDER".equals(reason))) {
            rematchBtn.setVisible(true);
            rematchBtn.setManaged(true);
            rematchBtn.setDisable(false);
            rematchBtn.setText("Tái đấu");
            return;
        }

        resultTitle.getStyleClass().removeAll("dialog-title-win", "dialog-title-lose");

        rematchBtn.setVisible(true);
        rematchBtn.setManaged(true);
        rematchBtn.setDisable(false);
        rematchBtn.setText("Tái đấu");

        if (isWinner) {
            resultIcon.setText("★");
            resultTitle.setText("Chiến thắng!");
            resultTitle.getStyleClass().add("dialog-title-win");
            if ("SURRENDER".equals(reason) || "LEFT".equals(reason)) {
                resultSubtitle.setText("Đối thủ đã bỏ chạy. Bạn được xử thắng!");
            } else if ("TIMEOUT".equals(reason)) {
                resultSubtitle.setText("Đối thủ hết thời gian suy nghĩ!");
            } else {
                resultSubtitle.setText("Bạn đã đánh chìm toàn bộ hạm đội đối phương.");
            }
        } else {
            resultIcon.setText("✖");
            resultTitle.setText("Thất bại");
            resultTitle.getStyleClass().add("dialog-title-lose");
            if ("SURRENDER".equals(reason) || "LEFT".equals(reason)) {
                resultSubtitle.setText("Bạn đã đầu hàng và rời trận đấu.");
            } else if ("TIMEOUT".equals(reason)) {
                resultSubtitle.setText("Bạn đã hết thời gian suy nghĩ!");
            } else {
                resultSubtitle.setText("Hạm đội của bạn đã bị đánh chìm hoàn toàn.");
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

    public void showRematchRequestPopup(String opponentName, Runnable onAccept, Runnable onDecline) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Lời mời tái đấu");
            alert.setHeaderText(opponentName + " muốn tái đấu với bạn!");
            alert.setContentText("Bạn có đồng ý chơi tiếp ván mới không?");

            ButtonType btnDongY = new ButtonType("Đồng ý", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnTuChoi = new ButtonType("Từ chối", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnDongY, btnTuChoi);
            DialogStyle.apply(alert);

            alert.showAndWait().ifPresent(type -> {
                if (type == btnDongY) {
                    hideResultPopup();
                    if (onAccept != null) onAccept.run();
                } else {
                    if (onDecline != null) onDecline.run();
                }
            });
        });
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
        setFleetCellState(row, col, state, shipType, null);
    }

    public void setFleetCellState(int row, int col, String state, String shipType, String shapeClass) {
        StackPane cell = fleetCells[row][col];
        clearCellStateClasses(cell);
        cell.getChildren().clear();

        if ("empty".equals(state)) {
            cell.getStyleClass().add("water-cell");
            cell.getChildren().add(createWaterDot());
        } else {
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
                "cell-hover-aim", "cell-interactive", "cell-place-valid", "cell-place-invalid");
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
        exitSetupPhase();
        this.isMyTurn = isMyTurn;
        hideCountdown();

        turnIndicatorLabel.getStyleClass().removeAll("turn-indicator-active", "turn-indicator-waiting");
        khungDoiThu.getStyleClass().remove("pl-board-active");
        if (isMyTurn) {
            turnIndicatorLabel.setText("LƯỢT CỦA BẠN - KHAI HỎA!");
            turnIndicatorLabel.getStyleClass().add("turn-indicator-active");
            khungDoiThu.getStyleClass().add("pl-board-active");   // bàn cờ đối thủ sáng viền cam
            playPulse(turnIndicatorLabel);
        } else {
            turnIndicatorLabel.setText("Lượt đối thủ...");
            turnIndicatorLabel.getStyleClass().add("turn-indicator-waiting");
        }

        startTurnTimer(45);
    }

    public void setTimerSeconds(int seconds) {
        timerLabel.setText(String.valueOf(seconds));
        // Vòng tròn ngắn dần theo thời gian còn lại (đủ vòng = 360 độ)
        double phanCon = Math.max(0, Math.min(1, (double) seconds / tongGiayLuot));
        vongDemNguoc.setLength(-360 * phanCon);
        if (seconds <= 10) {
            if (!timerLabel.getStyleClass().contains("timer-label-warn")) {
                timerLabel.getStyleClass().add("timer-label-warn");
            }
            if (!vongDemNguoc.getStyleClass().contains("pl-timer-arc-warn")) {
                vongDemNguoc.getStyleClass().add("pl-timer-arc-warn");
            }
        } else {
            timerLabel.getStyleClass().remove("timer-label-warn");
            vongDemNguoc.getStyleClass().remove("pl-timer-arc-warn");
        }
    }

    public void renderGameView(GameView view) {
        if (view == null) return;

        exitSetupPhase();

        char[][] own = view.ownGrid;
        if (own != null) {
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    char ch = own[r][c];
                    if (ch == 'o') {
                        StackPane cell = fleetCells[r][c];
                        clearCellStateClasses(cell);
                        cell.getChildren().clear();
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createMissDot());
                    } else if (ch != 'S' && ch != 'X') {
                        StackPane cell = fleetCells[r][c];
                        clearCellStateClasses(cell);
                        cell.getChildren().clear();
                        cell.getStyleClass().add("water-cell");
                        cell.getChildren().add(createWaterDot());
                    }
                }
            }

            boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
            java.util.List<java.util.List<int[]>> ships = new java.util.ArrayList<>();

            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    char ch = own[r][c];
                    if ((ch == 'S' || ch == 'X') && !visited[r][c]) {
                        java.util.List<int[]> comp = new java.util.ArrayList<>();
                        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
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
                                    char nch = own[nr][nc];
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

            ships.sort((a, b) -> Integer.compare(b.size(), a.size()));

            boolean hasAssigned3A = false;
            for (java.util.List<int[]> shipCellsList : ships) {
                int len = shipCellsList.size();
                String shipType;
                if (len == 5) {
                    shipType = SHIP_SIZE_5;
                } else if (len == 4) {
                    shipType = SHIP_SIZE_4;
                } else if (len == 3) {
                    if (!hasAssigned3A) {
                        shipType = SHIP_SIZE_3_A;
                        hasAssigned3A = true;
                    } else {
                        shipType = SHIP_SIZE_3_B;
                    }
                } else if (len == 2) {
                    shipType = SHIP_SIZE_2;
                } else {
                    shipType = SHIP_SIZE_3_A;
                }

                boolean isShipSunk = true;
                for (int[] p : shipCellsList) {
                    if (own[p[0]][p[1]] != 'X') {
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
                    char ch = own[r][c];

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

                    String state;
                    if (ch == 'X') {
                        state = isShipSunk ? "sunk" : "hit";
                    } else {
                        state = "ship";
                    }

                    setFleetCellState(r, c, state, shipType, shapeClass);
                }
            }
        }

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

        if (view.sunkEnemyShips != null) {
            for (List<Point> shipCells : view.sunkEnemyShips) {
                int len = shipCells.size();
                String sType = SHIP_SIZE_3_A;
                if (len == 5) sType = SHIP_SIZE_5;
                else if (len == 4) sType = SHIP_SIZE_4;
                else if (len == 2) sType = SHIP_SIZE_2;

                java.util.List<Point> sortedCells = new java.util.ArrayList<>(shipCells);
                sortedCells.sort((a, b) -> (a.getRow() == b.getRow()) ? Integer.compare(a.getCol(), b.getCol()) : Integer.compare(a.getRow(), b.getRow()));
                boolean isHorizontal = (len > 1 && sortedCells.get(0).getRow() == sortedCells.get(1).getRow());

                for (int i = 0; i < len; i++) {
                    Point p = sortedCells.get(i);
                    StackPane cell = enemyCells[p.getRow()][p.getCol()];
                    clearCellStateClasses(cell);
                    cell.getChildren().clear();

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

                    cell.getStyleClass().addAll("cell-ship", sType);
                    if (shapeClass != null) {
                        cell.getStyleClass().add(shapeClass);
                    } else {
                        cell.setStyle("-fx-background-radius: 6;");
                    }
                    cell.getChildren().add(createSunkMark());
                }
            }
        }

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