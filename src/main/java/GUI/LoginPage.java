package GUI;

import Sketchify.ChatServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginPage extends Application {

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login-page.fxml")));
        Scene scene = new Scene(root);
//        scene.getStylesheets().add(getClass().getResource("HomePageStyling.css").toExternalForm());

        //css style sheet added
        String homePageCss = this.getClass().getResource("LoginStyle.css").toExternalForm();
        scene.getStylesheets().add(homePageCss);

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.setResizable(false);
        ChatServer server = new ChatServer();
        server.startServer();
        stage.show();
    }
}
