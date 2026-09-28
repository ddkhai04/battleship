package com.mycompany.battleship.common.model;

/** 4 loại đạn. Mỗi loại có "hình dạng" là các độ lệch (dRow, dCol) so với ô được chọn. */
public enum MissileType {
    /** Đạn thường: 1 ô. */
    SIMPLE,
    /** Đạn chữ thập: 5 ô (tâm + 4 hướng). */
    BIG,
    /** Đạn nguyên tử: 13 ô, hình thoi đường chéo 5x5. */
    NUCLEAR,
    /** Đạn rải rác: 4-7 ô ngẫu nhiên chưa bị đánh dấu, không cần chọn ô. */
    RAIN;

    private static final int[][] SIMPLE_SHAPE = {{0, 0}};
    private static final int[][] BIG_SHAPE = {{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] NUCLEAR_SHAPE = diamond(2);

    private static int[][] diamond(int radius) {
        java.util.List<int[]> list = new java.util.ArrayList<>();
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                if (Math.abs(dr) + Math.abs(dc) <= radius) list.add(new int[]{dr, dc});
            }
        }
        return list.toArray(new int[0][]);
    }

    /** Đạn đặc biệt = mọi loại trừ đạn thường. */
    public boolean isSpecial() { return this != SIMPLE; }

    /** Người chơi có phải chọn ô ngắm không? (Rải rác thì không). */
    public boolean needsTarget() { return this != RAIN; }

    /** Hình dạng vùng nổ; rỗng với RAIN vì vùng nổ là ngẫu nhiên. */
    public int[][] shape() {
        switch (this) {
            case SIMPLE:  return SIMPLE_SHAPE;
            case BIG:     return BIG_SHAPE;
            case NUCLEAR: return NUCLEAR_SHAPE;
            default:      return new int[0][];
        }
    }
}
