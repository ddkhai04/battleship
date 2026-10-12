/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battleship.client.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;

/**
 
 */
public final class DialogStyle {

    private DialogStyle() {
    }

    public static void apply(Alert alert) {
        DialogPane pane = alert.getDialogPane();

        String css = DialogStyle.class.getResource("/game-style.css").toExternalForm();
        if (!pane.getStylesheets().contains(css)) {
            pane.getStylesheets().add(css);
        }
        if (!pane.getStyleClass().contains("lobby-dialog")) {
            pane.getStyleClass().add("lobby-dialog");
        }

        // Bỏ icon mặc định (chữ "i" xanh, dấu chấm than...) cho gọn
        alert.setGraphic(null);

        // Không để chữ bị cắt ở các hộp thoại ngắn
        pane.setMinHeight(Region.USE_PREF_SIZE);
        pane.setMinWidth(340);
    }
}