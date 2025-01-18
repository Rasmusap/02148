package GUI;

import Sketchify.Draw;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class SketchifyController implements Initializable {

    // FXML fields from "sketchify-page.fxml"
    @FXML
    private Canvas Canvas;
    @FXML
    private AnchorPane SceneMain;
    @FXML
    private TextArea PlayerList;
    @FXML
    private Text PlayerListTitle;
    @FXML
    private AnchorPane Tools;
    @FXML
    private TextField guessTextField;
    @FXML
    private Text Title;
    @FXML
    private Text CurrentWord;
    @FXML
    private Text Timer;
    @FXML
    private TextArea Chat;
    @FXML
    private Text CurrentWord2;

    // Reference to the remote spaces
    RemoteSpace drawSpace;
    RemoteSpace chatSpace;
    RemoteSpace gameSpace;
    private GraphicsContext gc;
    private Set<String> generatedWords = new HashSet<>();
    private List<Object[]> userList;
    private int lastDrawCount = 0;
    private int lastChatCount = 0;
    private int lastUserCount = 0;
    private int seconds = 60;  // or 120, etc.

    private int points = 0;
    private Timeline timeline;

    private String word1, word2, word3;
    private String selectedWord = "";
    private boolean guessedCorrectly = false;
    private boolean isGuessed = false;
    private boolean isWordAppended = false;
    private boolean isGuessCorrect = false;
    private boolean drawerAppended = false;
    private int lastUserIndex = 0;

    private String actiontype = "";
    private double x = 0, y = 0;

    private String myUsername = "UnknownUser";

    private String chosenDrawer;
    private String lastDrawer = null;
    private boolean hasGuessedCorrectly = false;
    private boolean isDrawer = false;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // The FXML is loaded, but we do NOT have remote spaces yet.

        // Basic Canvas Setup
        gc = Canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // The random word lines from old "App"
        word1 = generateRandomWord();
        word2 = generateRandomWord();
        word3 = generateRandomWord();
        selectedWord = word1;

        // Display the initial word in the UI if needed
        CurrentWord.setText(selectedWord);
        CurrentWord.setVisible(false);
        updateHiddenWord();

        // Chat & PlayerList read-only
        Chat.setEditable(false);
        PlayerList.setEditable(false);
    }

    private void updateHiddenWord() {
        CurrentWord2.setText("_".repeat(CurrentWord.getText().length()));
        CurrentWord2.setVisible(false);
    }


    /**
     * Called by HostGameController or from another controller after it
     * obtains the RemoteSpaces. We inject them here, then start threads.
     */
    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn,
                          String currentUsername) throws InterruptedException {
        myUsername = currentUsername;
        System.out.println(myUsername);
        this.chatSpace = chatSpaceIn;
        this.gameSpace = gameSpaceIn;
        this.drawSpace = drawSpaceIn;

        if (isDrawer) {
            setUpDrawingEvents();
        }

        // Initialize the timeline or start the timer logic if needed
        initializeTimeline();

        // Start background threads for chat, draws, user-list, etc.
        startThreads();

        // Optional: Show an updated list of users in "PlayerList"
        updatePlayerList();
    }

    private void startThreads() {
        // Chat messages
        Thread chatListenerThread = new Thread(this::listenForChatMessages, "ChatListener");
        chatListenerThread.setDaemon(true);
        chatListenerThread.start();

        // Drawing
        Thread drawListenerThread = new Thread(this::listenForDraws, "DrawListener");
        drawListenerThread.setDaemon(true);
        drawListenerThread.start();

        // If you want user changes
        Thread userListenerThread = new Thread(this::listenForUsers, "UserListener");
        userListenerThread.setDaemon(true);
        userListenerThread.start();

        // If you have game logic threads
        Thread gameLogicListener = new Thread(this::listenForGameLogic, "GameLogicListener");
        gameLogicListener.setDaemon(true);
        gameLogicListener.start();

        Thread timerListener = new Thread(this::listenForTimerAction, "TimerActionListener");
        timerListener.setDaemon(true);
        timerListener.start();

        Thread guessListener = new Thread(this::listenForGuesses, "GuessListener");
        guessListener.setDaemon(true);
        guessListener.start();
    }

    /**
     * Sets up local mouse event handlers to post "draw" to the drawSpace
     */
    private void setUpDrawingEvents() {
        Canvas.setOnMousePressed(event -> {
            double px = event.getX();
            double py = event.getY();
            try {
                drawSpace.put("draw", px, py, "start");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });

        Canvas.setOnMouseDragged(event -> {
            double px = event.getX();
            double py = event.getY();
            try {
                drawSpace.put("draw", px, py, "draw");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
    }

    /**
     * Timer logic (countdown)
     */
    public void initializeTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (seconds > 0) {
                seconds--;
                Timer.setText("Time: " + seconds + " s");
            } else {
                Timer.setText("Time's up!");
                timeline.stop();
                generateNewRound();  // or do whatever happens at timeout
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ============ Chat & Draw Listeners ============

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
                            Chat.appendText(text + "\n");
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
                        double dx = (double) draws.get(i)[1];
                        double dy = (double) draws.get(i)[2];
                        String action = (String) draws.get(i)[3];

                        Platform.runLater(() -> {
                            if ("clear".equals(action)) {
                                gc.clearRect(0, 0, Canvas.getWidth(), Canvas.getHeight());
                            } else if ("start".equals(action)) {
                                gc.beginPath();
                                gc.moveTo(dx, dy);
                                gc.stroke();
                            } else if ("draw".equals(action)) {
                                gc.lineTo(dx, dy);
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

    // ============ Button: Send Chat / Guess ============

    @FXML
    public void sendGuess(ActionEvent event) {
        String guess = guessTextField.getText().trim();
        if (!guess.isEmpty()) {
            try {
                chatSpace.put("message", "you: " + guess);
                // Also display locally
                Chat.appendText("You: " + guess + "\n");
                guessTextField.clear();
                checkGuess(guess);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    /**
     * Checking guess logic
     */
    private void checkGuess(String message) {
        if (message.trim().equalsIgnoreCase(selectedWord) && !guessedCorrectly) {
            guessedCorrectly = true;
            Platform.runLater(() -> {
                Chat.appendText("You guessed the word correctly!\n");
            });
        }
        if (guessedCorrectly) {
            generateNewRound();
        }
    }

    // ============ Other threads for game logic ============

    private void listenForGuesses() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> chatMessages = chatSpace.queryAll(
                        new ActualField("message"),
                        new FormalField(String.class)
                );
                if (!chatMessages.isEmpty()) {
                    String guess = (String) chatMessages.get(0)[1];
                    // If the guess hasn't been marked correct yet, check it
                    if (!hasGuessedCorrectly) {
                        checkGuess(guess);
                        Thread.sleep(10);
                        hasGuessedCorrectly = true;
                    }
                }
                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
                break;
            }
        }
    }

    private void listenForGameLogic() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> statuses = gameSpace.queryAll(
                        new ActualField("game"),
                        new FormalField(String.class)
                );
                if (!statuses.isEmpty()) {
                    // e.g. ("game", "start"), ("game", "drawer"), etc.
                    String status = (String) statuses.get(0)[1];

                    if ("start".equals(status)) {
                        Platform.runLater(this::startGame);
                    }
                    if ("drawer".equals(status)) {
                        String newDrawer = getDrawer();
                        if (lastDrawer == null || !newDrawer.equals(lastDrawer)) {
                            lastDrawer = newDrawer;
                            Platform.runLater(() -> {
                                Chat.appendText("[System] Drawer selected: " + newDrawer + "\n");
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

    // ============ Round Flow Methods ============

    private void startGame() {
        // Example of setting up a new round or choosing a drawer
        try {
            if (lastDrawer == null) {
                chooseRandomPlayer();
                gameSpace.put("game", "drawer", chosenDrawer);
                gameSpace.put("game", "start");
                gameSpace.put("game", "timerAction", "start");
                if (!drawerAppended) {
                    Chat.appendText("Drawer selected " + getDrawer() + "\n");
                    drawerAppended = true;
                }
                isolateDrawerAndGuesser();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void isolateDrawerAndGuesser() {
        String actualDrawer = getDrawer();  // e.g. read from gameSpace
        if (myUsername.equalsIgnoreCase(actualDrawer)) {
            isDrawer = true;
            CurrentWord.setVisible(true);
            enableCanvasDrawing();
        } else {
            isDrawer = false;
            CurrentWord.setVisible(false);
            disableCanvasDrawing();
        }
    }

    private void chooseRandomPlayer() {
        try {
            List<Object[]> allUsers = gameSpace.queryAll(
                    new ActualField("user"),
                    new FormalField(String.class)
            );
            if (allUsers.isEmpty()) {
                Platform.runLater(() -> Chat.appendText("[System] No users found, cannot start.\n"));
                return;
            }
            int idx = new Random().nextInt(allUsers.size());
            chosenDrawer = (String) allUsers.get(idx)[1];
            gameSpace.put("game", "drawer", chosenDrawer);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private String getDrawer() {
        try {
            Object[] drawerEntry = gameSpace.query(
                    new ActualField("game"),
                    new ActualField("drawer"),
                    new FormalField(String.class)
            );
            return (String) drawerEntry[2];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return null;
    }

    private void generateNewRound() {
        // Example method if the timer runs out or guess is correct
        gc.setStroke(Color.BLACK);
        isWordAppended = false;
        hasGuessedCorrectly = false;
        drawerAppended = false;
        isGuessCorrect = false;
        isGuessed = false;

        try {
            chooseRandomPlayer();
            gameSpace.put("game", "drawer", chosenDrawer);
            gameSpace.put("game", "timerAction", "start");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        gc.clearRect(0, 0, Canvas.getWidth(), Canvas.getHeight());
        // Set word to a new generated word.
        CurrentWord.setText(generateRandomWord());
        updatePlayerList();

        seconds = 61;
        timeline.playFromStart();
        timeline.play();
        
        isolateDrawerAndGuesser();
        updateHiddenWord();
    }

    // ============ Utility: random word generation ============

    public String generateRandomWord() {
        String randomWord = "";
        List<String> words = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(
                "src/main/resources/Sketchify/words.txt"
        ))) {
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

    // ============ Show the user list in PlayerList TextArea ============

    private void updatePlayerList() {
        try {
            List<Object[]> users = gameSpace.queryAll(
                    new ActualField("user"),
                    new FormalField(String.class)
            );
            StringBuilder sb = new StringBuilder();
            for (Object[] arr : users) {
                String name = (String) arr[1];
                sb.append(name).append(": ").append(points).append("\n");
            }
            // Update the UI
            Platform.runLater(() -> {
                PlayerList.setText(sb.toString());
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private void listenForUsers() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> users = gameSpace.queryAll(
                        new ActualField("user"),
                        new FormalField(String.class)
                );
                if (users.size() > lastUserCount) {
                    Set<String> allNames = new HashSet<>();
                    for (Object[] arr : users) {
                        String name = (String) arr[1];
                        allNames.add(name);
                    }
                    Platform.runLater(() -> {
                        PlayerList.setText(String.join("\n", allNames));
                    });
                    lastUserCount = users.size();
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

    private void enableCanvasDrawing() {
        Canvas.setOnMousePressed(event -> {
            double px = event.getX();
            double py = event.getY();
            try {
                drawSpace.put("draw", px, py, "start");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });

        Canvas.setOnMouseDragged(event -> {
            double px = event.getX();
            double py = event.getY();
            try {
                drawSpace.put("draw", px, py, "draw");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
    }

    private void disableCanvasDrawing() {
        Canvas.setOnMousePressed(null);
        Canvas.setOnMouseDragged(null);
    }

    @FXML
    public void setClearCanvas(ActionEvent event) {
        if (isDrawer) {
            // Only the drawer can broadcast a clear
            try {
                drawSpace.put("draw", 0.0, 0.0, "clear");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setStrokeBlue(ActionEvent event) {
        gc.setStroke(Color.BLUE);
    }
    public void setStrokeBlack(ActionEvent event) {
        gc.setStroke(Color.BLACK);
    }
    public void setStrokeBrown(ActionEvent event) {
        gc.setStroke(Color.BROWN);
    }
    public void setStrokeGreen(ActionEvent event) {
        gc.setStroke(Color.GREEN);
    }
    public void setStrokeRed(ActionEvent event) {
        gc.setStroke(Color.RED);
    }
    public void setStrokeYellow(ActionEvent event) {
        gc.setStroke(Color.YELLOW);
    }
    public void setStrokePink(ActionEvent event) {
        gc.setStroke(Color.PINK);
    }
    public void setStrokeTurquoise(ActionEvent event) {
        gc.setStroke(Color.TURQUOISE);
    }
}
