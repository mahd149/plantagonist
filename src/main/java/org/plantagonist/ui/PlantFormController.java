package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
    @FXML private HBox errorContainer;
    @FXML private ImageView photoPreview;

    private Plant working;
    private Stage stage;
    private Plant result;

    @FXML
    public void initialize() {
        // Initialize the form with proper styling and formatting
        initFormattersAndDnD();

        // Clear any error messages initially
        hideError();

        // Set up photo preview placeholder
        setupPhotoPreview();
    }

    /** Open the modal dialog. If editing, pass a plant; if creating, pass null. Returns saved Plant or null if canceled. */
    public static Plant openDialog(Stage owner, Plant forEdit) {
        try {
            var url = PlantFormController.class.getResource("/org/plantagonist/ui/plant_form.fxml");
            if (url == null) {
                Alert a = new Alert(Alert.AlertType.ERROR,
                        "FXML file not found: /org/plantagonist/ui/plant_form.fxml\n" +
                                "Please ensure the file exists in src/main/resources/org/plantagonist/ui/",
                        ButtonType.OK);
                a.setHeaderText("Missing FXML File");
                a.showAndWait();
                return null;
            }

            FXMLLoader fx = new FXMLLoader(url);
            Scene scene = new Scene(fx.load());

            // Apply the CSS stylesheet to match your dark theme
            var cssUrl = PlantFormController.class.getResource("/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            PlantFormController c = fx.getController();
            Stage dialog = new Stage();
            dialog.setTitle(forEdit == null ? "Add New Plant" : "Edit Plant");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) dialog.initOwner(owner);
            dialog.setScene(scene);
            dialog.setResizable(false);

            // Center the dialog
            dialog.centerOnScreen();

            c.stage = dialog;
            c.setPlant(forEdit);

            dialog.showAndWait();
            return c.result;
        } catch (IOException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Failed to load Plant Form: " + e.getMessage() +
                            "\n\nPlease check that plant_form.fxml exists in the correct location.",
                    ButtonType.OK);
            a.setHeaderText("Form Load Error");
            a.showAndWait();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Unexpected error: " + e.getMessage(),
                    ButtonType.OK);
            a.setHeaderText("Error Opening Form");
            a.showAndWait();
            return null;
        }
    }

    /** Input masks, drag&drop, preview update listeners */
    private void initFormattersAndDnD() {
        if (waterEveryField != null) {
            // Positive integer for "water every"
            UnaryOperator<TextFormatter.Change> intFilter = ch -> {
                String txt = ch.getControlNewText();
                if (txt.isBlank()) return ch;
                return txt.matches("\\d{0,4}") ? ch : null;
            };
            waterEveryField.setTextFormatter(new TextFormatter<>(intFilter));
        }

        if (sunlightField != null) {
            // Non-negative decimal for "sunlight"
            UnaryOperator<TextFormatter.Change> dblFilter = ch -> {
                String txt = ch.getControlNewText();
                if (txt.isBlank()) return ch;
                return txt.matches("\\d{0,3}(\\.\\d{0,2})?") ? ch : null;
            };
            sunlightField.setTextFormatter(new TextFormatter<>(dblFilter));
        }

        if (photoField != null) {
            // Update preview when photo path changes
            photoField.textProperty().addListener((obs, old, v) -> updatePreview(v));

            // Enhanced drag & drop styling
            photoField.setOnDragOver(e -> {
                if (e.getDragboard().hasFiles()) {
                    e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                    photoField.setStyle(photoField.getStyle() + "; -fx-border-color: -color-primary-3; -fx-border-width: 2;");
                }
                e.consume();
            });

            photoField.setOnDragExited(e -> {
                // Reset styling
                photoField.setStyle(photoField.getStyle().replaceAll("; -fx-border-color: -color-primary-3; -fx-border-width: 2", ""));
                e.consume();
            });

            photoField.setOnDragDropped(e -> {
                var db = e.getDragboard();
                if (db.hasFiles() && !db.getFiles().isEmpty()) {
                    File f = db.getFiles().get(0);
                    if (isImageFile(f)) {
                        photoField.setText(f.getAbsolutePath());
                        updatePreview(f.getAbsolutePath());
                        e.setDropCompleted(true);
                        hideError();
                    } else {
                        showError("Please drop an image file (PNG, JPG, GIF, WEBP)");
                        e.setDropCompleted(false);
                    }
                } else {
                    e.setDropCompleted(false);
                }
                // Reset styling
                photoField.setStyle(photoField.getStyle().replaceAll("; -fx-border-color: -color-primary-3; -fx-border-width: 2", ""));
                e.consume();
            });
        }
    }

    private void setupPhotoPreview() {
        if (photoPreview != null) {
            // Set a subtle placeholder background
            photoPreview.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10px;");
        }
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".gif") || name.endsWith(".webp");
    }

    /** Load data into fields (or prepare empty form) */
    private void setPlant(Plant p) {
        this.working = (p == null ? new Plant() : p);
        if (title != null) title.setText(p == null ? "Add New Plant" : "Edit Plant");

        if (nameField != null) nameField.setText(nz(working.getName()));
        if (speciesField != null) speciesField.setText(nz(working.getSpecies()));
        if (waterEveryField != null) waterEveryField.setText(working.getWaterEveryDays() == null ? "" : String.valueOf(working.getWaterEveryDays()));
        if (sunlightField != null) sunlightField.setText(working.getSunlightHours() == null ? "" : String.valueOf(working.getSunlightHours()));
        if (photoField != null) photoField.setText(nz(working.getPhotoPath()));
        if (lastWateredPicker != null) lastWateredPicker.setValue(working.getLastWatered());

        hideError();
        updatePreview(photoField != null ? photoField.getText() : null);
    }

    private void updatePreview(String path) {
        if (photoPreview == null) return;

        try {
            if (path == null || path.isBlank()) {
                photoPreview.setImage(null);
                return;
            }
            File f = new File(path);
            if (!f.exists()) {
                photoPreview.setImage(null);
                return;
            }

            if (!isImageFile(f)) {
                showError("Selected file is not a supported image format");
                photoPreview.setImage(null);
                return;
            }

            photoPreview.setImage(new Image(f.toURI().toString(), 148, 108, true, true, true));
            hideError(); // Clear any previous image-related errors
        } catch (Exception e) {
            photoPreview.setImage(null);
            showError("Error loading image: " + e.getMessage());
            System.err.println("Error loading image preview: " + e.getMessage());
        }
    }

    @FXML
    private void browsePhoto() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Plant Photo");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                    new FileChooser.ExtensionFilter("PNG files", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG files", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("GIF files", "*.gif"),
                    new FileChooser.ExtensionFilter("WebP files", "*.webp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File f = fc.showOpenDialog(stage);
            if (f != null && photoField != null) {
                if (isImageFile(f)) {
                    photoField.setText(f.getAbsolutePath());
                    updatePreview(f.getAbsolutePath());
                    hideError();
                } else {
                    showError("Please select an image file (PNG, JPG, GIF, WEBP)");
                }
            }
        } catch (Exception e) {
            showError("Failed to browse for photo: " + e.getMessage());
        }
    }

    @FXML
    private void save() {
        try {
            hideError(); // Clear any previous errors

            // Required name
            String name = nz(nameField != null ? nameField.getText() : "").trim();
            if (name.isBlank()) {
                showError("Plant name is required.");
                if (nameField != null) nameField.requestFocus();
                return;
            }

            // Optional species
            String species = blankToNull(nz(speciesField != null ? speciesField.getText() : "").trim());

            // Optional cadence
            Integer waterEvery = null;
            String w = nz(waterEveryField != null ? waterEveryField.getText() : "").trim();
            if (!w.isBlank()) {
                try {
                    int v = Integer.parseInt(w);
                    if (v <= 0) throw new IllegalArgumentException("Must be positive");
                    waterEvery = v;
                } catch (Exception e) {
                    showError("Water frequency must be a positive number of days.");
                    if (waterEveryField != null) waterEveryField.requestFocus();
                    return;
                }
            }

            // Optional sunlight
            Double sunlight = null;
            String s = nz(sunlightField != null ? sunlightField.getText() : "").trim();
            if (!s.isBlank()) {
                try {
                    double v = Double.parseDouble(s);
                    if (v < 0) throw new IllegalArgumentException("Must be non-negative");
                    if (v > 24) throw new IllegalArgumentException("Cannot exceed 24 hours");
                    sunlight = v;
                } catch (Exception e) {
                    showError("Sunlight hours must be a number between 0 and 24.");
                    if (sunlightField != null) sunlightField.requestFocus();
                    return;
                }
            }

            // Optional photo - validate if provided
            String photo = blankToNull(nz(photoField != null ? photoField.getText() : "").trim());
            if (photo != null) {
                File photoFile = new File(photo);
                if (!photoFile.exists()) {
                    showError("Photo file does not exist. Please select a valid image file.");
                    if (photoField != null) photoField.requestFocus();
                    return;
                }
                if (!isImageFile(photoFile)) {
                    showError("Photo must be an image file (PNG, JPG, GIF, WEBP).");
                    if (photoField != null) photoField.requestFocus();
                    return;
                }
            }

            LocalDate lastWatered = lastWateredPicker != null ? lastWateredPicker.getValue() : null;

            // Apply to working
            working.setName(name);
            working.setSpecies(species);
            working.setWaterEveryDays(waterEvery);
            working.setSunlightHours(sunlight);
            working.setPhotoPath(photo);
            working.setLastWatered(lastWatered);

            result = working;
            stage.close();

        } catch (Exception e) {
            showError("Failed to save plant: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel() {
        result = null;
        stage.close();
    }

    private void showError(String msg) {
        if (errorLabel != null && errorContainer != null) {
            errorLabel.setText(msg);
            errorContainer.setVisible(true);
            errorContainer.setManaged(true);
        } else {
            System.err.println("Form error: " + msg);
        }
    }

    private void hideError() {
        if (errorLabel != null && errorContainer != null) {
            errorLabel.setText("");
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}