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

public class UserShotTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String coordinate;
    private int shotCount;
    private int hitCount;

    public UserShotTarget() {}

    public UserShotTarget(int id, int userId, String coordinate, int shotCount, int hitCount) {
        this.id = id;
        this.userId = userId;
        this.coordinate = coordinate;
        this.shotCount = shotCount;
        this.hitCount = hitCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCoordinate() { return coordinate; }
    public void setCoordinate(String coordinate) { this.coordinate = coordinate; }

    public int getShotCount() { return shotCount; }
    public void setShotCount(int shotCount) { this.shotCount = shotCount; }

    public int getHitCount() { return hitCount; }
    public void setHitCount(int hitCount) { this.hitCount = hitCount; }
}
