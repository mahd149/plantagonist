package org.plantagonist.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.plantagonist.core.auth.CurrentUser;
import org.plantagonist.core.models.CareLogEntry;
import org.plantagonist.core.models.CareTask;
import org.plantagonist.core.models.Plant;
import org.plantagonist.core.repositories.CareLogRepository;
import org.plantagonist.core.repositories.CareTaskRepository;
import org.plantagonist.core.repositories.PlantRepository;
import org.plantagonist.core.services.SuggestionService;
import org.plantagonist.core.services.TaskService;
import org.plantagonist.core.services.WeatherService;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CareLogController {

    @FXML private Label plantFactLabel;
    @FXML private TableView<CareTask> todayTasksTable;
    @FXML private TableView<CareTask> upcomingTasksTable;
    @FXML private TableView<CareLogEntry> careHistoryTable;
    @FXML private ListView<String> notificationsList;
    @FXML private VBox notificationContainer;
    @FXML private TabPane careLogTabPane;

    private final CareLogRepository careLogRepository;
    private final CareTaskRepository careTaskRepository;
    private final PlantRepository plantRepository;
    private final TaskService taskService;
    private final String currentUserId;

    private ObservableList<CareTask> todayTasks;
    private ObservableList<CareTask> upcomingTasks;
    private ObservableList<CareLogEntry> careHistory;
    private ObservableList<String> notifications;

    public CareLogController() {
        this.careLogRepository = new CareLogRepository();
        this.careTaskRepository = new CareTaskRepository();
        this.plantRepository = new PlantRepository();

        // Initialize services with required dependencies
        WeatherService weatherService = new WeatherService();
        SuggestionService suggestionService = new SuggestionService();
        this.taskService = new TaskService(plantRepository, careTaskRepository, weatherService, suggestionService);

        this.currentUserId = CurrentUser.get().getId();

        this.todayTasks = FXCollections.observableArrayList();
        this.upcomingTasks = FXCollections.observableArrayList();
        this.careHistory = FXCollections.observableArrayList();
        this.notifications = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        // Attach table styles globally
        String tableCss = getClass().getResource("/org/plantagonist/css/table-styles.css").toExternalForm();
        todayTasksTable.getStylesheets().add(tableCss);
        upcomingTasksTable.getStylesheets().add(tableCss);
        careHistoryTable.getStylesheets().add(tableCss);
        notificationsList.getStylesheets().add(tableCss);

        setupTables();
        loadPlantFact();
        loadData();
        setupTabSelectionListener();
    }

    private void setupTables() {
        // Today's tasks table
        TableColumn<CareTask, String> taskCol = new TableColumn<>("Task");
        taskCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getTypeDisplay()
        ));

        TableColumn<CareTask, String> plantCol = new TableColumn<>("Plant");
        plantCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getPlantName()
        ));

        TableColumn<CareTask, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getStatusDisplay()
        ));

        TableColumn<CareTask, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(createActionCellFactory());

        todayTasksTable.getColumns().setAll(taskCol, plantCol, statusCol, actionCol);
        todayTasksTable.setItems(todayTasks);

        // Upcoming tasks table
        TableColumn<CareTask, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getDueDate().toString()
        ));

        TableColumn<CareTask, String> upcomingTaskCol = new TableColumn<>("Task");
        upcomingTaskCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getTypeDisplay()
        ));

        TableColumn<CareTask, String> upcomingPlantCol = new TableColumn<>("Plant");
        upcomingPlantCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getPlantName()
        ));

        TableColumn<CareTask, Void> upcomingActionCol = new TableColumn<>("Actions");
        upcomingActionCol.setCellFactory(createActionCellFactory());

        upcomingTasksTable.getColumns().setAll(dateCol, upcomingTaskCol, upcomingPlantCol, upcomingActionCol);
        upcomingTasksTable.setItems(upcomingTasks);

        // Care history table
        TableColumn<CareLogEntry, String> historyDateCol = new TableColumn<>("Date");
        historyDateCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getDate().toString()
        ));

        TableColumn<CareLogEntry, String> historyActionCol = new TableColumn<>("Action");
        historyActionCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getActionTypeDisplay()
        ));

        TableColumn<CareLogEntry, String> historyPlantCol = new TableColumn<>("Plant");
        historyPlantCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getPlantName()
        ));

        TableColumn<CareLogEntry, String> historyDetailsCol = new TableColumn<>("Details");
        historyDetailsCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    CareLogEntry entry = data.getValue();
                    if ("WATERING".equals(entry.getActionType()) && entry.getSoilMoisturePct() != null) {
                        return "Moisture: " + entry.getSoilMoisturePct() + "%";
                    } else if ("FERTILIZING".equals(entry.getActionType()) && entry.getFertilizerMl() != null) {
                        return "Fertilizer: " + entry.getFertilizerMl() + "ml";
                    }
                    return entry.getNotes() != null ? entry.getNotes() : "";
                }
        ));

        careHistoryTable.getColumns().setAll(historyDateCol, historyActionCol, historyPlantCol, historyDetailsCol);
        careHistoryTable.setItems(careHistory);

        // Notifications list
        notificationsList.setItems(notifications);
        notificationsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("notification-label");
                }
            }
        });
    }

    private Callback<TableColumn<CareTask, Void>, TableCell<CareTask, Void>> createActionCellFactory() {
        return param -> new TableCell<CareTask, Void>() {
            private final HBox buttonBox = new HBox(5);
            private final Button doneBtn = new Button("✅ Done");
            private final Button missBtn = new Button("❌ Miss");
            private final Button cancelBtn = new Button("🚫 Cancel");

            {
                buttonBox.getChildren().addAll(doneBtn, missBtn, cancelBtn);
                buttonBox.setStyle("-fx-alignment: center;");

                doneBtn.getStyleClass().addAll("action-btn", "done");
                missBtn.getStyleClass().addAll("action-btn", "miss");
                cancelBtn.getStyleClass().addAll("action-btn", "cancel");

                doneBtn.setOnAction(event -> {
                    CareTask task = getTableView().getItems().get(getIndex());
                    markTaskDone(task);
                });

                missBtn.setOnAction(event -> {
                    CareTask task = getTableView().getItems().get(getIndex());
                    markTaskMissing(task);
                });

                cancelBtn.setOnAction(event -> {
                    CareTask task = getTableView().getItems().get(getIndex());
                    cancelTask(task);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        };
    }

    private void loadPlantFact() {
        String[] facts = {
                "🌱 Plants can communicate with each other through chemical signals!",
                "💧 Some plants can absorb up to 95% of their water through their leaves!",
                "🌿 Talking to plants really can help them grow faster!",
                "☀️ The oldest living plant is over 5,000 years old!",
                "🌺 Some plants can change color based on soil pH levels!",
                "🔄 Plants release oxygen during the day and carbon dioxide at night!",
                "💫 The smell of fresh soil is caused by bacteria called actinomycetes!",
                "🌻 Sunflowers can track the sun across the sky throughout the day!"
        };

        int randomIndex = new Random().nextInt(facts.length);
        plantFactLabel.setText(facts[randomIndex]);
        plantFactLabel.getStyleClass().add("plant-fact-label");
    }

    private void loadData() {
        loadTodayTasks();
        loadUpcomingTasks();
        loadCareHistory();
        loadNotifications();
    }

    private void loadTodayTasks() {
        List<CareTask> tasks = careTaskRepository.findByUserIdAndDate(currentUserId, LocalDate.now());
        todayTasks.setAll(tasks);
    }

    private void loadUpcomingTasks() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate nextWeek = LocalDate.now().plusWeeks(1);
        List<CareTask> tasks = careTaskRepository.findByUserIdAndDateRange(currentUserId, tomorrow, nextWeek);
        upcomingTasks.setAll(tasks);
    }

    private void loadCareHistory() {
        List<CareLogEntry> entries = careLogRepository.findRecentByUser(currentUserId, 50);
        careHistory.setAll(entries);
    }

    private void loadNotifications() {
        notifications.clear();

        // Check for due tasks
        List<CareTask> dueTasks = careTaskRepository.findByUserIdAndStatus(currentUserId, "DUE");
        if (!dueTasks.isEmpty()) {
            notifications.add("⚠️ You have " + dueTasks.size() + " tasks due today!");
        }

        // Check for missed tasks
        List<CareTask> missedTasks = careTaskRepository.findByUserIdAndStatus(currentUserId, "MISSED");
        if (!missedTasks.isEmpty()) {
            notifications.add("❌ You have " + missedTasks.size() + " missed tasks!");
        }

        // Hide notification section if no notifications
        notificationContainer.setVisible(!notifications.isEmpty());
        notificationContainer.setManaged(!notifications.isEmpty());
    }

    private void setupTabSelectionListener() {
        careLogTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                switch (newTab.getText()) {
                    case "Today's Tasks":
                        loadTodayTasks();
                        break;
                    case "Upcoming Tasks":
                        loadUpcomingTasks();
                        break;
                    case "Care History":
                        loadCareHistory();
                        break;
                }
            }
        });
    }

    @FXML
    private void logWatering() {
        showLogDialog("WATERING");
    }

    @FXML
    private void logFertilizing() {
        showLogDialog("FERTILIZING");
    }

    @FXML
    private void logSoilChange() {
        showLogDialog("SOIL_CHANGE");
    }

    private void showLogDialog(String actionType) {
        Dialog<CareLogEntry> dialog = new Dialog<>();
        dialog.setTitle("Log " + actionType.replace("_", " "));
        dialog.setHeaderText("Record your plant care activity");

        // Attach form stylesheet
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/org/plantagonist/css/form-styles.css").toExternalForm()
        );

        ButtonType logButtonType = new ButtonType("Log", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(logButtonType, ButtonType.CANCEL);

        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(20, 10, 10, 10));

        ComboBox<Plant> plantCombo = new ComboBox<>();
        plantCombo.setPromptText("Select Plant");
        List<Plant> userPlants = plantRepository.findByUserId(currentUserId);
        plantCombo.getItems().addAll(userPlants);
        plantCombo.setCellFactory(lv -> new ListCell<Plant>() {
            @Override
            protected void updateItem(Plant item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName() + " (" + item.getSpecies() + ")");
            }
        });
        plantCombo.setButtonCell(new ListCell<Plant>() {
            @Override
            protected void updateItem(Plant item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName() + " (" + item.getSpecies() + ")");
            }
        });

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPromptText("Date");

        TextField notesField = new TextField();
        notesField.setPromptText("Notes (optional)");

        // Add specific fields based on action type
        if ("WATERING".equals(actionType)) {
            TextField moistureField = new TextField();
            moistureField.setPromptText("Soil Moisture % (optional)");
            form.getChildren().addAll(
                    new Label("Plant:"), plantCombo,
                    new Label("Date:"), datePicker,
                    new Label("Soil Moisture %:"), moistureField,
                    new Label("Notes:"), notesField
            );
        } else if ("FERTILIZING".equals(actionType)) {
            TextField fertilizerField = new TextField();
            fertilizerField.setPromptText("Fertilizer ml (optional)");
            form.getChildren().addAll(
                    new Label("Plant:"), plantCombo,
                    new Label("Date:"), datePicker,
                    new Label("Fertilizer Amount:"), fertilizerField,
                    new Label("Notes:"), notesField
            );
        } else {
            form.getChildren().addAll(
                    new Label("Plant:"), plantCombo,
                    new Label("Date:"), datePicker,
                    new Label("Notes:"), notesField
            );
        }

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == logButtonType) {
                if (plantCombo.getValue() == null) {
                    showAlert("Please select a plant");
                    return null;
                }

                Plant selectedPlant = plantCombo.getValue();
                CareLogEntry entry = new CareLogEntry();
                entry.setId(UUID.randomUUID().toString());
                entry.setPlantId(selectedPlant.getId());
                entry.setUserId(currentUserId);
                entry.setDate(datePicker.getValue());
                entry.setActionType(actionType);
                entry.setPlantName(selectedPlant.getName());
                entry.setNotes(notesField.getText());

                return entry;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(entry -> {
            careLogRepository.insert(entry);
            loadCareHistory();
            showSuccess("Care activity logged successfully!");
        });
    }

    private void markTaskDone(CareTask task) {
        task.setStatus("DONE");
        careTaskRepository.updateStatus(task.getId(), "DONE");

        CareLogEntry logEntry = new CareLogEntry();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setPlantId(task.getPlantId());
        logEntry.setUserId(currentUserId);
        logEntry.setDate(LocalDate.now());
        logEntry.setActionType(task.getType());
        logEntry.setPlantName(task.getPlantName());
        logEntry.setNotes("Completed scheduled task");
        careLogRepository.insert(logEntry);

        loadTodayTasks();
        loadCareHistory();
        loadNotifications();
    }

    private void markTaskMissing(CareTask task) {
        task.setStatus("MISSED");
        careTaskRepository.updateStatus(task.getId(), "MISSED");
        loadTodayTasks();
        loadNotifications();
    }

    private void cancelTask(CareTask task) {
        task.setStatus("CANCELLED");
        careTaskRepository.updateStatus(task.getId(), "CANCELLED");
        loadTodayTasks();
        loadNotifications();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void refreshData() {
        loadData();
        loadPlantFact();
    }
}
