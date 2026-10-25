package com.mycompany.battleship.server.dao;

import com.mycompany.battleship.common.model.UserShipPlacement;
import com.mycompany.battleship.common.model.UserShotTarget;
import com.mycompany.battleship.server.db.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho 2 bảng dùng vẽ heatmap:
 *  - user_ship_placements: ô nào người chơi hay đặt tàu (placement_count)
 *  - user_shot_targets: ô nào người chơi hay nhắm bắn (shot_count) và bắn trúng bao nhiêu lần (hit_count)
 * Tọa độ lưu dạng chuỗi A1..J10 (cột là chữ A-J, hàng là số 1-10). Dùng hàm toaDo(row, col) để tạo.
 */
public class UserHeatmapDAO {

    /**
     * Tạo chuỗi tọa độ lưu vào DB theo kiểu A1..J10: cột là chữ cái (A-J), hàng là số (1-10).
     * row, col truyền vào bắt đầu từ 0. Ví dụ toaDo(0, 0) -> "A1", toaDo(9, 9) -> "J10", toaDo(2, 4) -> "E3".
     */
    public static String toaDo(int row, int col) {
        return "" + (char) ('A' + col) + (row + 1);
    }

    /**
     * Gọi lúc bắt đầu mỗi ván: ghi các ô người chơi đặt tàu.
     * Ô chưa có thì thêm mới (placement_count = 1), đã có thì tăng placement_count lên 1.
     */
    public boolean ghiNhanDatTau(int userId, List<String> danhSachToaDo) {
        String sql = "INSERT INTO user_ship_placements (user_id, coordinate, placement_count) VALUES (?, ?, 1) "
                + "ON DUPLICATE KEY UPDATE placement_count = placement_count + 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(3);
            for (String toaDo : danhSachToaDo) {
                ps.setInt(1, userId);
                ps.setString(2, toaDo);
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gọi sau mỗi phát bắn có ô được chọn: ghi ô trung tâm người chơi nhắm vào.
     * shot_count tăng 1; nếu trúng thì hit_count cũng tăng 1.
     * Đạn rải rác (không chọn ô) thì không gọi hàm này.
     */
    public boolean ghiNhanBan(int userId, String toaDo, boolean trung) {
        String sql = "INSERT INTO user_shot_targets (user_id, coordinate, shot_count, hit_count) VALUES (?, ?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE shot_count = shot_count + 1, hit_count = hit_count + ?";
        int diemTrung = trung ? 1 : 0;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(3);
            ps.setInt(1, userId);
            ps.setString(2, toaDo);
            ps.setInt(3, diemTrung);
            ps.setInt(4, diemTrung);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Lấy dữ liệu heatmap đặt tàu của một người chơi (mỗi phần tử = 1 ô đã từng đặt tàu). */
    public List<UserShipPlacement> layDatTau(int userId) {
        List<UserShipPlacement> ds = new ArrayList<>();
        String sql = "SELECT id, coordinate, placement_count FROM user_ship_placements WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(3);
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(new UserShipPlacement(rs.getInt("id"), userId,
                            rs.getString("coordinate"), rs.getInt("placement_count")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    /** Lấy dữ liệu heatmap nhắm bắn của một người chơi (mỗi phần tử = 1 ô: số lần bắn, số lần trúng). */
    public List<UserShotTarget> layOBan(int userId) {
        List<UserShotTarget> ds = new ArrayList<>();
        String sql = "SELECT id, coordinate, shot_count, hit_count FROM user_shot_targets WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(3);
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(new UserShotTarget(rs.getInt("id"), userId,
                            rs.getString("coordinate"), rs.getInt("shot_count"), rs.getInt("hit_count")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }
}
