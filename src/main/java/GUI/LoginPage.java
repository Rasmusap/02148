package GUI;

import Sketchify.ChatServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Objects;

public class LoginPage extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Check if there's already a server by attempting a client connection
        String testURI = "tcp://127.0.0.1:8753/chat?keep";
        boolean serverRunning = isServerRunning(testURI);

        // 2. If no server is running, start a new one
        if (!serverRunning) {
            ChatServer server = new ChatServer();
            server.startServer();
            System.out.println("[LoginPage] Started a new ChatServer on port 8753.");
        } else {
            System.out.println("[LoginPage] Server already running. Skipping new ChatServer startup.");
        }

        // 3. Load the login-page.fxml
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource("login-page.fxml"))
        );

        Scene scene = new Scene(root);

        // Add CSS
        String homePageCss = Objects.requireNonNull(
                this.getClass().getResource("LoginStyle.css")
        ).toExternalForm();
        scene.getStylesheets().add(homePageCss);

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Attempts to connect to the space at 'uri'.
     * If successful, returns true (server is running).
     * If it fails with IOException/ConnectException, returns false.
     */
    private boolean isServerRunning(String uri) {
        try {
            RemoteSpace testSpace = new RemoteSpace(uri);
            // If we get this far without an exception, the server is up
            testSpace.queryp(); // do a harmless operation
            return true;
        } catch (IOException e) {
            // Could be ConnectException, etc.
            System.out.println("[LoginPage] No server found at " + uri);
            return false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

