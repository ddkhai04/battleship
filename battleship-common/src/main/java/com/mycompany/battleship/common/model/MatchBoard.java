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

public class MatchBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int matchId;
    private int playerId;
    private String shipData; // Chuỗi JSON vị trí tàu
    private Timestamp createdAt;

    public MatchBoard() {}

    public MatchBoard(int id, int matchId, int playerId, String shipData, Timestamp createdAt) {
        this.id = id;
        this.matchId = matchId;
        this.playerId = playerId;
        this.shipData = shipData;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getShipData() { return shipData; }
    public void setShipData(String shipData) { this.shipData = shipData; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
