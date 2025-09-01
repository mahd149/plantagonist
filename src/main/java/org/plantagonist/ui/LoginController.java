package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.plantagonist.core.auth.CurrentUser;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.services.AuthService;

public class LoginController {

    @FXML private TextField emailOrUsername;
    @FXML private PasswordField password;
    @FXML private Label error;

    private final AuthService auth = new AuthService();

    @FXML
    private void onLogin() {
        String email = emailOrUsername.getText().trim();
        char[] passwordChars = password.getText().toCharArray();

        try {
            UserProfile user = auth.login(email, passwordChars);
            CurrentUser.set(user);

            // CHANGE THIS LINE: Show main application instead of dashboard
            UiRouter.showMainApp((Stage) emailOrUsername.getScene().getWindow());

        } catch (Exception e) {
            error.setText(e.getMessage());
        } finally {
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }

    @FXML
    private void onGoRegister() {
        UiRouter.showRegister((Stage) emailOrUsername.getScene().getWindow());
    }
}