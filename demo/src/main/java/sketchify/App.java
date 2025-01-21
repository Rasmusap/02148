package sketchify;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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

    String selectedWord = "";
    String currentDrawer = "";
    double x = 0;
    double y = 0;
    int seconds = 60;
    boolean isGuessed = false;
    boolean isStarted = false;
    private int lastDrawCount = 0;
    private int lastChatCount = 0;
    private int lastUserCount = 0;
    
    private RemoteSpace drawSpace;
    private RemoteSpace chatSpace;
    private RemoteSpace gameSpace;

    private TextArea chatDisplay;
    private TextField chatInput;
    private Label wordlabel;
    private Label guessedField;
    private Label timerLabel;
    private Button sendBtn;
    private Button label1;
    private Button label2;
    private Button label3;
    private Button start;
    private HBox top;
    private String myUsername;
    private String chosenDrawer;

    private Canvas canvas;
    private GraphicsContext gc;
    private String actiontype = "";
    private Timeline timeline;
    private ListView<String> userList; 
    private List<Object[]> gameStatus;
    String word = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Enter Username");
        dialog.setHeaderText("Welcome to Sketchify!");
        dialog.setContentText("Please enter your username:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            System.out.println("[App] No username entered, exiting.");
            Platform.exit();
            return;
        }
        myUsername = result.get().trim();
        System.out.println("[App] My username: " + myUsername);

        String chatURI = "tcp://192.168.0.247:8753/chat?keep";
        String serverURI = "tcp://192.168.0.247:8753/draw?keep";
        String gameURI = "tcp://192.168.0.247:8753/game?keep";
        try {
            chatSpace = new RemoteSpace(chatURI);
            drawSpace = new RemoteSpace(serverURI);
            gameSpace = new RemoteSpace(gameURI);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            gameSpace.put("user", myUsername);
            System.out.println("[App] Put (\"user\", " + myUsername + ") in gameSpace.");
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        top.setPadding(new Insets(0, 182, 0, 0));

        //label1.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");
        //label2.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");
        //label3.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-padding: 5px; -fx-border-color: blue; -fx-border-width: 2px;");

        top.setSpacing(20);
        top.setAlignment(Pos.CENTER);
        top.getChildren().addAll(guessedField, timerLabel);

        //label1.setVisible(false);
        //label2.setVisible(false);
        //label3.setVisible(false);
        guessedField.setVisible(false);
        timerLabel.setVisible(false);
        
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
        start = new Button("Start");
        start.setOnAction(e -> {
            startGame();
            isStarted = true;
        });
        
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

        canvas = new Canvas(800, 530);
        gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        VBox left = new VBox(8);
        left.setStyle("-fx-background-color: lightblue;");
        left.setPadding(new Insets(10));
        left.setPrefWidth(200);

        Label lobbyLabel = new Label("Lobby (Users):");
        userList = new ListView<>();
        userList.setPrefHeight(300);

        left.getChildren().addAll(lobbyLabel, userList);
        StackPane centerPane = new StackPane();
        centerPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        centerPane.widthProperty().addListener((obs, oldW, newW) -> {
            canvas.setWidth(newW.doubleValue());
        });
        centerPane.heightProperty().addListener((obs, oldH, newH) -> {
            canvas.setHeight(newH.doubleValue());
        });

        centerVBox.getChildren().addAll(wordlabel, canvas);
        center.getChildren().add(centerVBox);

        Button clear = new Button("Clear canvas");
        clear.setOnAction(event -> gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));

        clear.setOnAction(event -> {
            try {
                drawSpace.put("draw", 0.0, 0.0, "clear");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        bottom.getChildren().addAll(clear, start);

        root.setLeft(left);
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

        Thread userListener = new Thread(this::listenForUsers, "UserListener");
        userListener.setDaemon(true);
        userListener.start();

        Thread gameStartListener = new Thread(this::listenForGameLogic, "GameLogicListener");
        gameStartListener.setDaemon(true);
        gameStartListener.start();

        Thread timerStartListener = new Thread(this::listenForTimerAction, "ListenForTimerListener");
        timerStartListener.setDaemon(true);
        timerStartListener.start();

        Thread GuessStartListener = new Thread(this::listenForGuesses, "GuessStartListener");
        GuessStartListener.setDaemon(true);
        GuessStartListener.start();
    }

    private boolean drawerAppended = false;
    private boolean isSelected = false;

    private void startGame() {
        isStarted = false;
        if (!isSelected) {
            selectedWord = generateNextWord();
            isSelected = true;
        }

        try {
            if (lastDrawer == null) {
                chooseRandomPlayer();
                gameSpace.put("game", "drawer", chosenDrawer);
                gameSpace.put("game", "start");
                gameSpace.put("game", "timerAction", "start");
                if (!drawerAppended) {
                    chatDisplay.appendText("[System] Drawer selected: " + getDrawer() + "\n");
                    drawerAppended = true;
                }
                isolateDrawerAndGuesser();  
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void isolateDrawerAndGuesser() {
        if (myUsername.equalsIgnoreCase(getDrawer())) {
            Draw draw = new Draw(x, y, actiontype);
            draw.isPressed(canvas, gc, drawSpace);
            draw.isDragged(canvas, gc, drawSpace);
            Platform.runLater(() -> {
                guessedField.setVisible(true);
                wordlabel.setText(selectedWord);
            });
        } else {
            guessedField.setVisible(false);
            wordlabel.setText("");
        }
    }

    private boolean isGuessCorrect = false;
    private void checkGuess(String message) {
        if (message.trim().equalsIgnoreCase(selectedWord) && !isGuessCorrect) {
            isGuessed = true;
            isGuessCorrect = true;

            try {
                chatSpace.put("message", "[System] Correct guess: " + selectedWord + " was guessed by " + myUsername);
                chatSpace.put("message", "[System] Type " + "new round" +  " to start a new round");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        startNewRound();
    }

    private void startNewRound() {
        isSelected = false;
        drawerAppended = false;
        isGuessCorrect = false;
        hasGuessedCorrectly = false;

        try {
            gameSpace.getp(new ActualField("game"), new ActualField("drawer"), new FormalField(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        chooseRandomPlayer();
        try {
            gameSpace.put("game", "drawer", chosenDrawer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        chatInput.setDisable(false);
        sendBtn.setDisable(false);

        Platform.runLater(() -> {
            wordlabel.setText(selectedWord);
        });
    
        Platform.runLater(() -> {
            startGame();
        });
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
 
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            try {
                chatSpace.put("message", "[" + myUsername + "]: " + text);
                if (text.equalsIgnoreCase(selectedWord) || text.equalsIgnoreCase("new round")) {
                    checkGuess(text);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            chatInput.clear();
        }
    }

    private boolean hasGuessedCorrectly = false;

    private void listenForGuesses() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<Object[]> chatMessages = chatSpace.queryAll(
                        new ActualField("message"),
                        new FormalField(String.class)
                    );

                    if (!chatMessages.isEmpty()) {
                        String guess = (String) chatMessages.get(0)[1];
                        if (!hasGuessedCorrectly) {
                            if (guess.equalsIgnoreCase(selectedWord)) {
                                checkGuess(guess);
                                Thread.sleep(10);
                                hasGuessedCorrectly = true; 
                            }
                        }
                    }

                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void listenForTimerAction() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> gameStatus = gameSpace.queryAll(
                    new ActualField("game"),
                    new ActualField("timerAction"),
                    new FormalField(String.class)
                );
    
                if (!gameStatus.isEmpty()) {
                    String timerAction = (String) gameStatus.get(0)[1];
    
                    if ("stop".equals(timerAction)) {
                        timeline.stop();
                        seconds = 61;
                    }
                }
    
                Thread.sleep(300); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String lastDrawer = null;
    private void listenForGameLogic() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> gameStatus = gameSpace.queryAll(new ActualField("game"), new FormalField(String.class));
    
                if (!gameStatus.isEmpty()) {
                    String status = (String) gameStatus.get(0)[1];
    
                    if ("start".equals(status)) {
                        Platform.runLater(() -> {
                            startGame();
                            timerLabel.setVisible(true);
                        });
                    }
    
                    if ("drawer".equals(status)) {
                        String newDrawer = getDrawer();
                        if (lastDrawer == null || !newDrawer.equals(lastDrawer)) {
                            lastDrawer = newDrawer;
                            Platform.runLater(() -> {
                                chatDisplay.appendText("[System] Drawer selected: " + newDrawer + "\n");
                            });
                        }
                    }
                    }
                Thread.sleep(500);
    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                            if ("clear".equals(action)) {
                                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                            }
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

    private void chooseRandomPlayer() {
        try {
            List<Object[]> allUsers = gameSpace.queryAll(
                new ActualField("user"),
                new FormalField(String.class)
            );
            if (allUsers.isEmpty()) {
                chatDisplay.appendText("[System] No users found, cannot start.\n");
                return;
            }
            int idx = new Random().nextInt(allUsers.size());
            chosenDrawer = (String) allUsers.get(idx)[1];
            gameSpace.put("game", "drawer", chosenDrawer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDrawer() {
        try {
            Object[] drawerEntry = gameSpace.query(new ActualField("game"), new ActualField("drawer"), new FormalField(String.class));
            return (String) drawerEntry[2];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return null;
    }

    public String generateNextWord() {
        String filePath = "C:\\Users\\nicla\\OneDrive\\Dokumenter\\GitHub\\02148\\demo\\src\\words.txt";
        String nextWord = null;
    
        // Static tracker for generated words and file reader
        if (generatedWords == null) {
            generatedWords = new HashSet<>();
        }
        BufferedReader reader = null;
    
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (!generatedWords.contains(word)) {
                        generatedWords.add(word); // Track used words
                        nextWord = word; // Found the next unused word
                        break;
                    }
                }
                if (nextWord != null) break; // Stop once a word is found
            }
    
            if (nextWord == null) {
                throw new IllegalStateException("No more unused words available in the file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Error reading from the file.");
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        return nextWord;
    }
    

    private void listenForUsers() {
        while(!Thread.currentThread().isInterrupted()){
            try {
                List<Object[]> users = gameSpace.queryAll(
                    new ActualField("user"),
                    new FormalField(String.class)
                );
                if(users.size() > lastUserCount){
                    Set<String> allNames = new HashSet<>();
                    for(Object[] arr : users){
                        String name = (String) arr[1];
                        allNames.add(name);
                    }
                    Platform.runLater(() -> {
                        userList.getItems().setAll(allNames);
                    });

                    lastUserCount = users.size();
                }
                Thread.sleep(300);
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}