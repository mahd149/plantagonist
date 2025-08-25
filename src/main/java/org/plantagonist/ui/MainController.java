package org.plantagonist.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import org.plantagonist.core.services.DiagnosticsService;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.storage.PathsConfig;
import com.google.gson.Gson;

import java.nio.file.Files;

public class MainController {
    @FXML private StackPane content;

    @FXML
    public void initialize() {
        // Attach stylesheet once the Scene exists:
        content.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                var url = getClass().getResource("/css/styles.css");
                if (url != null) {
                    var css = url.toExternalForm();
                    if (!newScene.getStylesheets().contains(css)) {
                        newScene.getStylesheets().add(css);
                    }
                } else {
                    System.err.println("⚠ styles.css not found at /css/styles.css");
                }
            }
        });

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

    @FXML
    public void runDiagnostics() {
        String city = loadCityOrDefault();
        Task<String> task = new Task<>() {
            @Override protected String call() {
                return DiagnosticsService.runAll(city);
            }
        };

        task.setOnSucceeded(ev -> showDiagnostics(task.getValue()));
        task.setOnFailed(ev -> {
            Alert a = new Alert(Alert.AlertType.ERROR, "Diagnostics failed: " + task.getException());
            a.setHeaderText("Diagnostics");
            a.showAndWait();
        });

        Thread t = new Thread(task, "diagnostics");
        t.setDaemon(true);
        t.start();
    }

    private void showDiagnostics(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Diagnostics");
        a.setHeaderText("Plantagonist Diagnostics");
        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(760, 420);
        a.getDialogPane().setContent(ta);
        a.showAndWait();
    }

    private String loadCityOrDefault() {
        try {
            var p = PathsConfig.userJson();
            if (Files.exists(p)) {
                var json = Files.readString(p);
                if (!json.isBlank()) {
                    UserProfile u = new Gson().fromJson(json, UserProfile.class);
                    if (u != null && u.getCity() != null && !u.getCity().isBlank()) return u.getCity();
                }
            }
        } catch (Exception ignored) {}
        return "Dhaka";
    }
}
