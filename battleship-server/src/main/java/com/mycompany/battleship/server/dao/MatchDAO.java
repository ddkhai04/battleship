/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battleship.server.dao;

/**
 *
 * @author dkhai
 */
import com.mycompany.battleship.common.model.MatchHistoryDTO;
import com.mycompany.battleship.server.db.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MatchDAO {

    public boolean saveMatchResult(int player1Id, int player2Id, int winnerId, int loserId, int scoreDelta) {
        String sqlInsertMatch = "INSERT INTO matches (player1_id, player2_id, winner_id, loser_id) VALUES (?, ?, ?, ?)";
        String sqlUpdateWinner = "UPDATE users SET score = score + ?, wins = wins + 1 WHERE id = ?";
        String sqlUpdateLoser = "UPDATE users SET score = GREATEST(0, score - ?), losses = losses + 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            // 1. Bắt đầu Transaction
            conn.setAutoCommit(false);

            // Bước 1: Lưu dữ liệu vào bảng matches
            try (PreparedStatement psMatch = conn.prepareStatement(sqlInsertMatch)) {
                psMatch.setInt(1, player1Id);
                psMatch.setInt(2, player2Id);
                psMatch.setInt(3, winnerId);
                psMatch.setInt(4, loserId);
                psMatch.executeUpdate();
            }

            // Bước 2: Cộng điểm và số trận thắng cho Winner
            try (PreparedStatement psWinner = conn.prepareStatement(sqlUpdateWinner)) {
                psWinner.setInt(1, scoreDelta);
                psWinner.setInt(2, winnerId);
                psWinner.executeUpdate();
            }

            // Bước 3: Trừ điểm (không âm) và tăng số trận thua cho Loser
            try (PreparedStatement psLoser = conn.prepareStatement(sqlUpdateLoser)) {
                psLoser.setInt(1, scoreDelta);
                psLoser.setInt(2, loserId);
                psLoser.executeUpdate();
            }

            // 2. Toàn bộ câu lệnh thành công -> Lưu vĩnh viễn
            conn.commit();
            return true;

        } catch (SQLException e) {
            // 3. Có bất kỳ lỗi nào xảy ra -> Hoàn tác toàn bộ
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
            // 4. Khôi phục lại trạng thái ban đầu và trả kết nối về HikariCP
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

    public List<MatchHistoryDTO> getMatchHistory(int userId) {
        List<MatchHistoryDTO> historyList = new ArrayList<>();
        
        // Query join để lấy tên đối thủ và xác định kết quả Thắng/Thua theo góc nhìn của userId
        String sql = "SELECT " +
                     "  m.id AS match_id, " +
                     "  u.nickname AS opponent_nickname, " +
                     "  CASE WHEN m.winner_id = ? THEN 'Thắng' ELSE 'Thua' END AS result, " +
                     "  m.played_at " +
                     "FROM matches m " +
                     "JOIN users u ON u.id = CASE WHEN m.player1_id = ? THEN m.player2_id ELSE m.player1_id END " +
                     "WHERE m.player1_id = ? OR m.player2_id = ? " +
                     "ORDER BY m.played_at DESC";

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
}
