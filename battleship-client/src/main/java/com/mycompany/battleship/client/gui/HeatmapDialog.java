/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battleship.client.gui;

/**
 *
 * @author dkhai
 */
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class HeatmapDialog {
    public static void hienThi(JsonObject data) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Biểu Đồ Nhiệt Thống Kê (Heatmap)");

        Map<String, Integer> placementMap = parseMap(data.getAsJsonObject("placementHeatmap"));
        Map<String, Integer> shotMap = parseMap(data.getAsJsonObject("shotHeatmap"));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabPlacement = new Tab("Vị Trí Đặt Tàu", taoBangHeatmap(placementMap, "PLACEMENT"));
        Tab tabShot = new Tab("Vị Trí Bắn", taoBangHeatmap(shotMap, "SHOT"));

        tabPane.getTabs().addAll(tabPlacement, tabShot);

        VBox root = new VBox(10, tabPane);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 520, 560);
        stage.setScene(scene);
        stage.show();
    }

    private static Map<String, Integer> parseMap(JsonObject json) {
        Map<String, Integer> map = new HashMap<>();
        if (json != null) {
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        return map;
    }

    private static VBox taoBangHeatmap(Map<String, Integer> data, String type) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);

        int maxVal = data.values().stream().max(Integer::compareTo).orElse(1);
        if (maxVal == 0) maxVal = 1;

        // Tiêu đề cột (1-10)
        for (int c = 1; c <= 10; c++) {
            Label colLabel = new Label(String.valueOf(c));
            colLabel.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold;");
            grid.add(colLabel, c, 0);
        }

        // Tiêu đề hàng (A-J) và các ô nhiệt
        for (int r = 0; r < 10; r++) {
            char rowChar = (char) ('A' + r);
            Label rowLabel = new Label(String.valueOf(rowChar));
            rowLabel.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold;");
            grid.add(rowLabel, 0, r + 1);

            for (int c = 1; c <= 10; c++) {
                String coord = "" + rowChar + c;
                int count = data.getOrDefault(coord, 0);
                double ratio = (double) count / maxVal;

                StackPane cell = new StackPane();
                cell.setPrefSize(42, 42);

                String colorStyle;
                if (count == 0) {
                    colorStyle = "-fx-background-color: rgba(255, 255, 255, 0.05);";
                } else if ("PLACEMENT".equals(type)) {
                    // Màu xanh lam -> xanh ngọc -> tím đậm cho vị trí đặt tàu
                    colorStyle = String.format("-fx-background-color: rgba(0, 180, 216, %.2f);", Math.max(0.25, ratio));
                } else {
                    // Màu vàng cam -> đỏ rực cho vị trí bắn
                    colorStyle = String.format("-fx-background-color: rgba(233, 69, 96, %.2f);", Math.max(0.25, ratio));
                }

                cell.setStyle(colorStyle + "-fx-border-color: #0f3460; -fx-border-width: 1px; -fx-background-radius: 4px;");

                Label countLabel = new Label(count > 0 ? String.valueOf(count) : "");
                countLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                cell.getChildren().add(countLabel);

                Tooltip.install(cell, new Tooltip("Tọa độ: " + coord + "\nSố lần: " + count));
                grid.add(cell, c, r + 1);
            }
        }

        VBox box = new VBox(15, grid);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        return box;
    }
}
