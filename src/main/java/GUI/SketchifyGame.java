package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;


public class SketchifyGame extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("sketchify-page.fxml")));
        Scene scene = new Scene(root, 1080, 720);

        //css style sheet added
        String css = Objects.requireNonNull(
                getClass().getResource("SketchifyStyle.css")
        ).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Sketchify");
        stage.setScene(scene);

        stage.show();
    }
}
