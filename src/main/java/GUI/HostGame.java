package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HostGame extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {

        //Parent root loaded from FXML file
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("host-game.fxml")));
        Scene scene = new Scene(root, 1920, 1080);
//        scene.getStylesheets().add(getClass().getResource("HomePageStyling.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }
}
