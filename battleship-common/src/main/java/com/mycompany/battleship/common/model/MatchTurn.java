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

public class MatchTurn implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TurnResult {
        MISS, HIT, SUNK
    }

    private int id;
    private int matchId;
    private int turnNumber;
    private int playerId;
    private String coordinate;
    private TurnResult result;
    private String sunkShipType;
    private Timestamp createdAt;

    public MatchTurn() {}

    public MatchTurn(int id, int matchId, int turnNumber, int playerId, String coordinate, TurnResult result, String sunkShipType, Timestamp createdAt) {
        this.id = id;
        this.matchId = matchId;
        this.turnNumber = turnNumber;
        this.playerId = playerId;
        this.coordinate = coordinate;
        this.result = result;
        this.sunkShipType = sunkShipType;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getCoordinate() { return coordinate; }
    public void setCoordinate(String coordinate) { this.coordinate = coordinate; }

    public TurnResult getResult() { return result; }
    public void setResult(TurnResult result) { this.result = result; }

    public String getSunkShipType() { return sunkShipType; }
    public void setSunkShipType(String sunkShipType) { this.sunkShipType = sunkShipType; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
