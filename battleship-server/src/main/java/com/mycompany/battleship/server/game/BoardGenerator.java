package com.mycompany.battleship.server.game;

import com.mycompany.battleship.common.model.Point;
import com.mycompany.battleship.common.model.Ship;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Sinh ngẫu nhiên một bàn hợp lệ: đủ tàu, nằm trong bàn, các tàu cách nhau ít nhất 1 ô trống (kể cả chéo). */
public final class BoardGenerator {
    private static final int MAX_ATTEMPTS_PER_SHIP = 200;

    private BoardGenerator() {}

    public static Board generate(int[] shipLengths, Random rng) {
        int[] lengths = shipLengths.clone();
        Arrays.sort(lengths);                       // đặt tàu dài trước cho dễ xếp
        while (true) {
            Board board = tryGenerate(lengths, rng);
            if (board != null) return board;        // thất bại (rất hiếm) thì làm lại từ đầu
        }
    }

    private static Board tryGenerate(int[] ascLengths, Random rng) {
        Board board = new Board();
        for (int i = ascLengths.length - 1; i >= 0; i--) {
            int len = ascLengths[i];
            boolean placed = false;
            for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_SHIP && !placed; attempt++) {
                boolean horizontal = rng.nextBoolean();
                int r = rng.nextInt(horizontal ? Board.SIZE : Board.SIZE - len + 1);
                int c = rng.nextInt(horizontal ? Board.SIZE - len + 1 : Board.SIZE);
                List<Point> cells = new ArrayList<>(len);
                for (int k = 0; k < len; k++) {
                    cells.add(horizontal ? new Point(r, c + k) : new Point(r + k, c));
                }
                if (board.canPlace(cells)) {
                    board.addShip(new Ship(board.getShips().size(), cells));
                    placed = true;
                }
            }
            if (!placed) return null;
        }
        return board;
    }
}
