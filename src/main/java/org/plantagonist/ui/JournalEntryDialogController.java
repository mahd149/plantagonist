package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.plantagonist.core.models.JournalEntry;
import org.plantagonist.core.models.Plant;

import java.io.File;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import java.net.URL;

public class JournalEntryDialogController {

    @FXML private TextArea contentTextArea;
    @FXML private ImageView photoImageView;
    @FXML private Label dateLabel;
    @FXML private Label plantNameLabel;
    @FXML private Button uploadPhotoBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Button wateredBtn;
    @FXML private Button fertilizedBtn;
    @FXML private Button prunedBtn;
    @FXML private Button repottedBtn;

    private File selectedPhotoFile;
    private Plant selectedPlant;
    private Consumer<JournalEntry> onSaveCallback;

    public static void openDialog(Stage owner, Plant plant, Consumer<JournalEntry> onSaveCallback) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    JournalEntryDialogController.class.getResource("/org/plantagonist/ui/journal_entry_form.fxml")
            );
            VBox dialogPane = loader.load();

            JournalEntryDialogController controller = loader.getController();
            controller.selectedPlant = plant;
            controller.onSaveCallback = onSaveCallback;
            controller.initializeDialog();

            // Load CSS from the correct path
            try {
                URL cssUrl = JournalEntryDialogController.class.getResource("/css/journal.css");
                if (cssUrl != null) {
                    dialogPane.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("CSS file not found at: /css/journal.css");
                    // Fallback: check if it's in the UI folder
                    URL altCssUrl = JournalEntryDialogController.class.getResource("/org/plantagonist/ui/css/journal.css");
                    if (altCssUrl != null) {
                        dialogPane.getStylesheets().add(altCssUrl.toExternalForm());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading CSS: " + e.getMessage());
            }

            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle("Add Journal Entry");

            Scene scene = new Scene(dialogPane);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void initializeDialog() {
        // Set current date
        dateLabel.setText(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")));

        // Set plant name if available
        if (selectedPlant != null) {
            plantNameLabel.setText(selectedPlant.getName());
        } else {
            plantNameLabel.setText("General Entry");
        }

        // Initialize photo image view
        photoImageView.setVisible(false);

        // Set up event handlers
        uploadPhotoBtn.setOnAction(e -> handlePhotoUpload());
        saveBtn.setOnAction(e -> handleSave());
        cancelBtn.setOnAction(e -> handleCancel());

        // Initialize quick actions
        initializeQuickActions();
    }

    private void initializeQuickActions() {
        wateredBtn.setOnAction(e -> appendToContent("💧 Watered plant"));
        fertilizedBtn.setOnAction(e -> appendToContent("🌱 Fertilized plant"));
        prunedBtn.setOnAction(e -> appendToContent("✂️ Pruned plant"));
        repottedBtn.setOnAction(e -> appendToContent("🪴 Repotted plant"));
    }

    private void appendToContent(String text) {
        String current = contentTextArea.getText();
        if (!current.isEmpty() && !current.endsWith("\n") && !current.endsWith(" ")) {
            current += "\n";
        }
        contentTextArea.setText(current + text + "\n");
        contentTextArea.positionCaret(contentTextArea.getText().length());
    }

    private void handlePhotoUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Plant Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(uploadPhotoBtn.getScene().getWindow());
        if (file != null) {
            selectedPhotoFile = file;
            try {
                Image image = new Image(file.toURI().toString());
                photoImageView.setImage(image);
                photoImageView.setVisible(true);

                // Add photo note to content
                appendToContent("📷 Added photo: " + file.getName());
            } catch (Exception e) {
                showAlert("Error", "Could not load the selected image.");
                e.printStackTrace();
            }
        }
    }

    private void handleSave() {
        if (contentTextArea.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please write something in your journal entry.");
            return;
        }

        JournalEntry entry = new JournalEntry();
        entry.setContent(contentTextArea.getText().trim());
        entry.setEntryDate(LocalDateTime.now());

        if (selectedPhotoFile != null) {
            entry.setPhotoPath(selectedPhotoFile.getAbsolutePath());
        }

        if (selectedPlant != null) {
            entry.setPlantId(selectedPlant.getId());
            entry.setPlantName(selectedPlant.getName());
        }

        if (onSaveCallback != null) {
            onSaveCallback.accept(entry);
        }

        closeDialog();
    }

    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}