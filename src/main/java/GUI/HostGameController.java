package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.List;
import java.util.Scanner;

import org.jspace.*;

import java.io.IOException;

public class HostGameController {
    RemoteSpace chatSpace;
    RemoteSpace drawSpace;
    RemoteSpace gameSpace;

    private Parent root;
    private Stage stage;
    private Scene scene;
    @FXML
    private Button StartGameButton;
    public void InvitePlayers(ActionEvent e) {
        System.out.println("Inviting Players...");
    }

    public void StartGame(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sketchify-page.fxml"));
        root = loader.load();
        try {
            SketchifyController sketchifyController = loader.getController();
            sketchifyController.setSpaces(chatSpace, gameSpace, drawSpace);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void GoBack(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        String title = "Home page";
        String css = "HomePageStyle.css";
        String fxml = "home-page.fxml";
        controller.switchToScene(e, fxml, css, title);
    }

    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn) throws InterruptedException {
        chatSpace = chatSpaceIn;
        gameSpace = gameSpaceIn;
        drawSpace = drawSpaceIn;
        System.out.println("Host game chatSpace contain "
                + chatSpace.queryAll().toString()
                + " and gameSpace "
                + gameSpace.queryAll(new ActualField("user"), new FormalField(String.class)).toString()
                + " and drawSpace "
                + drawSpace.queryAll().toString());
    }
}
