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

public class MatchHistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int matchId;
    private String opponentNickname;
    private String result; // "Thắng" hoặc "Thua"
    private Timestamp playedAt;

    public MatchHistoryDTO() {
    }

    public MatchHistoryDTO(int matchId, String opponentNickname, String result, Timestamp playedAt) {
        this.matchId = matchId;
        this.opponentNickname = opponentNickname;
        this.result = result;
        this.playedAt = playedAt;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public String getOpponentNickname() {
        return opponentNickname;
    }

    public void setOpponentNickname(String opponentNickname) {
        this.opponentNickname = opponentNickname;
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

