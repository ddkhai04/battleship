/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battleship.common.model;

/**
 *
 * @author dkhai
 */

import java.io.Serializable;
import java.sql.Timestamp;

public class UserCombatStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private int totalShots;
    private int hits;
    private int currentWinStreak;
    private int longestWinStreak;
    private int currentLossStreak;
    private int longestLossStreak;
    private Timestamp updatedAt;

    public UserCombatStats() {}

    public UserCombatStats(int userId, int totalShots, int hits, int currentWinStreak, int longestWinStreak, int currentLossStreak, int longestLossStreak, Timestamp updatedAt) {
        this.userId = userId;
        this.totalShots = totalShots;
        this.hits = hits;
        this.currentWinStreak = currentWinStreak;
        this.longestWinStreak = longestWinStreak;
        this.currentLossStreak = currentLossStreak;
        this.longestLossStreak = longestLossStreak;
        this.updatedAt = updatedAt;
    }

    public double getAccuracy() {
        return totalShots == 0 ? 0.0 : ((double) hits / totalShots) * 100.0;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getTotalShots() { return totalShots; }
    public void setTotalShots(int totalShots) { this.totalShots = totalShots; }

    public int getHits() { return hits; }
    public void setHits(int hits) { this.hits = hits; }

    public int getCurrentWinStreak() { return currentWinStreak; }
    public void setCurrentWinStreak(int currentWinStreak) { this.currentWinStreak = currentWinStreak; }

    public int getLongestWinStreak() { return longestWinStreak; }
    public void setLongestWinStreak(int longestWinStreak) { this.longestWinStreak = longestWinStreak; }

    public int getCurrentLossStreak() { return currentLossStreak; }
    public void setCurrentLossStreak(int currentLossStreak) { this.currentLossStreak = currentLossStreak; }

    public int getLongestLossStreak() { return longestLossStreak; }
    public void setLongestLossStreak(int longestLossStreak) { this.longestLossStreak = longestLossStreak; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
