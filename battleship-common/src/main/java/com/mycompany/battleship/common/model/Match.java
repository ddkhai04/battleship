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
public class Match {
    private static final long serialVersionUID = 1L;

    
    private int id, player1_id, player2_id;
    private int winner_id, loser_id;
    private Timestamp playedAt;

    public Match() {
    }

    public Match(int id, int player1_id, int player2_id, int winer_id, int loser_id, Timestamp playedAt) {
        this.id = id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.winner_id = winer_id;
        this.loser_id = loser_id;
        this.playedAt = playedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlayer1_id() {
        return player1_id;
    }

    public void setPlayer1_id(int player1_id) {
        this.player1_id = player1_id;
    }

    public int getPlayer2_id() {
        return player2_id;
    }

    public void setPlayer2_id(int player2_id) {
        this.player2_id = player2_id;
    }

    public int getWiner_id() {
        return winner_id;
    }

    public void setWiner_id(int winer_id) {
        this.winner_id = winer_id;
    }

    public int getLoser_id() {
        return loser_id;
    }

    public void setLoser_id(int loser_id) {
        this.loser_id = loser_id;
    }

    public Timestamp getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Timestamp playedAt) {
        this.playedAt = playedAt;
    }

    
    
    
}
