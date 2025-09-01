package org.plantagonist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.plantagonist.ui.UiRouter;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // at the top of start()
//        var url = App.class.getResource("/org/plantagonist/ui/main.fxml");
//        System.out.println("FXML URL -> " + url);
//        var loader = new javafx.fxml.FXMLLoader(java.util.Objects.requireNonNull(
//                url, "Missing on classpath: /org/plantagonist/ui/main.fxml"
//        ));
////
//         FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/plantagonist/ui/main.fxml"));
//        Scene scene = new Scene(loader.load(), 1080, 700);
        stage.setTitle("Plantagonist");
//        stage.setScene(scene);
      //  stage.show();


      UiRouter.showLogin(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
