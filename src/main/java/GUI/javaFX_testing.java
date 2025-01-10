package GUI;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;


public class javaFX_testing extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Scenebuilder root
//        Parent sceneBuilderRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("sample.fxml")));
        // Root
        Group root = new Group();
        //Scene
        Scene scene = new Scene(root, 1920, 1080, Color.WHITESMOKE);

        //Button
        Button btn1 = new Button("Hello World");
        btn1.setCenterShape(true);

        // Rectangle
        Rectangle rect = new Rectangle();
        rect.setX(scene.getWidth() / 2 - 50);
        rect.setY(scene.getHeight() / 2 - 50);
        rect.setWidth(100);
        rect.setHeight(100);

        // Text
//        Text text = new Text();
//        text.setText("Center");
//        text.setX(scene.getWidth() / 2 - 50);
//        text.setY(scene.getHeight() / 2 - 50);
//        text.setFont(Font.font("Baskerville", 50));
//        text.setFill(Color.LIME);

        // Line
        Line line = new Line();
        line.setStartX(960);
        line.setStartY(0);
        line.setEndX(960);
        line.setEndY(1080);
        line.setStrokeWidth(1);
        line.setStroke(Color.BLUE);
        Line line2 = new Line();
        line2.setStartX(0);
        line2.setStartY(540);
        line2.setEndX(1920);
        line2.setEndY(540);
        line2.setStrokeWidth(1);
        line2.setStroke(Color.BLUE);

        // Add to root.
        root.getChildren().add(rect);
        root.getChildren().add(btn1);
//        root.getChildren().add(text);
        root.getChildren().add(line);
        root.getChildren().add(line2);

        //Scene resize ability.
//        primaryStage.setFullScreenExitKeyCombination(KeyCombination.valueOf("esc"));
//        primaryStage.setTitle("Hello World button");

        // Add scene to stage.
        primaryStage.setResizable(false);
        primaryStage.setFullScreen(false);


        //Show stage
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
