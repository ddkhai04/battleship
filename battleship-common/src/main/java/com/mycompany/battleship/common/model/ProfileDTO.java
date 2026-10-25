package com.mycompany.battleship.common.model;

import java.io.Serializable;
import java.util.List;

/**
 * Gói dữ liệu hồ sơ 1 người chơi, server gửi cho client trong lệnh PROFILE_DATA:
 * thống kê bắn/streak, và dữ liệu 2 heatmap cá nhân.
 */
public class ProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public int userId;
    public UserCombatStats thongKe;
    public List<UserShipPlacement> datTau;
    public List<UserShotTarget> oBan;

    public ProfileDTO() {}
}