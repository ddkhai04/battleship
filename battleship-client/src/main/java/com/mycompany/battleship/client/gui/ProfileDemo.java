package com.mycompany.battleship.client.gui;

import javafx.application.Application;

/**
 * Lớp chạy thử riêng màn ProfileGUI (hiện dữ liệu mẫu, chưa cần server).
 * Lớp này KHÔNG extends Application nên chuột phải -> Run File chạy được
 * (chạy thẳng ProfileGUI bằng Run File sẽ báo "JavaFX runtime components are missing").
 * Khi nộp bài có thể xoá file này.
 */
public class ProfileDemo {

    public static void main(String[] args) {
        Application.launch(ProfileGUI.class, args);
    }
}
