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

public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String nickname;
    private String status;
    private int score;
    private int wins;
    private int losses;

    public UserDTO() {
    }

    public UserDTO(int id, String nickname, String status, int score, int wins, int losses) {
        this.id = id;
        this.nickname = nickname;
        this.status = status;
        this.score = score;
        this.wins = wins;
        this.losses = losses;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
}