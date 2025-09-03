package org.plantagonist.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.plantagonist.core.auth.CurrentUser;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.services.AuthService;

import java.util.Arrays;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;          // (hook up in FXML)
    @FXML private ProgressIndicator spinner;      // (optional, hook up in FXML)

    private final AuthService auth = new AuthService();
    private static final Pattern EMAIL_RX =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    @FXML
    private void onRegister() {
        errorLabel.setText("");

        final String email = emailField.getText().trim();
        final String username = usernameField.getText().trim();
        final char[] password = passwordField.getText().toCharArray();
        final char[] confirmPassword = confirmPasswordField.getText().toCharArray();

        try {
            // validations...
            if (email.isEmpty() || username.isEmpty()) { errorLabel.setText("Email and username are required."); return; }
            if (!EMAIL_RX.matcher(email).matches())   { errorLabel.setText("Please enter a valid email address."); return; }
            if (password.length < 6)                  { errorLabel.setText("Password must be at least 6 characters."); return; }
            if (!java.util.Arrays.equals(password, confirmPassword)) {
                errorLabel.setText("Passwords do not match."); return;
            }

            // IMPORTANT: make a COPY for the worker to consume
            final char[] pwdForWorker = java.util.Arrays.copyOf(password, password.length);

            Task<UserProfile> task = new Task<>() {
                @Override protected UserProfile call() throws Exception {
                    return auth.register(email, username, pwdForWorker);
                }
            };

            task.setOnRunning(e -> {
                if (spinner != null) spinner.setVisible(true);
                if (registerButton != null) registerButton.setDisable(true);
            });

            task.setOnSucceeded(e -> {
                if (spinner != null) spinner.setVisible(false);
                if (registerButton != null) registerButton.setDisable(false);

                UserProfile newUser = task.getValue();
                CurrentUser.set(newUser);

                // Navigate
                UiRouter.showMainApp((Stage) emailField.getScene().getWindow());

                // Clear UI fields (NOW it's safe)
                passwordField.clear();
                confirmPasswordField.clear();
            });

            task.setOnFailed(e -> {
                if (spinner != null) spinner.setVisible(false);
                if (registerButton != null) registerButton.setDisable(false);

                Throwable ex = task.getException();
                errorLabel.setText(ex != null ? ex.getMessage() : "Registration failed.");
                // Also clear UI fields here
                passwordField.clear();
                confirmPasswordField.clear();
            });

            new Thread(task, "register-user").start();

        } finally {
            // DO NOT wipe the arrays here — it races with the background task.
            // If you want to be extra cautious, you can wipe these AFTER setOnSucceeded / setOnFailed,
            // but the UI fields are already cleared there.
            java.util.Arrays.fill(confirmPassword, '\0'); // optional
            // (pwdForWorker is wiped inside auth.register; see next section)
        }
    }


    @FXML
    private void onGoLogin() {
        UiRouter.showLogin((Stage) emailField.getScene().getWindow());
    }
}
