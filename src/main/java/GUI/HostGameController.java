package GUI;

import java.io.IOException;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class HostGameController {
    private String currentUser;
    private String myRole;
    private List<Object[]> userList;
    RemoteSpace chatSpace;
    RemoteSpace drawSpace;
    RemoteSpace gameSpace;

    private Parent root;
    private Stage stage;
    private Scene scene;
    @FXML
    private TextArea lobbyTextArea;
    @FXML
    private Button StartGameButton;

    public void StartGame(ActionEvent event) throws IOException, InterruptedException {
        try {
            gameSpace.put("game", "startAll");
            System.out.println("[HostGameController] Put (\"game\", \"startAll\") in gameSpace.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("sketchify-page.fxml"));
        root = loader.load();
        try {
            SketchifyController sketchifyController = loader.getController();
            sketchifyController.setSpaces(chatSpace, gameSpace, drawSpace, currentUser, myRole);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void GoBack(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("home-page.fxml"));
        root = loader.load();

        HomepageController homepageController = loader.getController();
        homepageController.setMyRole(myRole);
        homepageController.displayName(gameSpace);
        homepageController.setSpaces(chatSpace, gameSpace, drawSpace, currentUser, myRole);

        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
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
            lobbyTextArea.setText(sb.toString());
            lobbyTextArea.setEditable(false);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
    public void setMyRole(String role) {
        this.myRole = role;
    }
}
