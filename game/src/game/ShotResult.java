package com.mycompany.battleship.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Kết quả của một lượt bắn. Toạ độ trong {@link #getCells()} là toạ độ trên bàn của người BỊ bắn:
 * client người bắn dùng để cập nhật "bàn địch", client người bị bắn dùng để cập nhật "bàn của mình".
 * {@link #getNewMysteryBox()}, {@link #getRewards()} và {@link #getShooterInventory()} chỉ cần gửi cho người bắn.
 */
public final class ShotResult {

    /** Kết quả của từng ô vừa bị đánh dấu (ô đã đánh dấu từ trước không xuất hiện ở đây). */
    public static final class CellResult {
        private final Point point;
        private final boolean hit;
        private final Ship sunkShip;
        private final boolean mysteryCollected;

        CellResult(Point point, boolean hit, Ship sunkShip, boolean mysteryCollected) {
            this.point = point;
            this.hit = hit;
            this.sunkShip = sunkShip;
            this.mysteryCollected = mysteryCollected;
        }

        public Point getPoint() { return point; }
        /** Ô này có tàu không (tính cả ô bị ảnh hưởng bởi vùng nổ). */
        public boolean isHit() { return hit; }
        /** Nếu ô này làm chìm hoàn toàn 1 tàu thì trả về tàu đó, ngược lại null. */
        public Ship getSunkShip() { return sunkShip; }
        public boolean isMysteryCollected() { return mysteryCollected; }
    }

    private final String shooterId;
    private final String targetPlayerId;
    private final MissileType missile;
    private final Point target;               // null với đạn rải rác
    private final List<CellResult> cells = new ArrayList<>();
    private final List<MissileType> rewards = new ArrayList<>();
    private boolean directHit;
    private int rewardsLost;
    private Point newMysteryBox;
    private Map<MissileType, Integer> shooterInventory = new EnumMap<>(MissileType.class);
    private String nextTurnPlayerId;
    private boolean gameOver;

    ShotResult(String shooterId, String targetPlayerId, MissileType missile, Point target) {
        this.shooterId = shooterId;
        this.targetPlayerId = targetPlayerId;
        this.missile = missile;
        this.target = target;
    }

    void addCell(CellResult c) { cells.add(c); }
    void addReward(MissileType t) { rewards.add(t); }
    void incrementRewardsLost() { rewardsLost++; }
    void setDirectHit(boolean v) { directHit = v; }
    void setNewMysteryBox(Point p) { newMysteryBox = p; }
    void setShooterInventory(Map<MissileType, Integer> m) { shooterInventory = m; }
    void setNextTurnPlayerId(String id) { nextTurnPlayerId = id; }
    void setGameOver(boolean v) { gameOver = v; }

    public String getShooterId() { return shooterId; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public MissileType getMissile() { return missile; }
    /** Ô người chơi đã chọn; null nếu là đạn rải rác. */
    public Point getTarget() { return target; }
    public List<CellResult> getCells() { return Collections.unmodifiableList(cells); }

    /** Đúng khi chính ô được chọn có tàu. Chỉ khi đó người bắn mới được bắn tiếp. */
    public boolean isDirectHit() { return directHit; }

    /** Các đạn đặc biệt nhận được từ dấu "?" trong lượt này. */
    public List<MissileType> getRewards() { return Collections.unmodifiableList(rewards); }
    /** Số phần thưởng bị mất vì kho đầy. */
    public int getRewardsLost() { return rewardsLost; }
    /** Ô "?" mới xuất hiện trên bàn địch sau lượt bắn này (null nếu không có). */
    public Point getNewMysteryBox() { return newMysteryBox; }
    /** Kho đạn đặc biệt của người bắn sau lượt này (BIG / NUCLEAR / RAIN). */
    public Map<MissileType, Integer> getShooterInventory() { return shooterInventory; }

    /** Người có lượt kế tiếp; null nếu trận đã kết thúc. */
    public String getNextTurnPlayerId() { return nextTurnPlayerId; }
    public boolean isExtraTurn() { return !gameOver && shooterId.equals(nextTurnPlayerId); }
    public boolean isGameOver() { return gameOver; }

    /** Các tàu vừa bị chìm trong lượt này. */
    public List<Ship> getSunkShips() {
        List<Ship> list = new ArrayList<>();
        for (CellResult c : cells) if (c.sunkShip != null) list.add(c.sunkShip);
        return list;
    }
}
