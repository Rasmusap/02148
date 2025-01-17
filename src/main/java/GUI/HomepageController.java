package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.jspace.*;

import java.io.IOException;
import java.util.Currency;
import java.util.List;
import java.util.Scanner;

public class HomepageController {
    private Parent root;
    private Scene scene;
    private Stage stage;
    RemoteSpace gameSpace;
    RemoteSpace drawSpace;
    RemoteSpace chatSpace;
    String currentUserName = "";


    @FXML
    Label nameLabel;

    @FXML
    public Button ExitButton;
    public void HostGame(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("host-game.fxml"));
        Parent root = loader.load();

        HostGameController controller = new HostGameController();
        controller.setSpaces(chatSpace, gameSpace, drawSpace);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        String css = this.getClass().getResource("HostGameStyle.css").toExternalForm();
        scene.getStylesheets().add(css);
        stage.setTitle("Host Game");
        stage.show();
    }

    public void JoinGame(ActionEvent e) throws IOException {
        SceneController controller = new SceneController();
        String fxml = "join-game.fxml";
        String css = "JoinGameStyle.css";
        String title = "Join Game";
        controller.switchToScene(e, fxml, css, title);
    }

    public void Exit(ActionEvent e) {
        Stage stage = (Stage) ExitButton.getScene().getWindow();
        stage.close();
    }

    public void displayName(RemoteSpace gameSpace) {
        try {
            nameLabel.setText("Welcome " + gameSpace.query(new ActualField("user"), new FormalField(String.class))[1].toString());
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn) {
        chatSpace = chatSpaceIn;
        gameSpace = gameSpaceIn;
        drawSpace = drawSpaceIn;
    }
}