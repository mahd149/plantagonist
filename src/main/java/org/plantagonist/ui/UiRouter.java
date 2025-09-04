package org.plantagonist.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class UiRouter {

    private UiRouter() {}

    // Show Login screen
    public static void showLogin(Stage stage) {
        stage.setScene(new Scene(load("login.fxml")));
        stage.centerOnScreen();
        stage.show();
    }

    // Show Main application screen (with MainController)
    public static void showMainApp(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(UiRouter.class.getResource("/org/plantagonist/ui/main.fxml"));
            Scene scene = new Scene(loader.load(), 1080, 700);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load main application", e);
        }
    }

    // Show Registration screen
    public static void showRegister(Stage stage) {
        stage.setScene(new Scene(load("register.fxml")));
        stage.centerOnScreen();
        stage.show();
    }

    // Show Care Log screen
    public static void showCareLog(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(UiRouter.class.getResource("/org/plantagonist/ui/care_log.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            stage.setScene(scene);
            stage.setTitle("Plant Care Log");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load care log", e);
        }
    }

    // Show Care Log screen from existing stage (for navigation from main app)
    public static void showCareLog() {
        Stage stage = new Stage();
        try {
            FXMLLoader loader = new FXMLLoader(UiRouter.class.getResource("/org/plantagonist/ui/care_log.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            stage.setScene(scene);
            stage.setTitle("Plant Care Log");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load care log", e);
        }
    }

    private static Parent load(String fxml) {
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