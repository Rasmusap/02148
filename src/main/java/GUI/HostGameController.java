package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.List;
import java.util.Scanner;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import java.io.IOException;

public class HostGameController {
    @FXML
    private Button StartGameButton;
    public void InvitePlayers(ActionEvent e) {
        System.out.println("Inviting Players...");
    }

    public void StartGame(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        controller.switchToGame(e);
        Stage stage = (Stage) StartGameButton.getScene().getWindow();
        stage.close();
    }

    public void GoBack(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        String title = "Home page";
        String css = "HomePageStyle.css";
        String fxml = "home-page.fxml";
        controller.switchToScene(e, fxml, css, title);
    }
}
