package GUI;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneController {

    public void switchToScene(ActionEvent event, String sceneTo, String cssNewScene, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(sceneTo));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        String css = this.getClass().getResource(cssNewScene).toExternalForm();
        scene.getStylesheets().add(css);
        stage.setTitle(title);
        stage.show();
    }

    public void switchToGame(ActionEvent event) throws IOException {
        SketchifyGame sketchifyGame = new SketchifyGame();
        Stage stage = new Stage();
        sketchifyGame.start(stage);
    }
}
