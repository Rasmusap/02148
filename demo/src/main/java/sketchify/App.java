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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class App extends Application {

    private Set<String> generatedWords = new HashSet<>();

    String word1 = generateRandomWord();
    String word2 = generateRandomWord();
    String word3 = generateRandomWord();
    String selectedWord = "";
    double x = 0;
    double y = 0;
    int seconds = 60;
    boolean isGuessed = false;
    private int lastDrawCount = 0;
    private int lastChatCount = 0;
    
    private RemoteSpace drawSpace;
    private RemoteSpace chatSpace;

    private TextArea chatDisplay;
    private TextField chatInput;
    private Label wordlabel;
    private Label guessedField;
    private Label timerLabel;
    private Button sendBtn;
    private Button label1;
    private Button label2;
    private Button label3;
    private HBox top;

    private Canvas canvas;
    private GraphicsContext gc;
    private String actiontype = "";
    private Timeline timeline;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String chatURI = "tcp://192.168.0.247:8753/chat?keep";
        String serverURI = "tcp://192.168.0.247:8753/draw?keep";
        try {
            chatSpace = new RemoteSpace(chatURI);
            drawSpace = new RemoteSpace(serverURI);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        primaryStage.setTitle("Sketchify");
        primaryStage.setMaximized(true);

        BorderPane root = new BorderPane();

        
        top = new HBox();
        top.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        top.setPrefHeight(100);

        timerLabel = new Label();
        timerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        initializeTimeline(timerLabel);
    
        HBox.setHgrow(timerLabel, Priority.ALWAYS);

        guessedField = new Label();
        guessedField.setPrefWidth(250); 
        guessedField.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        HBox.setHgrow(guessedField, Priority.ALWAYS);

        label1 = new Button(word1);
        label2 = new Button(word2);
        label3 = new Button(word3);

        top.setPadding(new Insets(0, 182, 0, 0));

        label1.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");
        label2.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");
        label3.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");

        top.setSpacing(20);
        top.setAlignment(Pos.CENTER);
        
        HBox right = new HBox();
        right.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
        right.setPrefWidth(300);
        right.setPadding(new Insets(0, 20, 0, 0));

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

        wordlabel = new Label("");
        wordlabel.setStyle("-fx-font-size: 20px; -fx-text-fill: black;");
        
        VBox chatBox = new VBox(10);
        chatBox.setPadding(new Insets(10));
        chatBox.setPrefWidth(300);

        chatDisplay = new TextArea();
        chatDisplay.setEditable(false);
        chatDisplay.setWrapText(true);
        chatDisplay.setPrefHeight(220);

        ScrollPane chatScrollPane = new ScrollPane(chatDisplay);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(220);

        chatInput = new TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setPrefWidth(200);

        sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> sendChatMessage());

        chatBox.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendChatMessage();
            }
        });

        HBox chatInputBox = new HBox(10, chatInput, sendBtn);
        chatInputBox.setAlignment(Pos.CENTER);
        chatBox.getChildren().addAll(chatScrollPane, chatInputBox);

        right.getChildren().addAll(chatBox);

        canvas = new Canvas(1000, 530);
        gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        StackPane centerPane = new StackPane();
        centerPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        centerPane.widthProperty().addListener((obs, oldW, newW) -> {
            canvas.setWidth(newW.doubleValue());
        });
        centerPane.heightProperty().addListener((obs, oldH, newH) -> {
            canvas.setHeight(newH.doubleValue());
        });

        selectWord(word1, word2, word3);

        Draw draw = new Draw(x, y, actiontype);

        draw.isPressed(canvas, gc, drawSpace);

        draw.isDragged(canvas, gc, drawSpace);

        centerVBox.getChildren().addAll(wordlabel, canvas);
        center.getChildren().add(centerVBox);

        top.getChildren().addAll(label1, label2, label3, guessedField, timerLabel);

        Button clear = new Button("Clear canvas");
        clear.setOnAction(event -> gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));

        bottom.getChildren().addAll(clear);

        root.setTop(top);
        root.setRight(right);
        root.setBottom(bottom);
        root.setCenter(center);

        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();

        Thread chatListener = new Thread(this::listenForChatMessages, "ChatListener");
        chatListener.setDaemon(true);
        chatListener.start();

        Thread drawListener = new Thread(this::listenForDraws, "DrawListener");
        drawListener.setDaemon(true);
        drawListener.start();
    }

    private void selectWord(String word1, String word2, String word3) {
        label1.setOnAction((e) -> {
            wordlabel.setText(word1);
            selectedWord = word1;
            HBox parent = (HBox) label1.getParent();
            parent.getChildren().removeAll(label1, label2, label3);
            timeline.play();
        });
        label2.setOnAction(e -> {
            wordlabel.setText(word2);
            selectedWord = word2;
            HBox parent = (HBox) label2.getParent();
            parent.getChildren().removeAll(label1, label2, label3);
            timeline.play();
        });
        label3.setOnAction(e -> {
            wordlabel.setText(word3);
            selectedWord = word3;
            HBox parent = (HBox) label3.getParent();
            parent.getChildren().removeAll(label1, label2, label3);
            timeline.play();
        });
    }

    private void checkGuess(String message) {
        timeline.stop();
        if (message.trim().equalsIgnoreCase(selectedWord)) {
            guessedField.setText(getSender() + " has guessed the word right");
            isGuessed = true;
            chatInput.setEditable(false);
            sendBtn.setDisable(true);
            
            Timeline delayTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {    
                seconds = 61;
                timeline.playFromStart();
                generateNewRound();
            }));
            delayTimeline.setCycleCount(1);
            delayTimeline.play();
        }
    }

    private void generateNewRound() {
        isGuessed = false;
        word1 = generateRandomWord();
        word2 = generateRandomWord();
        word3 = generateRandomWord();

        label1.setText(word1);
        label2.setText(word2);
        label3.setText(word3);

        guessedField.setText("");
        chatInput.setEditable(true);
        sendBtn.setDisable(false);
        wordlabel.setText("");
        Platform.runLater(() -> {
            top.getChildren().addAll(label1, label2, label3); 
            selectWord(word1, word2, word3);
        });
    
        seconds = 61;
        timeline.playFromStart();
        timeline.stop();
    }

    private void initializeTimeline(Label timerLabel) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (seconds > 0) {
                seconds--;
                timerLabel.setText("Time: " + seconds + " s");
            } else {
                timerLabel.setText("Time's up!");
                timeline.stop();
            }
        }));
    
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    @Override
    public void stop() throws Exception {
        if (timeline != null) {
            timeline.stop();
        }
        super.stop();
    }

    private String getSender() {
        String sender = "";
        try {
            List<Object[]> messages = chatSpace.queryAll(
                new ActualField("message"),
                new FormalField(String.class),
                new FormalField(String.class)
            );
            if (messages.size() > lastChatCount) {
                for (int i = lastChatCount; i < messages.size(); i++) {
                    sender = (String) messages.get(i)[1];
                }
                lastChatCount = messages.size();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sender;
    }
 
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            try {
                chatSpace.put("message", text);
                checkGuess(text);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            chatInput.clear();
        }
    }
    
    private void listenForChatMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> messages = chatSpace.queryAll(
                    new ActualField("message"),
                    new FormalField(String.class)
                );
                if (messages.size() > lastChatCount) {
                    for (int i = lastChatCount; i < messages.size(); i++) {
                        String text = (String) messages.get(i)[1];
                        Platform.runLater(() -> {
                            chatDisplay.appendText(text + "\n");
                        });
                    }
                    lastChatCount = messages.size();
                }
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                if (draws.size() > lastDrawCount) {
                    for (int i = lastDrawCount; i < draws.size(); i++) {
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
                    lastDrawCount = draws.size();
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
