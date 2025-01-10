package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class JoinGameController {
    private final String fxml = "home-page.fxml";
    private final String css = "HomePageStyle.css";
    private final String title = "Home page";

    @FXML
    public void joinDirectly(ActionEvent e) throws IOException {
        System.out.println("Joining Directly...");
    }

    public void findGame(ActionEvent e) throws IOException {
        System.out.println("Loading games...");
    }

    public void GoBack(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        controller.switchToScene(e, fxml, css, title);
    }
}
