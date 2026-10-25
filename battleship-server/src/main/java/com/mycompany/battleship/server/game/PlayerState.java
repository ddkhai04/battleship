package com.mycompany.battleship.server.game;

import com.mycompany.battleship.common.model.GameConfig;
import com.mycompany.battleship.common.model.MissileType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PlayerState {
    private final String id;
    private final GameConfig config;
    private Board board;
    private boolean ready;
    private int starterBig;
    private final List<MissileType> bonus = new ArrayList<>();
    private int nuclearReceived;
    private int shotsFired;
    private int mysteriesCollected;

    PlayerState(String id, GameConfig config) {
        this.id = id;
        this.config = config;
    }

    void resetForNewMatch(Board newBoard) {
        this.board = newBoard;
        this.ready = false;
        this.starterBig = config.getStarterBigMissiles();
        this.bonus.clear();
        this.nuclearReceived = 0;
        this.shotsFired = 0;
        this.mysteriesCollected = 0;
    }

    String id() { return id; }
    Board board() { return board; }
    void setBoard(Board board) { this.board = board; }
    boolean isReady() { return ready; }
    void setReady(boolean ready) { this.ready = ready; }

    int shotsFired() { return shotsFired; }
    void incrementShots() { shotsFired++; }
    int nuclearReceived() { return nuclearReceived; }
    int bonusCount() { return bonus.size(); }

    int getMysteriesCollected() { return mysteriesCollected; }
    void incrementMysteriesCollected() { mysteriesCollected++; }

    boolean has(MissileType t) {
        if (t == MissileType.SIMPLE) return true;
        if (t == MissileType.BIG && starterBig > 0) return true;
        return bonus.contains(t);
    }

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