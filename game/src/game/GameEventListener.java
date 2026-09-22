package com.mycompany.battleship.server.game;

/**
 * Cầu nối sang tầng mạng (Thành viên 2). GameRoom gọi các hàm này khi có sự kiện; Thành viên 2
 * chỉ cần cài đặt và gửi message tương ứng tới 2 client.
 *
 * LƯU Ý: các hàm được gọi khi GameRoom đang giữ khoá, nên chỉ được đẩy message vào hàng đợi gửi
 * (không chặn, không gọi ngược vào GameRoom từ luồng khác chờ kết quả).
 */
public interface GameEventListener {
    /** Bàn đã được sinh xong: gửi viewFor(mỗi người) để hiện màn hình trận đấu + đếm ngược. */
    default void onMatchPrepared(GameRoom room) {}

    /** Đếm ngược 3-2-1 trước khi bắt đầu. */
    default void onCountdown(GameRoom room, int secondsLeft) {}

    /** Bắt đầu lượt của playerId (kể cả lượt thưởng khi bắn trúng). Client reset thanh 45s. */
    default void onTurnStarted(GameRoom room, String playerId, int turnSeconds, long deadlineMillis) {}

    /** Một lượt bắn đã được xử lý. Luôn đến trước onTurnStarted / onGameOver của lượt đó. */
    default void onShotResolved(GameRoom room, ShotResult result) {}

    /** Trận kết thúc (hết tàu / hết giờ / bỏ trận). */
    default void onGameOver(GameRoom room, GameResult result) {}

    /** Đối thủ bấm "Tái đấu". */
    default void onRematchRequested(GameRoom room, String fromPlayerId) {}

    /** Một người rời phòng (về sảnh hoặc mất kết nối). */
    default void onPlayerLeft(GameRoom room, String playerId) {}

    /** Có tin nhắn chat mới. Gửi cho CẢ HAI người (kể cả người gửi để xác nhận tin đã lên). */
    default void onChatMessage(GameRoom room, ChatMessage message) {}

    /** Có emote mới. Gửi cho cả hai người. */
    default void onEmote(GameRoom room, EmoteEvent event) {}

    /** Phòng đã đóng; trả trạng thái 2 người về ONLINE nếu họ còn kết nối. */
    default void onRoomClosed(GameRoom room) {}
}
