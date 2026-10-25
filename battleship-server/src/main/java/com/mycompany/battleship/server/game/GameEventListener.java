package com.mycompany.battleship.server.game;

import com.mycompany.battleship.common.model.ChatMessage;
import com.mycompany.battleship.common.model.EmoteEvent;
import com.mycompany.battleship.common.model.GameResult;
import com.mycompany.battleship.common.model.ShotResult;

public interface GameEventListener {
    // Bắt đầu 45s đặt tàu: thông báo 2 client mở giao diện đặt tàu. 
    default void onSetupStarted(GameRoom room, int setupSeconds, long deadlineMillis) {}

    // Một người chơi đã bấm Sẵn sàng đặt tàu.
    default void onPlayerReady(GameRoom room, String playerId) {}

    // Bàn đã được chốt xong: lưu DB và gửi viewFor(mỗi người) để đếm ngược 3-2-1.
    default void onMatchPrepared(GameRoom room) {}

    // Đếm ngược 3-2-1 trước khi bắt đầu. */
    default void onCountdown(GameRoom room, int secondsLeft) {}

    // Bắt đầu lượt của playerId (kể cả lượt thưởng khi bắn trúng). Client reset thanh 45s. 
    default void onTurnStarted(GameRoom room, String playerId, int turnSeconds, long deadlineMillis) {}

    // Một lượt bắn đã được xử lý. Luôn đến trước onTurnStarted / onGameOver của lượt đó. 
    default void onShotResolved(GameRoom room, ShotResult result) {}

    // Trận kết thúc (hết tàu / hết giờ / bỏ trận). 
    default void onGameOver(GameRoom room, GameResult result) {}

    // Đối thủ bấm "Tái đấu". 
    default void onRematchRequested(GameRoom room, String fromPlayerId) {}

    // Một người rời phòng (về sảnh hoặc mất kết nối). 
    default void onPlayerLeft(GameRoom room, String playerId) {}

    // Có tin nhắn chat mới. 
    default void onChatMessage(GameRoom room, ChatMessage message) {}

    // Có emote mới. 
    default void onEmote(GameRoom room, EmoteEvent event) {}

    // Phòng đã đóng. 
    default void onRoomClosed(GameRoom room) {}
}