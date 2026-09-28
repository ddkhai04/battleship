package com.mycompany.battleship.common.model;

/** Lỗi nước đi / trạng thái không hợp lệ. Tầng mạng bắt lại và gửi mã lỗi về cho client. */
public class GameException extends RuntimeException {
    public enum Code {
        ROOM_CLOSED,          // phòng đã đóng
        INVALID_STATE,        // hành động không hợp lệ ở giai đoạn hiện tại
        NOT_IN_ROOM,          // người chơi không thuộc phòng này
        NOT_YOUR_TURN,        // chưa tới lượt
        INVALID_MISSILE,      // loại đạn null
        MISSILE_UNAVAILABLE,  // không còn loại đạn này trong kho
        OUT_OF_BOUNDS,        // toạ độ ngoài bàn 10x10 / thiếu toạ độ
        CELL_ALREADY_MARKED,  // ô đã bắn rồi
        CHAT_EMPTY,           // tin nhắn trống sau khi làm sạch
        CHAT_TOO_LONG,        // tin nhắn quá dài
        INVALID_EMOTE,        // emote null / không có trong danh sách
        RATE_LIMITED          // gửi chat/emote quá nhanh (xem getRetryAfterMillis)
    }

    private static final long serialVersionUID = 1L;

    private final Code code;
    private final long retryAfterMillis;

    public GameException(Code code, String message) {
        this(code, message, 0);
    }

    public GameException(Code code, String message, long retryAfterMillis) {
        super(message);
        this.code = code;
        this.retryAfterMillis = retryAfterMillis;
    }

    public Code getCode() { return code; }

    /** Với RATE_LIMITED: bao lâu nữa được gửi lại (để client hiển thị/khoá nút); các lỗi khác trả 0. */
    public long getRetryAfterMillis() { return retryAfterMillis; }
}
