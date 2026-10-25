package com.mycompany.battleship.common.model;

public class GameConfig {
    private int setupSeconds = 45;         
    private int turnSeconds = 45;
    private int countdownSeconds = 3;
    private int starterBigMissiles = 2;
    private int inventoryCapacity = 4;
    private int mysteryEveryNShots = 3;
    private int rainMin = 4;
    private int rainMax = 7;
    private int maxNuclearPerMatch = 1;
    private int[] shipLengths = {5, 4, 3, 3, 2};

    // --- chat & emote ---
    private int chatMaxLength = 200;
    private int chatMaxMessagesPerWindow = 5;
    private long chatWindowMillis = 10_000;
    private long emoteCooldownMillis = 1_500;
    private int chatHistorySize = 50;

    public static GameConfig defaults() { return new GameConfig(); }

    public int getSetupSeconds() { return setupSeconds; }
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

    public GameConfig setupSeconds(int v) { this.setupSeconds = v; return this; }
    public GameConfig turnSeconds(int v) { this.turnSeconds = v; return this; }
    public GameConfig countdownSeconds(int v) { this.countdownSeconds = v; return this; }
    public GameConfig chatMaxMessagesPerWindow(int v) { this.chatMaxMessagesPerWindow = v; return this; }
    public GameConfig chatWindowMillis(long v) { this.chatWindowMillis = v; return this; }
    public GameConfig emoteCooldownMillis(long v) { this.emoteCooldownMillis = v; return this; }
    public GameConfig chatHistorySize(int v) { this.chatHistorySize = v; return this; }
}