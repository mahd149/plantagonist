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
import javafx.stage.Stage;
import org.plantagonist.core.models.Plant;
import org.plantagonist.core.repositories.PlantRepository;

import java.io.File;
import java.time.LocalDate;
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

    // NEW fields

    private final ObservableList<Plant> backing = FXCollections.observableArrayList();
    private final PlantRepository repo = new PlantRepository();

    private final CareTaskRepository taskRepo = new CareTaskRepository();
    private final TaskService taskService = new TaskService(repo, taskRepo, new WeatherService(), new SuggestionService());


    // Card layout constants (tuned for 3 per row on common widths)
    private static final double CARD_WIDTH = 320;   // each card’s preferred width
    private static final double IMAGE_HEIGHT = 180; // hero image height

    @FXML
    public void initialize() {
        // initial load
        reload();

        // live search
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, q) -> render());
        }

        // FlowPane should wrap cards; ensure it uses available width
        grid.setPrefWrapLength(CARD_WIDTH * 3 + 24); // 3 cards + gaps
    }

    // ===== Buttons =====
    @FXML
    private void addPlant() {
        try {
            Stage owner = getWindow();
            Plant created = PlantFormController.openDialog(owner, null);
            if (created == null) return;

            if (created.getId() == null || created.getId().isBlank()) {
                created.setId(java.util.UUID.randomUUID().toString());
            }
            repo.insertOne(created);
            reload();
            taskService.syncAllWaterTasks();   // <— sync tasks

        } catch (Throwable t) {
            showError("Couldn’t add plant", t.getMessage());
        }
    }

    @FXML
    private void reload() {
        List<Plant> all = repo.findAll();
        backing.setAll(all);
        render();
        taskService.syncAllWaterTasks();
    }

    // ===== Rendering cards =====
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
        iv.setFitWidth(CARD_WIDTH - 24); // account for padding
        iv.setFitHeight(IMAGE_HEIGHT);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        File imgFile = (p.getPhotoPath() != null) ? new File(p.getPhotoPath()) : null;
        if (imgFile != null && imgFile.exists()) {
            iv.setImage(new Image(imgFile.toURI().toString(), iv.getFitWidth(), IMAGE_HEIGHT, false, true, true));
        } else {
            // fallback: subtle empty state (no external asset required)
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

    // ===== CRUD helpers =====
    private void edit(Plant target) {
        try {
            Plant workingCopy = deepCopy(target);
            Plant edited = PlantFormController.openDialog(getWindow(), workingCopy);
            if (edited != null) {
                String id = target.getId();
                if (edited.getId() == null || edited.getId().isBlank()) {
                    edited.setId(id);
                }
                repo.replaceById(id, edited);
                reload();
                taskService.syncAllWaterTasks();   // <— sync tasks
            }
        } catch (Throwable t) {
            showError("Couldn’t edit plant", t.getMessage());
        }
    }

    private void delete(Plant p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Remove Plant");
        a.setHeaderText("Delete “" + Objects.toString(p.getName(), "Unnamed") + "”?");
        a.setContentText("This will remove the plant from your list.");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                repo.deleteById(p.getId());
                reload();
                taskService.syncAllWaterTasks();
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
