package com.mycompany.battleship.common.model;

/** Một tin nhắn chat trong phòng (đã được server làm sạch và kiểm tra hợp lệ). */
public final class ChatMessage {
    private final String roomId;
    private final String senderId;
    private final String text;
    private final long timestampMillis;

    public ChatMessage(String roomId, String senderId, String text, long timestampMillis) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.text = text;
        this.timestampMillis = timestampMillis;
    }

    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public long getTimestampMillis() { return timestampMillis; }

    @Override
    public String toString() { return senderId + ": " + text; }
}
