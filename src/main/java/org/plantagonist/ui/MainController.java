package org.plantagonist.ui;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;


public class MainController {
    @FXML private StackPane content;


    @FXML public void initialize() {
        goDashboard();
    }


    public void goDashboard() { setCenter("dashboard.fxml"); }
    public void goPlants() { setCenter("plants.fxml"); }
    public void goCareLog() { setCenter("care_log.fxml"); }
    public void goSupplies() { setCenter("supplies.fxml"); }
    public void goSettings() { setCenter("settings.fxml"); }


    private void setCenter(String fxml) {
        try {
            Node ui = FXMLLoader.load(getClass().getResource("/org/plantagonist/ui/" + fxml));
            content.getChildren().setAll(ui);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}