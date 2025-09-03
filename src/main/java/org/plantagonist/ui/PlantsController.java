package org.plantagonist.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.plantagonist.core.auth.CurrentUser;
import org.plantagonist.core.models.Plant;
import org.plantagonist.core.models.JournalEntry;
import org.plantagonist.core.repositories.PlantRepository;
import org.plantagonist.core.repositories.JournalRepository;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.plantagonist.core.repositories.CareTaskRepository;
import org.plantagonist.core.services.TaskService;
import org.plantagonist.core.services.WeatherService;
import org.plantagonist.core.services.SuggestionService;

public class PlantsController {

    @FXML private TextField searchField;
    @FXML private FlowPane grid;
    @FXML private ComboBox<Plant> plantSelector;
    @FXML private VBox journalEntriesContainer;
    @FXML private Button addJournalEntryBtn;

    // NEW fields for journal
    private final JournalRepository journalRepo = new JournalRepository();
    private final ObservableList<JournalEntry> journalEntries = FXCollections.observableArrayList();
    private Plant selectedPlantForJournal;

    private final ObservableList<Plant> backing = FXCollections.observableArrayList();
    private final PlantRepository repo = new PlantRepository();

    private final CareTaskRepository taskRepo = new CareTaskRepository();
    private final TaskService taskService = new TaskService(repo, taskRepo, new WeatherService(), new SuggestionService());

    // Card layout constants
    private static final double CARD_WIDTH = 320;
    private static final double IMAGE_HEIGHT = 180;

