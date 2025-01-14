package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneController {

    public void switchToScene(ActionEvent event, String sceneTo, String cssNewScene, String title) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(sceneTo)));


        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        String css = this.getClass().getResource(cssNewScene).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle(title);


        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public void switchToGame(ActionEvent event) throws IOException {
        SketchifyGame sketchifyGame = new SketchifyGame();
        Stage stage = new Stage();
        sketchifyGame.start(stage);
    }
}
