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
public class MatchHistoryItem implements Serializable{
    private int matchId;
    private String opponentName;
    private String result;
    private Timestamp playedAt;

    public MatchHistoryItem() {
    }

    public MatchHistoryItem(int matchId, String opponentName, String result, Timestamp playedAt) {
        this.matchId = matchId;
        this.opponentName = opponentName;
        this.result = result;
        this.playedAt = playedAt;
    }



    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Timestamp getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Timestamp playedAt) {
        this.playedAt = playedAt;
    }
    
    
}
