package com.mycompany.battleship.server.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lịch sử chat của một phòng + kiểm tra nội dung + chống spam. Không tự khoá: luôn được GameRoom gọi
 * trong đoạn synchronized. Lịch sử giữ nguyên qua các ván tái đấu.
 */
final class ChatLog {
    private final GameConfig config;
    private final Deque<ChatMessage> history = new ArrayDeque<>();
    private final Map<String, Deque<Long>> messageTimes = new HashMap<>();   // mốc thời gian (đơn điệu) mỗi người
    private final Map<String, Long> lastEmoteAt = new HashMap<>();

    ChatLog(GameConfig config) { this.config = config; }

    /** Đồng hồ đơn điệu, không bị ảnh hưởng khi hệ thống chỉnh lại giờ. */
    private static long nowMono() { return System.nanoTime() / 1_000_000L; }

    ChatMessage addMessage(String roomId, String senderId, String rawText) {
        // Chặn sớm chuỗi khổng lồ trước khi làm sạch (1 ký tự có thể chiếm 2 char).
        if (rawText != null && rawText.length() > config.getChatMaxLength() * 4) {
            throw new GameException(GameException.Code.CHAT_TOO_LONG,
                    "Tin nhắn tối đa " + config.getChatMaxLength() + " ký tự");
        }
        String text = sanitize(rawText);
        if (text.isEmpty()) {
            throw new GameException(GameException.Code.CHAT_EMPTY, "Tin nhắn trống");
        }
        if (text.codePointCount(0, text.length()) > config.getChatMaxLength()) {
            throw new GameException(GameException.Code.CHAT_TOO_LONG,
                    "Tin nhắn tối đa " + config.getChatMaxLength() + " ký tự");
        }

        // Chống spam: tối đa N tin trong một cửa sổ trượt. Tin bị từ chối không được tính vào hạn mức.
        long now = nowMono();
        Deque<Long> times = messageTimes.computeIfAbsent(senderId, k -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() >= config.getChatWindowMillis()) times.pollFirst();
        if (times.size() >= config.getChatMaxMessagesPerWindow()) {
            long retry = config.getChatWindowMillis() - (now - times.peekFirst());
            throw new GameException(GameException.Code.RATE_LIMITED, "Bạn gửi quá nhanh, thử lại sau " + retry + "ms", retry);
        }
        times.addLast(now);

        ChatMessage msg = new ChatMessage(roomId, senderId, text, System.currentTimeMillis());
        history.addLast(msg);
        while (history.size() > config.getChatHistorySize()) history.pollFirst();
        return msg;
    }

    EmoteEvent addEmote(String roomId, String senderId, Emote emote) {
        if (emote == null) {
            throw new GameException(GameException.Code.INVALID_EMOTE, "Emote không hợp lệ");
        }
        long now = nowMono();
        Long last = lastEmoteAt.get(senderId);
        if (last != null && now - last < config.getEmoteCooldownMillis()) {
            long retry = config.getEmoteCooldownMillis() - (now - last);
            throw new GameException(GameException.Code.RATE_LIMITED, "Emote đang hồi, thử lại sau " + retry + "ms", retry);
        }
        lastEmoteAt.put(senderId, now);
        return new EmoteEvent(roomId, senderId, emote, System.currentTimeMillis());
    }

    List<ChatMessage> history() { return new ArrayList<>(history); }

    /**
     * Làm sạch: bỏ ký tự điều khiển, gộp mọi chuỗi khoảng trắng/xuống dòng thành 1 dấu cách, cắt hai đầu.
     * Chat chỉ có 1 dòng nên không thể "vẽ" nhiều dòng để lấn át khung chat của đối thủ.
     */
    static String sanitize(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        boolean pendingSpace = false;
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp) || Character.isISOControl(cp)) {
                pendingSpace = sb.length() > 0;
            } else {
                if (pendingSpace) {
                    sb.append(' ');
                    pendingSpace = false;
                }
                sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }
}