    @FXML
    public void initialize() {
        // initial load
        String userId = CurrentUser.get().getId();
        reload();

        // live search
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, q) -> render());
        }

        // FlowPane should wrap cards
        grid.setPrefWrapLength(CARD_WIDTH * 3 + 24);

        // Initialize journal section
        initializeJournalSection();
    }

    private void initializeJournalSection() {
        if (plantSelector != null) {
            // Populate plant selector
            plantSelector.setItems(backing);
            plantSelector.setCellFactory(param -> new ListCell<Plant>() {
                @Override
                protected void updateItem(Plant plant, boolean empty) {
                    super.updateItem(plant, empty);
                    if (empty || plant == null) {
                        setText(null);
                    } else {
                        setText(plant.getName() + " (" + plant.getSpecies() + ")");
                    }
                }
            });
            plantSelector.setButtonCell(new ListCell<Plant>() {
                @Override
                protected void updateItem(Plant plant, boolean empty) {
                    super.updateItem(plant, empty);
                    if (empty || plant == null) {
                        setText("All Plants");
                    } else {
                        setText(plant.getName() + " (" + plant.getSpecies() + ")");
                    }
                }
            });

            plantSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                selectedPlantForJournal = newVal;
                loadJournalEntries();
            });
        }

        if (addJournalEntryBtn != null) {
            addJournalEntryBtn.setOnAction(e -> openJournalEntryDialog());
        }

        // Load initial journal entries
        loadJournalEntries();
    }

    private void loadJournalEntries() {
        String userId = CurrentUser.get().getId();
        journalEntries.clear();

        if (selectedPlantForJournal != null) {
            // Load entries for selected plant
            List<JournalEntry> entries = journalRepo.findByUserIdAndPlantId(userId, selectedPlantForJournal.getId());
            journalEntries.addAll(entries);
        } else {
            // Load all entries for user
            List<JournalEntry> entries = journalRepo.findByUserId(userId);
            journalEntries.addAll(entries);
        }

        renderJournalEntries();
    }

    private void renderJournalEntries() {
        if (journalEntriesContainer == null) return;

        journalEntriesContainer.getChildren().clear();

        if (journalEntries.isEmpty()) {
            Label emptyLabel = new Label("No journal entries yet.\nClick 'Add New Entry' to start your plant journal!");
            emptyLabel.getStyleClass().addAll("journal-empty", "text-center");
            emptyLabel.setAlignment(javafx.geometry.Pos.CENTER);
            emptyLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            journalEntriesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (JournalEntry entry : journalEntries) {
            journalEntriesContainer.getChildren().add(createJournalEntryCard(entry));
        }
    }

    private Region createJournalEntryCard(JournalEntry entry) {
        HBox card = new HBox();
        card.getStyleClass().add("journal-entry-card-clean");
        card.setMaxWidth(600);

        // Left side - Text content
        VBox textContent = new VBox(12);
        textContent.getStyleClass().add("entry-content-side");
        textContent.setPadding(new Insets(20));
        textContent.setPrefWidth(400);

        // Date
        Label dateLabel = new Label(entry.getFormattedDate());
        dateLabel.getStyleClass().add("entry-date-clean");

        // Plant name if available
        if (entry.getPlantName() != null && !entry.getPlantName().isEmpty()) {
            Label plantLabel = new Label("• " + entry.getPlantName());
            plantLabel.getStyleClass().add("entry-date-clean");
            plantLabel.setStyle("-fx-text-fill: #7A8F95 !important;");
            HBox header = new HBox(8, dateLabel, plantLabel);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            textContent.getChildren().add(header);
        } else {
            textContent.getChildren().add(dateLabel);
        }

        // Content
        Label contentLabel = new Label(entry.getContent());
        contentLabel.getStyleClass().add("entry-content-clean");
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(360);
        contentLabel.setLineSpacing(5);
        textContent.getChildren().add(contentLabel);

        card.getChildren().add(textContent);

        // Right side - Photo if available
        if (entry.getPhotoPath() != null && !entry.getPhotoPath().isEmpty()) {
            File imgFile = new File(entry.getPhotoPath());
            if (imgFile.exists()) {
                try {
                    VBox photoContainer = new VBox();
                    photoContainer.getStyleClass().add("entry-photo-side");
                    photoContainer.setPadding(new Insets(15));
                    photoContainer.setPrefWidth(200);

                    ImageView photoView = new ImageView();
                    photoView.setFitWidth(150);
                    photoView.setFitHeight(150);
                    photoView.setPreserveRatio(true);
                    photoView.setSmooth(true);
                    photoView.getStyleClass().add("journal-photo-clean");
                    photoView.setImage(new Image(imgFile.toURI().toString()));

                    photoContainer.getChildren().add(photoView);
                    card.getChildren().add(photoContainer);
                } catch (Exception e) {
                    System.out.println("Failed to load journal photo: " + e.getMessage());
                }
            }
        }

        return card;
    }

    private void openJournalEntryDialog() {
        try {
            Stage owner = getWindow();
            JournalEntryDialogController.openDialog(owner, selectedPlantForJournal, this::saveJournalEntry);
        } catch (Exception e) {
            showError("Couldn't open journal entry", e.getMessage());
        }
    }

    private void saveJournalEntry(JournalEntry entry) {
        try {
            entry.setUserId(CurrentUser.get().getId());
            if (selectedPlantForJournal != null) {
                entry.setPlantId(selectedPlantForJournal.getId());
                entry.setPlantName(selectedPlantForJournal.getName());
            }

            journalRepo.insertOne(entry);
            loadJournalEntries();
        } catch (Exception e) {
            showError("Couldn't save journal entry", e.getMessage());
        }
    }

    // ===== Existing plant methods =====
    @FXML
    private void addPlant() {
        try {
            Stage owner = getWindow();
            Plant created = PlantFormController.openDialog(owner, null);
            if (created == null) return;

            if (created.getId() == null || created.getId().isBlank()) {
                created.setId(java.util.UUID.randomUUID().toString());
            }

            created.setUserId(CurrentUser.get().getId());
            repo.insertOne(created);
            reload();
            taskService.syncAllWaterTasks(CurrentUser.get().getId());

        } catch (Throwable t) {
            showError("Couldn't add plant", t.getMessage());
        }
    }

    @FXML
    private void reload() {
        String userId = CurrentUser.get().getId();
        List<Plant> all = repo.findByUserId(userId);
        backing.setAll(all);
        render();

        // Refresh plant selector if it exists
        if (plantSelector != null) {
            plantSelector.setItems(backing);
        }

        taskService.syncAllWaterTasks(userId);
    }

    private void render() {
        String needle = norm(searchField != null ? searchField.getText() : "");
        List<Plant> items = backing.stream()
                .filter(p -> matches(p, needle))
                .collect(Collectors.toList());

        grid.getChildren().setAll(items.stream().map(this::createCard).collect(Collectors.toList()));
    }

    private boolean matches(Plant p, String needle) {
        if (needle.isEmpty()) return true;
        return contains(p.getName(), needle) || contains(p.getSpecies(), needle);
    }

    private static boolean contains(String s, String n) { return s != null && s.toLowerCase().contains(n); }
    private static String norm(String s) { return s == null ? "" : s.trim().toLowerCase(); }

    private Region createCard(Plant p) {
        // Outer card
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(CARD_WIDTH);
        card.setFillWidth(true);
        card.setPadding(new Insets(12));

        // Image hero (with rounded clip)
        ImageView iv = new ImageView();
        iv.setFitWidth(CARD_WIDTH - 24);
        iv.setFitHeight(IMAGE_HEIGHT);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        File imgFile = (p.getPhotoPath() != null) ? new File(p.getPhotoPath()) : null;
        if (imgFile != null && imgFile.exists()) {
            iv.setImage(new Image(imgFile.toURI().toString(), iv.getFitWidth(), IMAGE_HEIGHT, false, true, true));
        } else {
            iv.setImage(new Image(
                    getClass().getResource("/org/plantagonist/ui/empty.png") != null
                            ? getClass().getResource("/org/plantagonist/ui/empty.png").toExternalForm()
                            : "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAEElEQVR4nGMAAQAABQABYy1V7wAAAABJRU5ErkJggg==",
                    iv.getFitWidth(), IMAGE_HEIGHT, false, true, true
            ));
        }
        Rectangle clip = new Rectangle(iv.getFitWidth(), IMAGE_HEIGHT);
        clip.setArcWidth(24); clip.setArcHeight(24);
        iv.setClip(clip);

        // Header
        Label name = new Label(Objects.toString(p.getName(), "Unnamed"));
        name.getStyleClass().add("card-header");

        Label species = new Label(Objects.toString(p.getSpecies(), "—"));
        species.getStyleClass().add("subtle");

        // Meta row (water cadence / next water)
        HBox meta = new HBox(8);
        meta.getChildren().addAll(
                badge("Water: " + textOrDash(p.getWaterEveryDays())),
                badge("Sun: " + textOrDash(p.getSunlightHours())),
                badge("Next: " + computeNextWaterText(p))
        );

        // Actions
        HBox actions = new HBox(8);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button edit = new Button("Edit");
        edit.getStyleClass().add("nav-btn");
        edit.setOnAction(e -> edit(p));

        Button remove = new Button("Remove");
        remove.getStyleClass().add("nav-btn");
        remove.setOnAction(e -> delete(p));

        actions.getChildren().addAll(spacer, edit, remove);

        // Allow clicking the image to edit (nice UX)
        iv.setOnMouseEntered(e -> card.setCursor(Cursor.HAND));
        iv.setOnMouseExited(e -> card.setCursor(Cursor.DEFAULT));
        iv.setOnMouseClicked(e -> edit(p));

        card.getChildren().addAll(iv, name, species, meta, actions);
        return card;
    }

    private String textOrDash(Object v) { return v == null ? "—" : v.toString(); }

    private Label badge(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("badge");
        return l;
    }

    private String computeNextWaterText(Plant p) {
        LocalDate last = p.getLastWatered();
        Integer every = p.getWaterEveryDays();
        if (last == null || every == null || every <= 0) return "—";
        return last.plusDays(every).toString();
    }

    private void edit(Plant target) {
        try {
            if (target == null || target.getId() == null || target.getId().isBlank()) {
                showError("Couldn't edit plant", "Plant is missing an id.");
                return;
            }

            Plant workingCopy = deepCopy(target);
            Plant edited = PlantFormController.openDialog(getWindow(), workingCopy);
            if (edited == null) return;

            edited.setId(target.getId());
            edited.setUserId(target.getUserId());

            repo.replaceById(target.getId(), edited);
            reload();
            taskService.syncAllWaterTasks(CurrentUser.get().getId());

        } catch (Exception t) {
            showError("Couldn't edit plant", t.getMessage());
        }
    }

    private void delete(Plant p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Remove Plant");
        a.setHeaderText("Delete " + Objects.toString(p.getName(), "Unnamed") + "?");
        a.setContentText("This will remove the plant from your list.");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                repo.deleteById(p.getId());
                reload();
                taskService.syncAllWaterTasks(CurrentUser.get().getId());
            }
        });
    }

    private Plant deepCopy(Plant src) {
        Plant x = new Plant();
        x.setId(src.getId());
        x.setName(src.getName());
        x.setSpecies(src.getSpecies());
        x.setWaterEveryDays(src.getWaterEveryDays());
        x.setSunlightHours(src.getSunlightHours());
        x.setPhotoPath(src.getPhotoPath());
        x.setLastWatered(src.getLastWatered());
        return x;
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(header);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private Stage getWindow() {
        if (grid != null && grid.getScene() != null) return (Stage) grid.getScene().getWindow();
        return null;
    }
}