package com.mycompany.battleship.server.dao;

import com.mycompany.battleship.common.model.UserCombatStats;
import com.mycompany.battleship.server.db.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO cho bảng user_combat_stats: tổng số phát bắn, số phát trúng, win/loss streak.
 * Mỗi người chơi có đúng 1 dòng (khoá chính user_id). Nếu chưa có dòng thì tự tạo.
 */
public class UserCombatStatsDAO {

    // Tạo dòng thống kê rỗng cho người chơi nếu chưa có (INSERT IGNORE: có rồi thì bỏ qua)
    private void taoDongNeuChuaCo(Connection conn, int userId) throws SQLException {
        String sql = "INSERT IGNORE INTO user_combat_stats (user_id) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Gọi sau MỖI phát bắn: total_shots tăng 1, nếu trúng thì hits tăng 1.
     * Đạn đặc biệt cũng chỉ tính là 1 phát bắn.
     */
    public boolean updateShot(int userId, boolean trung) {
        String sql = "UPDATE user_combat_stats SET total_shots = total_shots + 1, hits = hits + ? WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection()) {
            taoDongNeuChuaCo(conn, userId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setQueryTimeout(3);
                ps.setInt(1, trung ? 1 : 0);
                ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gọi khi kết thúc trận cho TỪNG người chơi (thắng hoặc thua).
     * Thắng: win streak hiện tại +1, cập nhật streak cao nhất, loss streak hiện tại về 0.
     * Thua: ngược lại.
     */
    public boolean updateMatchResult(int userId, boolean thang) {
        String sql = "UPDATE user_combat_stats SET current_win_streak = ?, longest_win_streak = ?, "
                + "current_loss_streak = ?, longest_loss_streak = ? WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection()) {
            taoDongNeuChuaCo(conn, userId);
            UserCombatStats s = layThongKe(conn, userId);

            int winHienTai = s.getCurrentWinStreak();
            int winCaoNhat = s.getLongestWinStreak();
            int lossHienTai = s.getCurrentLossStreak();
            int lossCaoNhat = s.getLongestLossStreak();

            if (thang) {
                winHienTai = winHienTai + 1;
                winCaoNhat = Math.max(winCaoNhat, winHienTai);
                lossHienTai = 0;
            } else {
                lossHienTai = lossHienTai + 1;
                lossCaoNhat = Math.max(lossCaoNhat, lossHienTai);
                winHienTai = 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setQueryTimeout(3);
                ps.setInt(1, winHienTai);
                ps.setInt(2, winCaoNhat);
                ps.setInt(3, lossHienTai);
                ps.setInt(4, lossCaoNhat);
                ps.setInt(5, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Lấy thống kê của một người chơi (dùng cho màn Profile). Chưa có dữ liệu thì trả về toàn số 0. */
    public UserCombatStats getStats(int userId) {
        try (Connection conn = DBContext.getConnection()) {
            return layThongKe(conn, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new UserCombatStats(userId, 0, 0, 0, 0, 0, 0, null);
    }

    // Đọc 1 dòng thống kê bằng connection có sẵn
    private UserCombatStats layThongKe(Connection conn, int userId) throws SQLException {
        String sql = "SELECT total_shots, hits, current_win_streak, longest_win_streak, "
                + "current_loss_streak, longest_loss_streak, updated_at FROM user_combat_stats WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(3);
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserCombatStats(userId,
                            rs.getInt("total_shots"),
                            rs.getInt("hits"),
                            rs.getInt("current_win_streak"),
                            rs.getInt("longest_win_streak"),
                            rs.getInt("current_loss_streak"),
                            rs.getInt("longest_loss_streak"),
                            rs.getTimestamp("updated_at"));
                }
            }
        }
        return new UserCombatStats(userId, 0, 0, 0, 0, 0, 0, null);
    }
}
