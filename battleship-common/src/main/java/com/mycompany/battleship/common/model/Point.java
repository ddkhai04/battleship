package com.mycompany.battleship.common.model;
import java.util.Objects;

/** Toạ độ một ô trên bàn 10x10 (row, col đều bắt đầu từ 0). */
public final class Point {
    private final int row;
    private final int col;

    public Point(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() { return Objects.hash(row, col); }

    @Override
    public String toString() { return "(" + row + "," + col + ")"; }
}
