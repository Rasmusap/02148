package GUI;

import Sketchify.ChatServer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jspace.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class LoginController {
    RemoteSpace chatSpace;
    RemoteSpace drawSpace;
    RemoteSpace gameSpace;

    @FXML
    private Text Title;

    @FXML
    private Button enterGameButton;

    @FXML
    private Label enterUsernamePrompt;

    @FXML
    TextField userNameTF;

    private Parent root;
    private Stage stage;
    private Scene scene;
    private String myUsername;

    public void enterGame(ActionEvent event) throws IOException, InterruptedException {
        myUsername = userNameTF.getText();
        if (myUsername.isEmpty()) {
            enterUsernamePrompt.setText("No username entered! please enter username");
            enterUsernamePrompt.setAlignment(Pos.CENTER);
            return; // Stop here so the user can try again
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("home-page.fxml"));
        root = loader.load();

        HomepageController homepageController = loader.getController();


        //Parent root loaded from FXML file
        String chatURI = "tcp://192.168.8.69:8753/chat?keep";
        String serverURI = "tcp://192.168.8.69:8753/draw?keep";
        String gameURI = "tcp://192.168.8.69:8753/game?keep";
        try {
            chatSpace = new RemoteSpace(chatURI);
            drawSpace = new RemoteSpace(serverURI);
            gameSpace = new RemoteSpace(gameURI);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            gameSpace.put("user", myUsername);
            System.out.println("[App] Put (\"user\", " + myUsername + ") in gameSpace.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        homepageController.displayName(gameSpace);
        homepageController.setSpaces(chatSpace, gameSpace, drawSpace, myUsername);

        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
