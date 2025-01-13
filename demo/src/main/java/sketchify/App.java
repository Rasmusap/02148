package sketchify;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class App extends Application {

    private Set<String> generatedWords = new HashSet<>();

    String word1 = generateRandomWord();
    String word2 = generateRandomWord();
    String word3 = generateRandomWord();
    String selectedWord = "";
    private int lastCount = 0;
    
    private RemoteSpace space;
    private RemoteSpace drawSpace;
    private Canvas canvas;
    private GraphicsContext gc;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String drawURI = "tcp://10.209.248.40:8753/draw?keep";
        try {
            drawSpace = new RemoteSpace(drawURI);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        primaryStage.setTitle("Sketchify");
        primaryStage.setMaximized(true);

        BorderPane root = new BorderPane();

        HBox top = new HBox();
        top.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        top.setPrefHeight(100);

        Button label1 = new Button(word1);
        Button label2 = new Button(word2);
        Button label3 = new Button(word3);

        top.setPadding(new Insets(0, 182, 0, 0));

        label1.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");
        label2.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");
        label3.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");

        top.setSpacing(20);
        top.setAlignment(Pos.CENTER);
        
        HBox right = new HBox();
        right.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
        right.setPrefWidth(200);
        HBox bottom = new HBox();
        bottom.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        bottom.setPrefHeight(100);
        bottom.setSpacing(10);
        bottom.setAlignment(Pos.CENTER);
        HBox center = new HBox();
        center.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox centerVBox = new VBox();
        centerVBox.setAlignment(Pos.TOP_CENTER);
        centerVBox.setSpacing(10);

        Label wordlabel = new Label("");
        wordlabel.setStyle("-fx-font-size: 20px; -fx-text-fill: black;");

        canvas = new Canvas(1100, 530);
        gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        label1.setOnAction((e) -> {
            wordlabel.setText(word1);
            selectedWord = word1;
            HBox parent = (HBox) label1.getParent();
            parent.getChildren().removeAll(label1, label2, label3);
        });
        label2.setOnAction(e -> {
            wordlabel.setText(word2);
            selectedWord = word2;
            HBox parent = (HBox) label1.getParent();
            parent.getChildren().removeAll(label1, label2, label3);
        });
        label3.setOnAction(e -> {
            wordlabel.setText(word3);
            selectedWord = word3;
            HBox parent = (HBox) label1.getParent();
            parent.getChildren().removeAll(label1, label2, label3);
        });

        canvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
            try {
                drawSpace.put("draw", e.getX(), e.getY(), "start");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
        });

        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            try {
                drawSpace.put("draw", e.getX(), e.getY(), "draw");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
        });

        centerVBox.getChildren().addAll(wordlabel, canvas);
        center.getChildren().add(centerVBox);

        top.getChildren().addAll(label1, label2, label3);

        Button clear = new Button("Clear canvas");
        clear.setOnAction(event -> gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));

        bottom.getChildren().addAll(clear);

        root.setTop(top);
        root.setRight(right);
        root.setBottom(bottom);
        root.setCenter(center);

        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();

        Thread drawListener = new Thread(this::listenForDraws, "DrawListener");
        drawListener.setDaemon(true);
        drawListener.start();
    }

    private void listenForDraws() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> draws = drawSpace.queryAll(
                        new ActualField("draw"),
                        new FormalField(Double.class),
                        new FormalField(Double.class),
                        new FormalField(String.class)
                );
                if (draws.size() > lastCount) {
                    for (int i = lastCount; i < draws.size(); i++) {
                        Object[] tuple = draws.get(i);
                        double x = (double) tuple[1];
                        double y = (double) tuple[2];
                        String action = (String) tuple[3];

                        Platform.runLater(() -> {
                            if ("start".equals(action)) {
                                gc.beginPath();
                                gc.moveTo(x, y);
                                gc.stroke();
                            } else if ("draw".equals(action)) {
                                gc.lineTo(x, y);
                                gc.stroke();
                            }
                        });
                    }
                    lastCount = draws.size();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String generateRandomWord() {
        String randomWord = "";
        List<String> words = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\nicla\\OneDrive\\Dokumenter\\GitHub\\02148\\demo\\src\\words.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] wordsLine = line.split("\\s+");
                words.addAll(Arrays.asList(wordsLine));
            }

            if (words.isEmpty()) {
                throw new IllegalStateException("No words found in the file.");
            }

            Random rand = new Random();
            int attempts = 0;
            int maxAttempts = 100;
            boolean wordGenerated = false;

            do {
                if (attempts++ > maxAttempts) {
                    System.out.println("Max attempts reached, stopping word generation.");
                    break;
                }
                randomWord = words.get(rand.nextInt(words.size()));
                if (!generatedWords.contains(randomWord)) {
                    generatedWords.add(randomWord);
                    wordGenerated = true;
                }
            } while (!wordGenerated);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return randomWord;
    }
}
