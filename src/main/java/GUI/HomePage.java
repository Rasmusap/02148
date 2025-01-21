//package GUI;
//
//import javafx.event.ActionEvent;
//import javafx.fxml.FXMLLoader;
//import javafx.fxml.Initializable;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import org.jspace.ActualField;
//import org.jspace.FormalField;
//import org.jspace.RemoteSpace;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.List;
//import java.util.Objects;
//import java.util.ResourceBundle;
//
//public class HomePage extends javafx.application.Application{
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage stage) throws IOException {
////        Parent root loaded from FXML file
//        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("home-page.fxml")));
//        Scene scene = new Scene(root, 1920, 1080);
////        scene.getStylesheets().add(getClass().getResource("HomePageStyling.css").toExternalForm());
//
//        //css style sheet added
//        String homePageCss = this.getClass().getResource("HomePageStyle.css").toExternalForm();
//        scene.getStylesheets().add(homePageCss);
//
//        stage.setTitle("Home Page");
//        stage.setScene(scene);
//        stage.setResizable(false);
//
//        stage.show();
//    }
//}