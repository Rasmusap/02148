package GUI;

import javafx.application.Platform;
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

public class HomepageController {
    private String myRole;
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
    @FXML
    private Button HostGameButton;
    public void HostGame(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("host-game.fxml"));
        Parent root = loader.load();

        HostGameController controller = loader.getController();
        controller.setSpaces(chatSpace, gameSpace, drawSpace, currentUserName, myRole);
        controller.setMyRole(myRole);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        String css = this.getClass().getResource("HostGameStyle.css").toExternalForm();
        scene.getStylesheets().add(css);
        stage.setTitle("Host Game");
        stage.show();
    }

    public void JoinGame(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("join-game.fxml"));
        Parent root = loader.load();

        JoinGameController controller = loader.getController();
        controller.setSpaces(chatSpace, gameSpace, drawSpace, currentUserName, myRole);
        controller.setMyRole(myRole);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        String css = this.getClass().getResource("JoinGameStyle.css").toExternalForm();
        scene.getStylesheets().add(css);
        stage.setTitle("Join Game");
        stage.show();
    }

    public void Exit(ActionEvent e) {
        Stage stage = (Stage) ExitButton.getScene().getWindow();
        stage.close();
    }

    public void displayName(RemoteSpace gameSpace) {
        try {
            nameLabel.setText("Welcome " + gameSpace.query(new ActualField("user"), new FormalField(String.class))[1].toString());
            if (myRole.equals("Client")) {
                HostGameButton.setVisible(false);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn, String myUsername, String role)
            throws InterruptedException {
        myRole = role;
        currentUserName = myUsername;
        System.out.println(currentUserName);
        chatSpace = chatSpaceIn;
        gameSpace = gameSpaceIn;
        drawSpace = drawSpaceIn;

        startListeningForStartAll();
    }
    private void startListeningForStartAll() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // We do a blocking read for ("game","startAll")
                    gameSpace.query(new ActualField("game"), new ActualField("startAll"));

                    // If we get here, the host has signaled "startAll".
                    System.out.println("[HomePageController] Detected (\"game\",\"startAll\"). Loading Sketchify...");

                    // Switch to Sketchify UI on JavaFX thread
                    Platform.runLater(() -> {
                        switchToSketchify();
                    });
                    break; // or keep listening if you want multiple re-joins
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "WaitForStartAll");
        t.setDaemon(true);
        t.start();
    }

    /** Moves this client to 'sketchify-page.fxml' */
    private void switchToSketchify() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("sketchify-page.fxml"));
            Parent root = loader.load();

            SketchifyController controller = loader.getController();
            controller.setSpaces(chatSpace, gameSpace, drawSpace, currentUserName, myRole);

            stage = (Stage) ExitButton.getScene().getWindow(); // or any node from the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMyRole(String role) {
        this.myRole = role;
    }
}