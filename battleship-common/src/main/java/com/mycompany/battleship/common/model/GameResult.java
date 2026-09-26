package com.mycompany.battleship.common.model;

/** Kết quả cuối trận – Thành viên 1 dùng để ghi Matches và cập nhật điểm (+3 / -1). */
public final class GameResult {
    public enum Reason { ALL_SHIPS_SUNK, TIMEOUT, LEFT }

    public static final int WIN_POINTS = 3;
    public static final int LOSE_POINTS = -1;

    private final String roomId;
    private final String winnerId;
    private final String loserId;
    private final Reason reason;
    private final long startedAtMillis;
    private final long endedAtMillis;

    public GameResult(String roomId, String winnerId, String loserId, Reason reason, long startedAtMillis, long endedAtMillis) {
        this.roomId = roomId;
        this.winnerId = winnerId;
        this.loserId = loserId;
        this.reason = reason;
        this.startedAtMillis = startedAtMillis;
        this.endedAtMillis = endedAtMillis;
    }

    public String getRoomId() { return roomId; }
    public String getWinnerId() { return winnerId; }
    public String getLoserId() { return loserId; }
    public Reason getReason() { return reason; }
    public long getStartedAtMillis() { return startedAtMillis; }
    public long getEndedAtMillis() { return endedAtMillis; }

    @Override
    public String toString() {
        return "GameResult{winner=" + winnerId + ", loser=" + loserId + ", reason=" + reason + "}";
    }
}
