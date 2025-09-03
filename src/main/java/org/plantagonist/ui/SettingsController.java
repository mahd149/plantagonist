package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.services.UserProfileService;

public class SettingsController {

    // User profile fields
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField cityField;

    // Achievements
    @FXML private Label streakLabel;
    @FXML private Label badgeLabel;

    private UserProfile userProfile;

    @FXML
    public void initialize() {
        // Load user profile
        userProfile = UserProfileService.loadUserProfile();

        // Populate fields
        usernameField.setText(userProfile.getUsername());
        emailField.setText(userProfile.getEmail());
        cityField.setText(userProfile.getCity());

        // Display achievements (placeholder for now, can later fetch from DB)
        streakLabel.setText(userProfile.getStreakDays() + " Days");
        badgeLabel.setText(userProfile.getBadgeCount() + " Badges");
    }

    // Save changes
    @FXML
    private void handleSaveProfile() {
        try {
            userProfile.setUsername(usernameField.getText().trim());
            userProfile.setEmail(emailField.getText().trim());
            userProfile.setCity(cityField.getText().trim());

            UserProfileService.saveUserProfile(userProfile);

            showAlert(AlertType.INFORMATION, "Success", "Profile updated successfully!");
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to save profile: " + e.getMessage());
        }
    }

    // Logout
    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.close();
            UiRouter.showLogin(new Stage());
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Logout failed: " + e.getMessage());
        }
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
