package org.plantagonist.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class UiRouter {

    private UiRouter() {}

    // Show Login screen
    public static void showLogin(Stage stage) {
        stage.setScene(new Scene(load("login.fxml")));
        stage.centerOnScreen(); // ← ADD THIS

        stage.show();
    }

    // Show Main application screen (with MainController)
    public static void showMainApp(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(UiRouter.class.getResource("/org/plantagonist/ui/main.fxml"));
            Scene scene = new Scene(loader.load(), 1080, 700);
            stage.setScene(scene);
            stage.centerOnScreen(); // ← ADD THIS

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load main application", e);
        }
    }

    // Show Registration screen
    public static void showRegister(Stage stage) {
        stage.setScene(new Scene(load("register.fxml")));
        stage.centerOnScreen(); // ← ADD THIS

        stage.show();
    }

    private static javafx.scene.Parent load(String fxml) {
        try {
            return FXMLLoader.load(UiRouter.class.getResource("/org/plantagonist/ui/" + fxml));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load FXML", e);
        }
    }
    // Show Settings screen
    public static void showSettings(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(UiRouter.class.getResource("/org/plantagonist/ui/settings.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);
            stage.setScene(scene);
            stage.setTitle("Settings - Plantagonist");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load settings", e);
        }
    }
}