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

public class Match implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MatchStatus {
        ONGOING, FINISHED, ABORTED
    }

    private int id;
    private int player1_id;
    private int player2_id;
    private Integer winner_id; 
    private Integer loser_id;  
    private Timestamp playedAt;
    private MatchStatus status;
    private int duration_seconds;

    public Match() {
    }

    public Match(int id, int player1_id, int player2_id, int winer_id, int loser_id, Timestamp playedAt) {
        this.id = id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.winner_id = winer_id;
        this.loser_id = loser_id;
        this.playedAt = playedAt;
        this.status = MatchStatus.FINISHED; // Trận từ code cũ mặc định đã hoàn thành
        this.duration_seconds = 0;
    }

    // Constructor mới: đầy đủ các trường mới và hỗ trợ null cho winner/loser
    public Match(int id, int player1_id, int player2_id, Integer winner_id, Integer loser_id, Timestamp playedAt, MatchStatus status, int duration_seconds) {
        this.id = id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.winner_id = winner_id;
        this.loser_id = loser_id;
        this.playedAt = playedAt;
        this.status = status;
        this.duration_seconds = duration_seconds;
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

    // Các phương thức getter/setter theo code cũ (trả về Integer để tránh lỗi NullPointerException khi trận chưa kết thúc)
    public Integer getWiner_id() {
        return winner_id;
    }

    public void setWiner_id(Integer winer_id) {
        this.winner_id = winer_id;
    }

    // Overload nhận int cho code cũ
    public void setWiner_id(int winer_id) {
        this.winner_id = winer_id;
    }

    // Bổ sung thêm chuẩn chính tả đúng để tiện dùng về sau
    public Integer getWinner_id() {
        return winner_id;
    }

    public void setWinner_id(Integer winner_id) {
        this.winner_id = winner_id;
    }

    public Integer getLoser_id() {
        return loser_id;
    }

    public void setLoser_id(Integer loser_id) {
        this.loser_id = loser_id;
    }

    // Overload nhận int cho code cũ
    public void setLoser_id(int loser_id) {
        this.loser_id = loser_id;
    }

    public Timestamp getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Timestamp playedAt) {
        this.playedAt = playedAt;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public int getDuration_seconds() {
        return duration_seconds;
    }

    public void setDuration_seconds(int duration_seconds) {
        this.duration_seconds = duration_seconds;
    }
}