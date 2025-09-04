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
        String id = emailOrUsername.getText() == null ? "" : emailOrUsername.getText().trim();
        char[] pwd = password.getText() == null ? new char[0] : password.getText().toCharArray();
        System.out.println("[LoginController] onLogin id='" + id + "' len(pwd)=" + pwd.length);

        try {
            UserProfile user = auth.login(id, pwd);
            System.out.println("[LoginController] login OK: " + user.getEmail());
            org.plantagonist.core.auth.CurrentUser.set(user);
            UiRouter.showMainApp((Stage) emailOrUsername.getScene().getWindow());
        } catch (Exception e) {
            System.out.println("[LoginController] login FAIL: " + e.getMessage());
            error.setText("Invalid credentials");
        } finally {
            java.util.Arrays.fill(pwd, '\0');
            password.clear();
        }
    }


    @FXML
    private void onGoRegister() {
        UiRouter.showRegister((Stage) emailOrUsername.getScene().getWindow());
    }
}