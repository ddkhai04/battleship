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

public class UserShipPlacement implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String coordinate;
    private int placementCount;

    public UserShipPlacement() {}

    public UserShipPlacement(int id, int userId, String coordinate, int placementCount) {
        this.id = id;
        this.userId = userId;
        this.coordinate = coordinate;
        this.placementCount = placementCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCoordinate() { return coordinate; }
    public void setCoordinate(String coordinate) { this.coordinate = coordinate; }

    public int getPlacementCount() { return placementCount; }
    public void setPlacementCount(int placementCount) { this.placementCount = placementCount; }
}
