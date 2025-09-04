package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.services.UserProfileService;
import org.plantagonist.core.auth.CurrentUser; // if you have it

import java.util.regex.Pattern;

public class SettingsController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField cityField;

    @FXML private Label streakLabel;
    @FXML private Label badgeLabel;

    private UserProfile userProfile;

    private static final Pattern EMAIL_RX =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    @FXML
    public void initialize() {
        try {
            userProfile = UserProfileService.loadUserProfile();
            if (userProfile == null) userProfile = new UserProfile();

            // Null-safe populate
            if (usernameField != null) usernameField.setText(nz(userProfile.getUsername()));
            if (emailField != null)    emailField.setText(nz(userProfile.getEmail()));
            if (cityField != null)     cityField.setText(nz(userProfile.getCity()));

            if (streakLabel != null) streakLabel.setText(userProfile.getStreakDays() + " Days");
            if (badgeLabel != null)  badgeLabel.setText(userProfile.getBadgeCount() + " Badges");
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Load Error", "Could not load profile. Using defaults.");
            userProfile = new UserProfile();
        }
    }

    @FXML
    private void handleSaveProfile() {
        // Collect + normalize
        String username = safeGet(usernameField).trim();
        String email    = safeGet(emailField).trim().toLowerCase();
        String city     = safeGet(cityField).trim();

        // Basic validation
        if (username.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation", "Username cannot be empty.");
            return;
        }
        if (email.isEmpty() || !EMAIL_RX.matcher(email).matches()) {
            showAlert(AlertType.WARNING, "Validation", "Please enter a valid email address.");
            return;
        }

        // Persist
        try {
            userProfile.setUsername(username);
            userProfile.setEmail(email);
            userProfile.setCity(city);

            UserProfileService.saveUserProfile(userProfile);
            showAlert(AlertType.INFORMATION, "Success", "Profile updated successfully!");
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to save profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // clear current user session (if you have this concept)
            try { CurrentUser.clear(); } catch (Throwable ignored) {}

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

    private static String nz(String s) { return s == null ? "" : s; }
    private static String safeGet(TextField tf) { return tf == null ? "" : nz(tf.getText()); }
}
