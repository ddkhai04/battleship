package com.mycompany.battleship.server.game;

/**
 * Danh sách emote cố định (whitelist). Client chỉ được gửi 1 trong các mã này, server không nhận emote tuỳ ý
 * nên không thể bị lợi dụng để gửi nội dung xấu. Ký tự emoji dùng escape \\u để không phụ thuộc encoding của file nguồn.
 */
public enum Emote {
    GG("\uD83E\uDD1D", "GG"),
    LAUGH("\uD83D\uDE02", "Haha"),
    WOW("\uD83D\uDE2E", "Wow"),
    CRY("\uD83D\uDE2D", "Huhu"),
    ANGRY("\uD83D\uDE21", "Tức quá"),
    THUMBS_UP("\uD83D\uDC4D", "Hay đấy"),
    CLAP("\uD83D\uDC4F", "Vỗ tay"),
    THINKING("\uD83E\uDD14", "Hmm..."),
    FIRE("\uD83D\uDD25", "Cháy quá"),
    HURRY("\u23F0", "Nhanh lên nào");

    private final String emoji;
    private final String label;

    Emote(String emoji, String label) {
        this.emoji = emoji;
        this.label = label;
    }

    /** Ký tự emoji để hiển thị (client có thể dùng ảnh riêng và chỉ cần tên enum). */
    public String emoji() { return emoji; }

    /** Nhãn tiếng Việt ngắn để hiện dưới emoji / làm tooltip. */
    public String label() { return label; }
}
