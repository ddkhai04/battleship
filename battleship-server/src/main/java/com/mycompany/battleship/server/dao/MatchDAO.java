package com.mycompany.battleship.server.dao;

import com.mycompany.battleship.common.model.MatchHistoryDTO;
import com.mycompany.battleship.server.db.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class MatchDAO {

    /**
     * Tạo một trận đấu mới ở trạng thái ONGOING và trả về match_id vừa tạo
     */
    public int createMatch(int player1Id, int player2Id) {
        String sql = "INSERT INTO matches (player1_id, player2_id, status) VALUES (?, ?, 'ONGOING')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, player1Id);
            ps.setInt(2, player2Id);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Lưu kết quả trận đấu trọn gói (dành cho luồng lưu nhanh cả trận một lần)
     */
    public boolean saveMatchResult(int player1Id, int player2Id, int winnerId, int loserId, int scoreDelta) {
        return saveMatchResult(player1Id, player2Id, winnerId, loserId, scoreDelta, 0);
    }

    public boolean saveMatchResult(int player1Id, int player2Id, int winnerId, int loserId, int scoreDelta, int durationSeconds) {
        String sqlInsertMatch = "INSERT INTO matches (player1_id, player2_id, winner_id, loser_id, status, duration_seconds) "
                + "VALUES (?, ?, ?, ?, 'FINISHED', ?)";
        String sqlUpdateWinner = "UPDATE users SET score = score + ?, wins = wins + 1 WHERE id = ?";
        String sqlUpdateLoser = "UPDATE users SET score = GREATEST(0, score - ?), losses = losses + 1 WHERE id = ?";

        String sqlWinnerStreak = "INSERT INTO user_combat_stats (user_id, current_win_streak, longest_win_streak, current_loss_streak, longest_loss_streak) "
                + "VALUES (?, 1, 1, 0, 0) "
                + "ON DUPLICATE KEY UPDATE "
                + "current_win_streak = current_win_streak + 1, "
                + "longest_win_streak = GREATEST(longest_win_streak, current_win_streak), "
                + "current_loss_streak = 0";

        String sqlLoserStreak = "INSERT INTO user_combat_stats (user_id, current_win_streak, longest_win_streak, current_loss_streak, longest_loss_streak) "
                + "VALUES (?, 0, 0, 1, 1) "
                + "ON DUPLICATE KEY UPDATE "
                + "current_loss_streak = current_loss_streak + 1, "
                + "longest_loss_streak = GREATEST(longest_loss_streak, current_loss_streak), "
                + "current_win_streak = 0";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // 1. Lưu bản ghi matches
            try (PreparedStatement psMatch = conn.prepareStatement(sqlInsertMatch)) {
                psMatch.setInt(1, player1Id);
                psMatch.setInt(2, player2Id);
                psMatch.setInt(3, winnerId);
                psMatch.setInt(4, loserId);
                psMatch.setInt(5, durationSeconds);
                psMatch.executeUpdate();
            }

            // 2. Cập nhật bảng users (Winner)
            try (PreparedStatement psWinner = conn.prepareStatement(sqlUpdateWinner)) {
                psWinner.setInt(1, scoreDelta);
                psWinner.setInt(2, winnerId);
                psWinner.executeUpdate();
            }

            // 3. Cập nhật bảng users (Loser)
            try (PreparedStatement psLoser = conn.prepareStatement(sqlUpdateLoser)) {
                psLoser.setInt(1, scoreDelta);
                psLoser.setInt(2, loserId);
                psLoser.executeUpdate();
            }

            // 4. Cập nhật streak cho Winner
            try (PreparedStatement psWinStreak = conn.prepareStatement(sqlWinnerStreak)) {
                psWinStreak.setInt(1, winnerId);
                psWinStreak.executeUpdate();
            }

            // 5. Cập nhật streak cho Loser
            try (PreparedStatement psLoseStreak = conn.prepareStatement(sqlLoserStreak)) {
                psLoseStreak.setInt(1, loserId);
                psLoseStreak.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Kết thúc một trận đấu đã được tạo trước đó qua createMatch
     */
    public boolean finishMatch(int matchId, int winnerId, int loserId, int scoreDelta, int durationSeconds) {
        String sqlUpdateMatch = "UPDATE matches SET winner_id = ?, loser_id = ?, status = 'FINISHED', duration_seconds = ? WHERE id = ?";
        String sqlUpdateWinner = "UPDATE users SET score = score + ?, wins = wins + 1 WHERE id = ?";
        String sqlUpdateLoser = "UPDATE users SET score = GREATEST(0, score - ?), losses = losses + 1 WHERE id = ?";

        String sqlWinnerStreak = "INSERT INTO user_combat_stats (user_id, current_win_streak, longest_win_streak, current_loss_streak, longest_loss_streak) "
                + "VALUES (?, 1, 1, 0, 0) "
                + "ON DUPLICATE KEY UPDATE "
                + "current_win_streak = current_win_streak + 1, "
                + "longest_win_streak = GREATEST(longest_win_streak, current_win_streak), "
                + "current_loss_streak = 0";

        String sqlLoserStreak = "INSERT INTO user_combat_stats (user_id, current_win_streak, longest_win_streak, current_loss_streak, longest_loss_streak) "
                + "VALUES (?, 0, 0, 1, 1) "
                + "ON DUPLICATE KEY UPDATE "
                + "current_loss_streak = current_loss_streak + 1, "
                + "longest_loss_streak = GREATEST(longest_loss_streak, current_loss_streak), "
                + "current_win_streak = 0";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psMatch = conn.prepareStatement(sqlUpdateMatch)) {
                psMatch.setInt(1, winnerId);
                psMatch.setInt(2, loserId);
                psMatch.setInt(3, durationSeconds);
                psMatch.setInt(4, matchId);
                psMatch.executeUpdate();
            }

            try (PreparedStatement psWinner = conn.prepareStatement(sqlUpdateWinner)) {
                psWinner.setInt(1, scoreDelta);
                psWinner.setInt(2, winnerId);
                psWinner.executeUpdate();
            }

            try (PreparedStatement psLoser = conn.prepareStatement(sqlUpdateLoser)) {
                psLoser.setInt(1, scoreDelta);
                psLoser.setInt(2, loserId);
                psLoser.executeUpdate();
            }

            try (PreparedStatement psWinStreak = conn.prepareStatement(sqlWinnerStreak)) {
                psWinStreak.setInt(1, winnerId);
                psWinStreak.executeUpdate();
            }

            try (PreparedStatement psLoseStreak = conn.prepareStatement(sqlLoserStreak)) {
                psLoseStreak.setInt(1, loserId);
                psLoseStreak.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Hủy trận đấu (người chơi thoát giữa trận hoặc sự cố mạng)
     */
    public boolean abortMatch(int matchId, int durationSeconds) {
        String sql = "UPDATE matches SET status = 'ABORTED', duration_seconds = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, durationSeconds);
            ps.setInt(2, matchId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu thông tin xếp tàu vào bảng match_boards
     */
    public boolean saveMatchBoard(int matchId, int playerId, String shipDataJson) {
        String sql = "INSERT INTO match_boards (match_id, player_id, ship_data) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE ship_data = VALUES(ship_data)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, playerId);
            ps.setString(3, shipDataJson);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu từng lượt bắn vào bảng match_turns
     * result: 'MISS', 'HIT', 'SUNK'
     */
    public boolean saveMatchTurn(int matchId, int turnNumber, int playerId, String coordinate, String result, String sunkShipType) {
        String sql = "INSERT INTO match_turns (match_id, turn_number, player_id, coordinate, result, sunk_ship_type) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, turnNumber);
            ps.setInt(3, playerId);
            ps.setString(4, coordinate);
            ps.setString(5, result);
            if (sunkShipType != null && !sunkShipType.isEmpty()) {
                ps.setString(6, sunkShipType);
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật tần suất bắn và độ chính xác của người chơi vào user_shot_targets & user_combat_stats
     */
    public void recordShotStats(int userId, String coordinate, boolean isHit) {
        int hitInc = isHit ? 1 : 0;
        String sqlTarget = "INSERT INTO user_shot_targets (user_id, coordinate, shot_count, hit_count) "
                + "VALUES (?, ?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE shot_count = shot_count + 1, hit_count = hit_count + ?";
        String sqlCombat = "INSERT INTO user_combat_stats (user_id, total_shots, hits) "
                + "VALUES (?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE total_shots = total_shots + 1, hits = hits + ?";

        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement psTarget = conn.prepareStatement(sqlTarget)) {
                psTarget.setInt(1, userId);
                psTarget.setString(2, coordinate);
                psTarget.setInt(3, hitInc);
                psTarget.setInt(4, hitInc);
                psTarget.executeUpdate();
            }

            try (PreparedStatement psCombat = conn.prepareStatement(sqlCombat)) {
                psCombat.setInt(1, userId);
                psCombat.setInt(2, hitInc);
                psCombat.setInt(3, hitInc);
                psCombat.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy lịch sử trận đấu của user
     */
    public List<MatchHistoryDTO> getMatchHistory(int userId) {
        List<MatchHistoryDTO> historyList = new ArrayList<>();

        String sql = "SELECT "
                + "  m.id AS match_id, "
                + "  u.nickname AS opponent_nickname, "
                + "  CASE "
                + "    WHEN m.winner_id = ? THEN 'Thắng' "
                + "    WHEN m.status = 'ABORTED' THEN 'Hủy' "
                + "    ELSE 'Thua' "
                + "  END AS result, "
                + "  m.played_at "
                + "FROM matches m "
                + "JOIN users u ON u.id = CASE WHEN m.player1_id = ? THEN m.player2_id ELSE m.player1_id END "
                + "WHERE (m.player1_id = ? OR m.player2_id = ?) "
                + "  AND m.status IN ('FINISHED', 'ABORTED') "
                + "ORDER BY m.played_at DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatchHistoryDTO dto = new MatchHistoryDTO(
                            rs.getInt("match_id"),
                            rs.getString("opponent_nickname"),
                            rs.getString("result"),
                            rs.getTimestamp("played_at")
                    );
                    historyList.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }
    
    /**
     * Ghi nhận các ô đặt tàu của người chơi vào bảng user_ship_placements
     */
    public void recordShipPlacements(int userId, List<String> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) return;
        String sql = "INSERT INTO user_ship_placements (user_id, coordinate, placement_count) "
                + "VALUES (?, ?, 1) "
                + "ON DUPLICATE KEY UPDATE placement_count = placement_count + 1";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String coord : coordinates) {
                ps.setInt(1, userId);
                ps.setString(2, coord);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy tần suất đặt tàu theo từng ô (userId <= 0: toàn server; userId > 0: cá nhân)
     */
    public java.util.Map<String, Integer> getShipPlacementHeatmap(int userId) {
        java.util.Map<String, Integer> heatmap = new java.util.HashMap<>();
        String sql = (userId > 0)
                ? "SELECT coordinate, placement_count AS total FROM user_ship_placements WHERE user_id = ?"
                : "SELECT coordinate, SUM(placement_count) AS total FROM user_ship_placements GROUP BY coordinate";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId > 0) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    heatmap.put(rs.getString("coordinate"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return heatmap;
    }

    /**
     * Lấy tần suất các ô bị bắn (userId <= 0: toàn server; userId > 0: cá nhân)
     */
    public java.util.Map<String, Integer> getShotTargetHeatmap(int userId) {
        java.util.Map<String, Integer> heatmap = new java.util.HashMap<>();
        String sql = (userId > 0)
                ? "SELECT coordinate, shot_count AS total FROM user_shot_targets WHERE user_id = ?"
                : "SELECT coordinate, SUM(shot_count) AS total FROM user_shot_targets GROUP BY coordinate";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId > 0) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    heatmap.put(rs.getString("coordinate"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return heatmap;
    }
}