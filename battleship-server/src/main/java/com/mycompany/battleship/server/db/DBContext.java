/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battleship.server.db;

/**
 *
 * @author dkhai
 */


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBContext {

    private static HikariDataSource dataSource;

    static {
        try (InputStream input = DBContext.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            if (input == null) {
                throw new RuntimeException("Không tìm thấy file db.properties trong resources");
            }
            props.load(input);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("jdbcUrl"));
            config.setUsername(props.getProperty("username"));
            config.setPassword(props.getProperty("password"));

            // Thiết lập các thông số pool
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("maximumPoolSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("minimumIdle", "2")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("idleTimeout", "30000")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("connectionTimeout", "20000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("maxLifetime", "1800000")));

            // Tối ưu hiệu năng truy vấn cho MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Lỗi đọc cấu hình database: " + e.getMessage());
        }
    }

    private DBContext() {
        // Chống khởi tạo instance từ bên ngoài
    }

    /**
     * Mượn một kết nối từ Hikari Pool.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Đóng Connection Pool khi dừng Server.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

