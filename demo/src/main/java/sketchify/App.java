package sketchify;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {
    public static void main( String[] args ){
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        primaryStage.setMaximized(true);
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction((ActionEvent event) -> {
            System.out.println("Hello World!");
        });
        
        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();
    }
}
