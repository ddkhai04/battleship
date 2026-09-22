package com.mycompany.battleship.server.game;

import java.util.List;
import java.util.Map;

/**
 * Ảnh chụp trạng thái trận đấu DƯỚI GÓC NHÌN của một người chơi (không lộ tàu của đối thủ).
 * Dùng để gửi cho client lúc bắt đầu trận / khi kết nối lại.
 *
 * ownGrid  : '.' nước, 'S' tàu còn nguyên, 'X' tàu đã bị bắn, 'o' đối thủ đã bắn trượt vào đây.
 * enemyGrid: '.' chưa biết, 'X' bắn trúng tàu, 'o' bắn trượt, '?' dấu hỏi chấm (hộp quà).
 */
public final class GameView {
    public final GameRoom.Phase phase;
    public final String myId;
    public final String opponentId;
    public final String currentTurnPlayerId;      // null nếu chưa/đã hết trận
    public final int secondsLeft;                 // giây còn lại của lượt hiện tại
    public final long turnDeadlineMillis;
    public final char[][] ownGrid;
    public final char[][] enemyGrid;
    public final List<List<Point>> sunkEnemyShips;
    public final Map<MissileType, Integer> inventory;

    GameView(GameRoom.Phase phase, String myId, String opponentId, String currentTurnPlayerId,
             int secondsLeft, long turnDeadlineMillis, char[][] ownGrid, char[][] enemyGrid,
             List<List<Point>> sunkEnemyShips, Map<MissileType, Integer> inventory) {
        this.phase = phase;
        this.myId = myId;
        this.opponentId = opponentId;
        this.currentTurnPlayerId = currentTurnPlayerId;
        this.secondsLeft = secondsLeft;
        this.turnDeadlineMillis = turnDeadlineMillis;
        this.ownGrid = ownGrid;
        this.enemyGrid = enemyGrid;
        this.sunkEnemyShips = sunkEnemyShips;
        this.inventory = inventory;
    }

    /** Vẽ 2 bàn cạnh nhau dạng text – tiện debug. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(myId).append(" vs ").append(opponentId)
          .append(" | phase=").append(phase).append(" | turn=").append(currentTurnPlayerId)
          .append(" | inv=").append(inventory).append('\n');
        sb.append("   BAN CUA MINH        BAN DICH\n");
        for (int r = 0; r < Board.SIZE; r++) {
            sb.append(String.format("%2d ", r));
            for (int c = 0; c < Board.SIZE; c++) sb.append(ownGrid[r][c]).append(' ');
            sb.append("  ");
            for (int c = 0; c < Board.SIZE; c++) sb.append(enemyGrid[r][c]).append(' ');
            sb.append('\n');
        }
        return sb.toString();
    }
}
