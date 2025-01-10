package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class HomepageController {
    @FXML
    public void HostGame(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        String fxml = "host-game.fxml";
        String css = "HostGameStyle.css";
        String title = "Host Game";
        controller.switchToScene(e, fxml, css, title);
    }

    public void JoinGame(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        String fxml = "join-game.fxml";
        String css = "JoinGameStyle.css";
        String title = "Join Game";
        controller.switchToScene(e, fxml, css, title);
    }

    public void Exit(ActionEvent e) {
        System.out.println("Exiting...");
    }

}