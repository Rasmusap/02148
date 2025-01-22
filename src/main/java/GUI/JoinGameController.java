package GUI;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class JoinGameController {
    @FXML
    private TextArea onlineTextArea;
    private String currentUser;
    private String myRole;
    private List<Object[]> userList;
    RemoteSpace chatSpace;
    RemoteSpace drawSpace;
    RemoteSpace gameSpace;

    public void GoBack(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("home-page.fxml"));
        Parent root = loader.load();

        HomepageController homepageController = loader.getController();
        homepageController.setMyRole(myRole);
        homepageController.displayName(gameSpace);
        homepageController.setSpaces(chatSpace, gameSpace, drawSpace, currentUser, myRole);


        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn, String myUsername, String role)
            throws InterruptedException {
        this.myRole = role;
        this.currentUser = myUsername;
        System.out.println(currentUser);
        chatSpace = chatSpaceIn;
        gameSpace = gameSpaceIn;
        drawSpace = drawSpaceIn;
//        System.out.println("Host game chatSpace contain "
//                + chatSpace.queryAll().toString()
//                + " and gameSpace "
//                + gameSpace.queryAll(new ActualField("user"), new FormalField(String.class)).toString()
//                + " and drawSpace "
//                + drawSpace.queryAll().toString());

        updateLobbyList();
        startListeningForStartAll();
    }
    public void updateLobbyList() {
        try {
            // Query all tuples of the form ("user", <String>)
            userList = gameSpace.queryAll(
                    new ActualField("user"),
                    new FormalField(String.class)
            );

            // Build a display string
            StringBuilder sb = new StringBuilder();
            for (Object[] userTuple : userList) {
                String username = (String) userTuple[1];
                sb.append(username).append("\n");
            }

            // Show them in the lobbyTextArea
            onlineTextArea.setText(sb.toString());
            onlineTextArea.setEditable(false);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
    private void startListeningForStartAll() {
        if (Objects.equals(myRole, "Client")) {
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
    }
    private void switchToSketchify() {
        if (Objects.equals(myRole, "Client")) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("sketchify-page.fxml"));
                Parent root = loader.load();

                SketchifyController controller = loader.getController();
                controller.setSpaces(chatSpace, gameSpace, drawSpace, currentUser, myRole);

                Stage stage = (Stage) onlineTextArea.getScene().getWindow(); // or any node from the scene
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void setMyRole(String role) {
        this.myRole = role;
    }
}
