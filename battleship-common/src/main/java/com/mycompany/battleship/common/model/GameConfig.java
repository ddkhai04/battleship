package com.mycompany.battleship.common.model;

/** Các hằng số luật chơi. Mặc định đúng theo đặc tả; có setter để test dễ chỉnh (vd turnSeconds = 1). */
public class GameConfig {
    private int turnSeconds = 45;
    private int countdownSeconds = 3;
    private int starterBigMissiles = 2;    // đạn chữ thập cấp đầu trận
    private int inventoryCapacity = 4;     // kho chứa tối đa 4 đạn nhận thêm
    private int mysteryEveryNShots = 3;    // cứ 3 lần bắn xuất hiện 1 dấu "?"
    private int rainMin = 4;
    private int rainMax = 7;
    private int maxNuclearPerMatch = 1;
    private int[] shipLengths = {5, 4, 3, 3, 2};

    // --- chat & emote ---
    private int chatMaxLength = 200;            // số ký tự tối đa mỗi tin
    private int chatMaxMessagesPerWindow = 5;   // tối đa 5 tin ...
    private long chatWindowMillis = 10_000;     // ... trong 10 giây
    private long emoteCooldownMillis = 1_500;   // giãn cách giữa 2 emote của cùng 1 người
    private int chatHistorySize = 50;           // số tin giữ lại trong phòng (để gửi lại khi kết nối lại)

    public static GameConfig defaults() { return new GameConfig(); }

    public int getTurnSeconds() { return turnSeconds; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public int getStarterBigMissiles() { return starterBigMissiles; }
    public int getInventoryCapacity() { return inventoryCapacity; }
    public int getMysteryEveryNShots() { return mysteryEveryNShots; }
    public int getRainMin() { return rainMin; }
    public int getRainMax() { return rainMax; }
    public int getMaxNuclearPerMatch() { return maxNuclearPerMatch; }
    public int[] getShipLengths() { return shipLengths.clone(); }
    public int getChatMaxLength() { return chatMaxLength; }
    public int getChatMaxMessagesPerWindow() { return chatMaxMessagesPerWindow; }
    public long getChatWindowMillis() { return chatWindowMillis; }
    public long getEmoteCooldownMillis() { return emoteCooldownMillis; }
    public int getChatHistorySize() { return chatHistorySize; }

    public GameConfig turnSeconds(int v) { this.turnSeconds = v; return this; }
    public GameConfig countdownSeconds(int v) { this.countdownSeconds = v; return this; }
    public GameConfig chatMaxMessagesPerWindow(int v) { this.chatMaxMessagesPerWindow = v; return this; }
    public GameConfig chatWindowMillis(long v) { this.chatWindowMillis = v; return this; }
    public GameConfig emoteCooldownMillis(long v) { this.emoteCooldownMillis = v; return this; }
    public GameConfig chatHistorySize(int v) { this.chatHistorySize = v; return this; }
}
