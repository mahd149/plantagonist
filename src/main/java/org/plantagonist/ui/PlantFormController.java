package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.plantagonist.core.models.Plant;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.function.UnaryOperator;

public class PlantFormController {

    @FXML private Label title;
    @FXML private TextField nameField;
    @FXML private TextField speciesField;
    @FXML private TextField waterEveryField;
    @FXML private TextField sunlightField;
    @FXML private TextField photoField;
    @FXML private DatePicker lastWateredPicker;
    @FXML private Label errorLabel;
    @FXML private ImageView photoPreview;

    private Plant working;
    private Stage stage;
    private Plant result;

    /** Open the modal dialog. If editing, pass a plant; if creating, pass null. Returns saved Plant or null if canceled. */
    public static Plant openDialog(Stage owner, Plant forEdit) {
        try {
            var url = PlantFormController.class.getResource("/org/plantagonist/ui/plant_form.fxml");
            if (url == null) {
                // Loud error to help diagnose classpath issues
                Alert a = new Alert(Alert.AlertType.ERROR,
                        "Not found: /org/plantagonist/ui/plant_form.fxml\n" +
                                "Make sure it’s under src/main/resources/org/plantagonist/ui/",
                        ButtonType.OK);
                a.setHeaderText("Missing FXML");
                a.showAndWait();
                return null;
            }

            FXMLLoader fx = new FXMLLoader(url);
            Scene scene = new Scene(fx.load());
            PlantFormController c = fx.getController();
            Stage dialog = new Stage();
            dialog.setTitle(forEdit == null ? "Add Plant" : "Edit Plant");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) dialog.initOwner(owner);
            dialog.setScene(scene);

            c.stage = dialog;
            c.initFormattersAndDnD();
            c.setPlant(forEdit);

            dialog.showAndWait();
            return c.result;
        } catch (IOException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Load error: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText("Failed to open Plant Form");
            a.showAndWait();
            return null;
        }
    }


    /** Input masks, drag&drop, preview update listeners */
    private void initFormattersAndDnD() {
        // Positive integer for "water every"
        UnaryOperator<TextFormatter.Change> intFilter = ch -> {
            String txt = ch.getControlNewText();
            if (txt.isBlank()) return ch;
            return txt.matches("\\d{0,4}") ? ch : null;
        };
        waterEveryField.setTextFormatter(new TextFormatter<>(intFilter));

        // Non-negative decimal for "sunlight"
        UnaryOperator<TextFormatter.Change> dblFilter = ch -> {
            String txt = ch.getControlNewText();
            if (txt.isBlank()) return ch;
            // up to 2 decimals, change as needed
            return txt.matches("\\d{0,3}(\\.\\d{0,2})?") ? ch : null;
        };
        sunlightField.setTextFormatter(new TextFormatter<>(dblFilter));

        // Update preview when photo path changes
        photoField.textProperty().addListener((obs, old, v) -> updatePreview(v));

        // Drag & drop on the photo textfield
        photoField.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            e.consume();
        });
        photoField.setOnDragDropped(e -> {
            var db = e.getDragboard();
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File f = db.getFiles().get(0);
                photoField.setText(f.getAbsolutePath());
                updatePreview(f.getAbsolutePath());
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });
    }

    /** Load data into fields (or prepare empty form) */
    private void setPlant(Plant p) {
        this.working = (p == null ? new Plant() : p);
        if (title != null) title.setText(p == null ? "Add Plant" : "Edit Plant");

        nameField.setText(nz(working.getName()));
        speciesField.setText(nz(working.getSpecies()));
        waterEveryField.setText(working.getWaterEveryDays() == null ? "" : String.valueOf(working.getWaterEveryDays()));
        sunlightField.setText(working.getSunlightHours() == null ? "" : String.valueOf(working.getSunlightHours()));
        photoField.setText(nz(working.getPhotoPath()));
        lastWateredPicker.setValue(working.getLastWatered());

        errorLabel.setText("");
        updatePreview(photoField.getText());
    }

    private void updatePreview(String path) {
        try {
            if (path == null || path.isBlank()) {
                photoPreview.setImage(null);
                return;
            }
            File f = new File(path);
            if (!f.exists()) { photoPreview.setImage(null); return; }
            photoPreview.setImage(new Image(f.toURI().toString(), 280, 0, true, true, true));
        } catch (Exception ignored) {
            photoPreview.setImage(null);
        }
    }

    @FXML
    private void browsePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Plant Photo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            photoField.setText(f.getAbsolutePath());
            updatePreview(f.getAbsolutePath());
        }
    }

    @FXML
    private void save() {
        // Required name
        String name = nz(nameField.getText()).trim();
        if (name.isBlank()) { error("Name is required."); nameField.requestFocus(); return; }

        // Optional species
        String species = blankToNull(nz(speciesField.getText()).trim());

        // Optional cadence
        Integer waterEvery = null;
        String w = nz(waterEveryField.getText()).trim();
        if (!w.isBlank()) {
            try {
                int v = Integer.parseInt(w);
                if (v <= 0) throw new IllegalArgumentException();
                waterEvery = v;
            } catch (Exception e) { error("“Water every (days)” must be a positive integer."); waterEveryField.requestFocus(); return; }
        }

        // Optional sunlight
        Double sunlight = null;
        String s = nz(sunlightField.getText()).trim();
        if (!s.isBlank()) {
            try {
                double v = Double.parseDouble(s);
                if (v < 0) throw new IllegalArgumentException();
                sunlight = v;
            } catch (Exception e) { error("“Sunlight (hrs/day)” must be a non‑negative number."); sunlightField.requestFocus(); return; }
        }

        // Optional photo
        String photo = blankToNull(nz(photoField.getText()).trim());

        LocalDate lastWatered = lastWateredPicker.getValue();

        // Apply to working
        working.setName(name);
        working.setSpecies(species);
        working.setWaterEveryDays(waterEvery);
        working.setSunlightHours(sunlight);
        working.setPhotoPath(photo);
        working.setLastWatered(lastWatered);

        result = working;
        stage.close();
    }

    @FXML
    private void cancel() {
        result = null;
        stage.close();
    }

    private void error(String msg) { errorLabel.setText(msg); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
