package org.plantagonist.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.plantagonist.core.models.CareTask;
import org.plantagonist.core.models.Plant;
import org.plantagonist.core.repositories.CareTaskRepository;
import org.plantagonist.core.repositories.PlantRepository;
import org.plantagonist.core.services.SuggestionService;
import org.plantagonist.core.services.TaskService;
import org.plantagonist.core.services.WeatherService;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DashboardController {

    // Weather-related FXML elements
    @FXML private Label weatherAdvice;
    @FXML private Label weatherLocation;
    @FXML private Label weatherTemp;
    @FXML private Label weatherPrecip;
    @FXML private Label weatherWind;
    @FXML private Label lastUpdated;

    // Task and plant related elements
    @FXML private ListView<CareTask> taskList;
    @FXML private ListView<String> streakList;
    @FXML private FlowPane plantsGrid;
    @FXML private Label taskCount;
    @FXML private Label plantsCount;

    // State elements
    @FXML private VBox emptyPlantsState;
    @FXML private HBox plantsTipContainer;
    @FXML private Button addPlantBtn;

    private final CareTaskRepository taskRepo = new CareTaskRepository();
    private final PlantRepository plantRepo = new PlantRepository();
    private final TaskService taskService =
            new TaskService(plantRepo, taskRepo, new WeatherService(), new SuggestionService());

    // Plant card constants
    private static final double PLANT_CARD_WIDTH = 180;
    private static final double PLANT_CARD_HEIGHT = 240;
    private static final double PLANT_IMAGE_SIZE = 140;

    @FXML
    public void initialize() {
        setupPlaceholders();
        setupWeatherDisplay();
        configureTaskCells();
        loadTasks();
        loadPlants();
        updateTimestamp();
    }

    private void setupPlaceholders() {
        if (taskList != null) taskList.setPlaceholder(createStyledPlaceholder("No tasks due today ✨"));
        if (streakList != null) streakList.setPlaceholder(createStyledPlaceholder("Start logging to build your streaks 🌿"));
    }

    private Label createStyledPlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.setStyle("-fx-text-fill: -color-subtext; -fx-font-size: 12px; -fx-padding: 20;");
        return placeholder;
    }

    private void setupWeatherDisplay() {
        // Initialize weather elements with loading states
        if (weatherLocation != null) weatherLocation.setText("Detecting location...");
        if (weatherTemp != null) weatherTemp.setText("--°C");
        if (weatherPrecip != null) weatherPrecip.setText("--mm");
        if (weatherWind != null) weatherWind.setText("-- km/h");
        if (weatherAdvice != null) weatherAdvice.setText("Loading weather data...");

        // Load weather data asynchronously to avoid blocking UI
        Platform.runLater(this::loadWeatherData);
    }

    private void loadWeatherData() {
        try {
            WeatherService ws = new WeatherService();
            WeatherService.WeatherNow weather = ws.getNowAuto();
            SuggestionService suggestions = new SuggestionService();

            // Update weather advice
            String advice = suggestions.waterAdvice(weather.precipMm, weather.tempC);
            if (weatherAdvice != null) {
                weatherAdvice.setText(advice);
            }

            // Update location
            String location = (weather.locationName != null && !weather.locationName.isBlank())
                    ? weather.locationName : "Unknown location";
            if (weatherLocation != null) {
                weatherLocation.setText(location);
            }

            // Update temperature
            if (weatherTemp != null) {
                weatherTemp.setText(String.format("%.1f°C", weather.tempC));
            }

            // Update precipitation
            if (weatherPrecip != null) {
                String precipText = weather.precipMm == 0 ? "No rain" : String.format("%.1fmm", weather.precipMm);
                weatherPrecip.setText(precipText);
            }

            // Update wind (if available from your weather service)
            if (weatherWind != null) {
                // You might need to add wind data to your WeatherService
                weatherWind.setText("Light breeze"); // Placeholder
            }

            // Add tooltip with more details
            if (weatherAdvice != null) {
                Tooltip tooltip = new Tooltip(String.format(
                        "Current conditions:\nTemperature: %.1f°C\nPrecipitation: %.1fmm\nLocation: %s\nData: WeatherAPI",
                        weather.tempC, weather.precipMm, location));
                tooltip.setStyle("-fx-font-size: 11px;");
                Tooltip.install(weatherAdvice, tooltip);
            }

        } catch (Exception e) {
            // Graceful error handling
            if (weatherAdvice != null) {
                weatherAdvice.setText("Weather data temporarily unavailable");
            }
            if (weatherLocation != null) {
                weatherLocation.setText("Location unavailable");
            }
            System.err.println("Weather load error: " + e.getMessage());
        }
    }

    private void updateTimestamp() {
        if (lastUpdated != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            lastUpdated.setText("Updated at " + timestamp);
        }
    }

    private void configureTaskCells() {
        taskList.setCellFactory(lv -> new ListCell<>() {
            private final Label badge = new Label();
            private final Label title = new Label();
            private final Region spacer = new Region();
            private final Button done = new Button("Done");
            private final HBox root = new HBox(8, badge, title, spacer, done);

            {
                badge.getStyleClass().add("badge");
                done.getStyleClass().addAll("enhanced-nav-btn");
                done.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: -color-primary; -fx-text-fill: white;");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.setPadding(new Insets(8, 6, 8, 6));
                root.setAlignment(Pos.CENTER_LEFT);
            }

            @Override protected void updateItem(CareTask t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null); setGraphic(null); return;
                }

                String status = t.getStatus() == null ? "" : t.getStatus();
                badge.setText(status.equals("DUE") || status.equals("TODAY") ? "DUE" : "NEXT");

                String when = t.getDueDate() == null ? "—" : t.getDueDate().toString();
                String plant = t.getPlantName() == null ? "General Task" : t.getPlantName();
                title.setText(plant + " • " + when);
                title.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text;");

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

        List<CareTask> sortedTasks = items.stream().sorted(cmp).collect(Collectors.toList());
        taskList.getItems().setAll(sortedTasks);

        // Update task count
        if (taskCount != null) {
            int count = sortedTasks.size();
            taskCount.setText(count == 1 ? "1 task" : count + " tasks");
        }
    }

    private void loadPlants() {
        try {
            List<Plant> plants = plantRepo.findAll();
            plantsGrid.getChildren().clear();

            // Update plants count
            if (plantsCount != null) {
                int count = plants.size();
                plantsCount.setText(count == 1 ? "1 plant" : count + " plants");
            }

            // Show/hide empty state
            boolean hasPlants = !plants.isEmpty();
            if (emptyPlantsState != null) {
                emptyPlantsState.setVisible(!hasPlants);
                emptyPlantsState.setManaged(!hasPlants);
            }
            if (plantsTipContainer != null) {
                plantsTipContainer.setVisible(hasPlants);
                plantsTipContainer.setManaged(hasPlants);
            }

            // Load plant cards
            for (Plant plant : plants) {
                VBox plantCard = createPlantCard(plant);
                plantsGrid.getChildren().add(plantCard);
            }
        } catch (Exception e) {
            System.err.println("Error loading plants: " + e.getMessage());
        }
    }

    private VBox createPlantCard(Plant plant) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "plant-card");
        card.setPrefWidth(PLANT_CARD_WIDTH);
        card.setPrefHeight(PLANT_CARD_HEIGHT);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(12));

        // Create image container
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(PLANT_IMAGE_SIZE, PLANT_IMAGE_SIZE);
        imageContainer.getStyleClass().add("plant-image-container");

        // Plant image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(PLANT_IMAGE_SIZE);
        imageView.setFitHeight(PLANT_IMAGE_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Set image or placeholder
        File imgFile = (plant.getPhotoPath() != null) ? new File(plant.getPhotoPath()) : null;
        if (imgFile != null && imgFile.exists()) {
            try {
                Image image = new Image(imgFile.toURI().toString(), PLANT_IMAGE_SIZE, PLANT_IMAGE_SIZE, true, true, true);
                imageView.setImage(image);
            } catch (Exception e) {
                setPlaceholderImage(imageView);
            }
        } else {
            setPlaceholderImage(imageView);
        }

        // Add rounded corners to image
        Rectangle clip = new Rectangle(PLANT_IMAGE_SIZE, PLANT_IMAGE_SIZE);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        imageView.setClip(clip);

        imageContainer.getChildren().add(imageView);

        // Plant name
        Label nameLabel = new Label(Objects.toString(plant.getName(), "Unnamed Plant"));
        nameLabel.getStyleClass().addAll("plant-name", "text-center");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(PLANT_CARD_WIDTH - 24);
        nameLabel.setAlignment(Pos.CENTER);

        // Species (optional, subtle)
        Label speciesLabel = new Label(Objects.toString(plant.getSpecies(), ""));
        speciesLabel.getStyleClass().addAll("plant-species", "subtle", "text-center");
        speciesLabel.setWrapText(true);
        speciesLabel.setMaxWidth(PLANT_CARD_WIDTH - 24);
        speciesLabel.setAlignment(Pos.CENTER);

        // Add spacing
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imageContainer, nameLabel);

        // Only add species label if it's not empty
        if (plant.getSpecies() != null && !plant.getSpecies().trim().isEmpty()) {
            card.getChildren().add(speciesLabel);
        }

        card.getChildren().add(spacer);

        // Enhanced hover effects
        card.setOnMouseEntered(e -> {
            card.getStyleClass().add("card-hover");
            card.setStyle(card.getStyle() + "; -fx-cursor: hand;");
        });
        card.setOnMouseExited(e -> {
            card.getStyleClass().remove("card-hover");
        });

        // Add click handler to edit plant
        card.setOnMouseClicked(e -> editPlant(plant));

        return card;
    }

    private void setPlaceholderImage(ImageView imageView) {
        try {
            // Try to load a default plant icon if available
            var defaultImageUrl = getClass().getResource("/org/plantagonist/ui/plant-placeholder.png");
            if (defaultImageUrl != null) {
                imageView.setImage(new Image(defaultImageUrl.toExternalForm(), PLANT_IMAGE_SIZE, PLANT_IMAGE_SIZE, true, true, true));
            } else {
                // Create a simple colored rectangle as placeholder
                imageView.setImage(createPlaceholderImage());
            }
        } catch (Exception e) {
            imageView.setImage(createPlaceholderImage());
        }
    }

    private Image createPlaceholderImage() {
        // Create a simple 1x1 transparent image as fallback
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAEElEQVR4nGNgZGBgYAAAAAIAAeFo2e8AAAAASUVORK5CYII=",
                PLANT_IMAGE_SIZE, PLANT_IMAGE_SIZE, false, true);
    }

    @FXML
    private void addPlant() {
        try {
            Stage owner = getWindow();
            Plant created = PlantFormController.openDialog(owner, null);
            if (created == null) return;

            if (created.getId() == null || created.getId().isBlank()) {
                created.setId(java.util.UUID.randomUUID().toString());
            }
            plantRepo.insertOne(created);
            loadPlants(); // Refresh the plants display
            taskService.syncAllWaterTasks();
        } catch (Exception e) {
            showError("Couldn't add plant", e.getMessage());
        }
    }

    // Enhanced button hover effects
    @FXML
    private void onAddPlantButtonHover(MouseEvent event) {
        if (addPlantBtn != null) {
            addPlantBtn.setStyle(addPlantBtn.getStyle() +
                    "; -fx-scale-x: 1.03; -fx-scale-y: 1.03;" +
                    "; -fx-background-color: linear-gradient(135deg, -color-primary-3 0%, #2F8E58 100%);" +
                    "; -fx-effect: dropshadow(gaussian, rgba(26,77,46,0.6), 16, 0.4, 0, 6);");
        }
    }

    @FXML
    private void onAddPlantButtonExit(MouseEvent event) {
        if (addPlantBtn != null) {
            addPlantBtn.setStyle(
                    "-fx-background-color: linear-gradient(135deg, -color-primary-2 0%, -color-primary-3 100%); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: 700; " +
                            "-fx-font-size: 13px; " +
                            "-fx-padding: 12 24; " +
                            "-fx-background-radius: 20px; " +
                            "-fx-border-radius: 20px; " +
                            "-fx-border-color: rgba(255,255,255,0.15); " +
                            "-fx-border-width: 1; " +
                            "-fx-effect: dropshadow(gaussian, rgba(26,77,46,0.4), 12, 0.3, 0, 4); " +
                            "-fx-cursor: hand; " +
                            "-fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        }
    }

    private void editPlant(Plant plant) {
        try {
            Plant workingCopy = deepCopyPlant(plant);
            Plant edited = PlantFormController.openDialog(getWindow(), workingCopy);
            if (edited != null) {
                String id = plant.getId();
                if (edited.getId() == null || edited.getId().isBlank()) {
                    edited.setId(id);
                }
                plantRepo.replaceById(id, edited);
                loadPlants(); // Refresh the plants display
                taskService.syncAllWaterTasks();
            }
        } catch (Exception e) {
            showError("Couldn't edit plant", e.getMessage());
        }
    }

    private Plant deepCopyPlant(Plant src) {
        Plant copy = new Plant();
        copy.setId(src.getId());
        copy.setName(src.getName());
        copy.setSpecies(src.getSpecies());
        copy.setWaterEveryDays(src.getWaterEveryDays());
        copy.setSunlightHours(src.getSunlightHours());
        copy.setPhotoPath(src.getPhotoPath());
        copy.setLastWatered(src.getLastWatered());
        return copy;
    }

    private Stage getWindow() {
        if (plantsGrid != null && plantsGrid.getScene() != null) {
            return (Stage) plantsGrid.getScene().getWindow();
        }
        return null;
    }

    private void markDoneSafe(String taskId, String plantId) {
        if (taskId == null || taskId.isBlank()) {
            showError("Task missing id", "Cannot update a task without an id.");
            return;
        }
        try {
            taskRepo.updateStatus(taskId, "DONE");

            if (plantId != null && !plantId.isBlank()) {
                Plant p = plantRepo.findById(plantId);
                if (p != null) {
                    p.setLastWatered(LocalDate.now());
                    plantRepo.replaceById(p);
                }
            }

            taskService.syncAllWaterTasks();
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