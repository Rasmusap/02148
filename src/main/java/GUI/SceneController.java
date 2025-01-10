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
    private Stage stage;
    private Scene scene;
    private Parent root;

    public void switchToScene(ActionEvent event, String sceneTo, String cssNewScene, String title) throws IOException {
        root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(sceneTo)));


        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);

        String css = this.getClass().getResource(cssNewScene).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle(title);


        stage.setScene(scene);
        stage.show();
    }
}
