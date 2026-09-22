package com.mycompany.battleship.server.game;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Trạng thái của một người chơi trong trận: bàn tàu của mình, kho đạn, số lần bắn.
 * Kho đạn: 2 đạn chữ thập cấp đầu trận (không chiếm chỗ) + tối đa 4 đạn nhận thêm => tối đa 6 lần bắn đặc biệt.
 */
final class PlayerState {
    private final String id;
    private final GameConfig config;
    private Board board;
    private int starterBig;
    private final List<MissileType> bonus = new ArrayList<>();
    private int nuclearReceived;
    private int shotsFired;

    PlayerState(String id, GameConfig config) {
        this.id = id;
        this.config = config;
    }

    void resetForNewMatch(Board newBoard) {
        this.board = newBoard;
        this.starterBig = config.getStarterBigMissiles();
        this.bonus.clear();
        this.nuclearReceived = 0;
        this.shotsFired = 0;
    }

    String id() { return id; }
    Board board() { return board; }
    int shotsFired() { return shotsFired; }
    void incrementShots() { shotsFired++; }
    int nuclearReceived() { return nuclearReceived; }
    int bonusCount() { return bonus.size(); }

    boolean has(MissileType t) {
        if (t == MissileType.SIMPLE) return true;
        if (t == MissileType.BIG && starterBig > 0) return true;
        return bonus.contains(t);
    }

    /** Dùng đạn nhận thêm trước để giải phóng chỗ trong kho, sau đó mới dùng đạn cấp đầu trận. */
    void consume(MissileType t) {
        if (t == MissileType.SIMPLE) return;
        if (bonus.remove(t)) return;
        if (t == MissileType.BIG && starterBig > 0) starterBig--;
    }

    boolean hasBonusSpace() { return bonus.size() < config.getInventoryCapacity(); }

    void addBonus(MissileType t) {
        bonus.add(t);
        if (t == MissileType.NUCLEAR) nuclearReceived++;
    }

    Map<MissileType, Integer> inventorySnapshot() {
        Map<MissileType, Integer> m = new EnumMap<>(MissileType.class);
        for (MissileType t : MissileType.values()) {
            if (!t.isSpecial()) continue;
            int n = 0;
            for (MissileType b : bonus) if (b == t) n++;
            if (t == MissileType.BIG) n += starterBig;
            m.put(t, n);
        }
        return m;
    }
}
