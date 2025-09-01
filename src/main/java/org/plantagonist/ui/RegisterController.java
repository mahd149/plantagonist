package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.plantagonist.core.auth.CurrentUser;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.services.AuthService;

public class RegisterController {

    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void onRegister() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        char[] password = passwordField.getText().toCharArray();
        char[] confirmPassword = confirmPasswordField.getText().toCharArray();

        // Validate input fields
        if (email.isEmpty() || username.isEmpty()) {
            errorLabel.setText("Email and username are required.");
            return;
        }

        if (password.length < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }

        if (!new String(password).equals(new String(confirmPassword))) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        try {
            // Register the user
            UserProfile newUser = auth.register(email, username, password);

            // Set the current user
            CurrentUser.set(newUser);

            // CHANGE: Navigate to the main application instead of dashboard
            UiRouter.showMainApp((Stage) emailField.getScene().getWindow());

        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        } finally {
            // Clean up password field (security)
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmPassword, '\0');
        }
    }

    @FXML
    private void onGoLogin() {
        // Switch to login screen
        UiRouter.showLogin((Stage) emailField.getScene().getWindow());
    }
}