package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class HostGameController {

    private final String fxml = "home-page.fxml";
    private final String css = "HomePageStyle.css";
    private final String title = "Home page";

    @FXML
    public void InvitePlayers(ActionEvent e) {
        System.out.println("Inviting Players...");
    }

    public void StartGame(ActionEvent e) {
        System.out.println("Starting Game...");
    }

    public void GoBack(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        controller.switchToScene(e, fxml, css, title);
    }
}
