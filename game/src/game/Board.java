package com.mycompany.battleship.server.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bàn 10x10 chứa đội tàu của MỘT người chơi. Người đối diện bắn vào bàn này.
 * Lớp này tự lưu: vị trí tàu, ô đã bị đánh dấu (bắn rồi) và ô dấu hỏi chấm "?".
 */
public final class Board {
    public static final int SIZE = 10;

    private final int[][] shipIndex = new int[SIZE][SIZE];   // -1 = không có tàu
    private final boolean[][] marked = new boolean[SIZE][SIZE];
    private final boolean[][] mystery = new boolean[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();
    private int unrevealedShipCells;

    public Board() {
        for (int[] row : shipIndex) Arrays.fill(row, -1);
    }

    public static boolean inBounds(int r, int c) { return r >= 0 && r < SIZE && c >= 0 && c < SIZE; }
    public static boolean inBounds(Point p) { return inBounds(p.getRow(), p.getCol()); }

    /** Đặt được nếu mọi ô nằm trong bàn và không có tàu nào ở 8 ô xung quanh (cách nhau >= 1 ô trống). */
    boolean canPlace(List<Point> cells) {
        for (Point p : cells) {
            if (!inBounds(p)) return false;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = p.getRow() + dr, c = p.getCol() + dc;
                    if (inBounds(r, c) && shipIndex[r][c] != -1) return false;
                }
            }
        }
        return true;
    }

    /** Id của tàu phải bằng chỉ số của nó trong danh sách (BoardGenerator đảm bảo điều này). */
    void addShip(Ship ship) {
        ships.add(ship);
        for (Point p : ship.getCells()) shipIndex[p.getRow()][p.getCol()] = ship.getId();
        unrevealedShipCells += ship.getLength();
    }

    public List<Ship> getShips() { return Collections.unmodifiableList(ships); }

    public boolean isMarked(Point p) { return marked[p.getRow()][p.getCol()]; }
    public boolean hasShip(Point p) { return shipIndex[p.getRow()][p.getCol()] != -1; }
    public boolean hasMystery(Point p) { return mystery[p.getRow()][p.getCol()]; }

    /** Đánh dấu 1 ô (bắt buộc chưa được đánh dấu). Trả về chuyện gì xảy ra ở ô đó. */
    ShotResult.CellResult mark(Point p) {
        int r = p.getRow(), c = p.getCol();
        marked[r][c] = true;

        boolean hit = shipIndex[r][c] != -1;
        Ship sunk = null;
        if (hit) {
            Ship ship = ships.get(shipIndex[r][c]);
            ship.registerHit();
            unrevealedShipCells--;
            if (ship.isSunk()) sunk = ship;
        }
        boolean box = mystery[r][c];
        mystery[r][c] = false;
        return new ShotResult.CellResult(p, hit, sunk, box);
    }

    /** Mọi ô chưa bị đánh dấu (kể cả ô đang có "?"). */
    List<Point> unmarkedCells() {
        List<Point> list = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (!marked[r][c]) list.add(new Point(r, c));
        return list;
    }

    /** Đặt 1 dấu "?" ở ô chưa đánh dấu và chưa có "?". Trả về null nếu không còn ô trống. */
    Point spawnMystery(Random rng) {
        List<Point> candidates = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (!marked[r][c] && !mystery[r][c]) candidates.add(new Point(r, c));
        if (candidates.isEmpty()) return null;
        Point p = candidates.get(rng.nextInt(candidates.size()));
        mystery[p.getRow()][p.getCol()] = true;
        return p;
    }

    /** Thua khi mọi ô tàu đều đã bị đánh dấu ("bị bắn lộ hết tàu"). */
    public boolean allShipsRevealed() { return unrevealedShipCells == 0; }
}
