package com.mycompany.battleship.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Một con tàu 1 x length nằm ngang hoặc dọc. */
public final class Ship {
    private final int id;
    private final List<Point> cells;
    private int hits;

    Ship(int id, List<Point> cells) {
        this.id = id;
        this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
    }

    public int getId() { return id; }
    public int getLength() { return cells.size(); }
    public List<Point> getCells() { return cells; }
    public boolean isSunk() { return hits >= cells.size(); }

    void registerHit() { hits++; }
}
