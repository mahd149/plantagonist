package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.plantagonist.core.models.CareTask;
import org.plantagonist.core.models.Plant;
import org.plantagonist.core.repositories.CareTaskRepository;
import org.plantagonist.core.repositories.PlantRepository;
import org.plantagonist.core.services.SuggestionService;
import org.plantagonist.core.services.TaskService;
import org.plantagonist.core.services.WeatherService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label weatherAdvice;
    @FXML private ListView<CareTask> taskList;
    @FXML private ListView<String> streakList;

    private final CareTaskRepository taskRepo = new CareTaskRepository();
    private final PlantRepository plantRepo = new PlantRepository();
    private final TaskService taskService =
            new TaskService(plantRepo, taskRepo, new WeatherService(), new SuggestionService());

    @FXML
    public void initialize() {
        if (taskList != null) taskList.setPlaceholder(new Label("No tasks due today ✨"));
        if (streakList != null) streakList.setPlaceholder(new Label("Start logging to build your streaks 🌿"));

        // Weather
        try {
            WeatherService ws = new WeatherService();
            WeatherService.WeatherNow w = ws.getNowAuto();
            String advice = new SuggestionService().waterAdvice(w.precipMm, w.tempC);
            String area = (w.locationName != null && !w.locationName.isBlank()) ? w.locationName : "your area";
            weatherAdvice.setText(String.format("%s (%.1f°C • %s)", advice, w.tempC, area));
            Tooltip.install(weatherAdvice, new Tooltip("Data: WeatherAPI • Auto‑IP"));
        } catch (Exception e) {
            weatherAdvice.setText("Weather unavailable: " + e.getMessage());
        }

        configureTaskCells();
        loadTasks();
    }

    private void configureTaskCells() {
        taskList.setCellFactory(lv -> new ListCell<>() {
            private final Label badge = new Label();
            private final Label title = new Label();
            private final Region spacer = new Region();
            private final Button done = new Button("Mark Done");
            private final HBox root = new HBox(8, badge, title, spacer, done);

            {
                badge.getStyleClass().add("badge");
                done.getStyleClass().addAll("accent-btn", "pill");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.setPadding(new Insets(6));
            }

            @Override protected void updateItem(CareTask t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null); setGraphic(null); return;
                }
                String s = t.getStatus() == null ? "" : t.getStatus();
                badge.setText(s.equals("DUE") || s.equals("TODAY") ? "DUE" : "Next");
                String when = t.getDueDate() == null ? "—" : t.getDueDate().toString();
                String plant = t.getPlantName() == null ? "" : t.getPlantName();
                title.setText(plant.isBlank() ? when : (plant + " — " + when));

                done.setOnAction(e -> markDoneSafe(t.getId(), t.getPlantId()));
                setText(null); setGraphic(root);
            }
        });
    }

    private void loadTasks() {
        List<CareTask> items = taskRepo.findDueOrUpcoming();

        Comparator<CareTask> cmp = Comparator
                .comparing((CareTask t) -> {
                    String s = t.getStatus();
                    boolean dueish = "DUE".equals(s) || "TODAY".equals(s);
                    return dueish ? 0 : 1;
                })
                .thenComparing(t -> t.getDueDate() == null ? LocalDate.MAX : t.getDueDate());

        taskList.getItems().setAll(items.stream().sorted(cmp).collect(Collectors.toList()));
    }

    /** Robust "Mark Done": atomic status update + plant lastWatered + resync */
    private void markDoneSafe(String taskId, String plantId) {
        if (taskId == null || taskId.isBlank()) {
            showError("Task missing id", "Cannot update a task without an id.");
            return;
        }
        try {
            // 1) status -> DONE (no full replace)
            taskRepo.updateStatus(taskId, "DONE");

            // 2) update plant.lastWatered = today
            if (plantId != null && !plantId.isBlank()) {
                Plant p = plantRepo.findById(plantId);
                if (p != null) {
                    p.setLastWatered(LocalDate.now());
                    plantRepo.replaceById(p);
                }
            }

            // 3) re-sync tasks (creates next WATER task using current weather)
            taskService.syncAllWaterTasks();

            // 4) refresh list
            loadTasks();
        } catch (Exception ex) {
            showError("Failed to mark task done", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }
}
