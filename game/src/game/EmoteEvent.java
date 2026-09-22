package com.mycompany.battleship.server.game;

/** Một lần người chơi gửi emote. Emote không lưu vào lịch sử chat. */
public final class EmoteEvent {
    private final String roomId;
    private final String senderId;
    private final Emote emote;
    private final long timestampMillis;

    EmoteEvent(String roomId, String senderId, Emote emote, long timestampMillis) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.emote = emote;
        this.timestampMillis = timestampMillis;
    }

    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
    public Emote getEmote() { return emote; }
    public long getTimestampMillis() { return timestampMillis; }
}
