package GUI;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

/**
 * Controls the main Sketchify game scene (sketchify-page.fxml), including
 * drawing logic, round progression, user updates, and guess checking.
 */
public class SketchifyController implements Initializable {

    // ---- FXML UI References ----
    @FXML
    private AnchorPane SceneMain, Tools;
    @FXML
    private Canvas Canvas;
    @FXML
    private TextArea PlayerList, Chat;
    @FXML
    private TextField guessTextField;
    @FXML
    private Text CurrentWord;       // Drawer sees this
    @FXML
    private Text CurrentWordHidden; // Guessers see underscores here
    @FXML
    private Text Timer;

    // ---- Remote Spaces ----
    private RemoteSpace drawSpace;
    private RemoteSpace chatSpace;
    private RemoteSpace gameSpace;

    // ---- Game State & Timers ----
    private GraphicsContext gc;
    private Timeline timeline;
    private int seconds = 60;
    private int lastDrawCount = 0;
    private int lastChatCount = 0;
    private int lastUserCount = 0;

    // ---- Logic for Players/Words/Drawers ----
    private boolean isDrawer = false;
    private boolean guessedCorrectly = false;
    private boolean roundOngoing = false;

    private String myUsername = "UnknownUser";
    private String myRole;
    private String chosenDrawer = null;
    private String selectedWord = "";

    // We store words we have used already, so we don't repeat:
    private Set<String> generatedWords = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup the canvas drawing context
        gc = Canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // By default, hide the real word from the user
        CurrentWord.setVisible(false);
        CurrentWordHidden.setVisible(false);

        // Make Chat and PlayerList read-only
        Chat.setEditable(false);
        PlayerList.setEditable(false);
    }

    /**
     * Called by another controller (e.g. HostGameController) to inject the remote spaces
     * and the local username. After that, we start threads, set up the timeline,
     * and immediately start a round for convenience.
     */
    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn, String username, String role) {
        this.myRole = role;
        this.chatSpace = chatSpaceIn;
        this.gameSpace = gameSpaceIn;
        this.drawSpace = drawSpaceIn;
        this.myUsername = username;

        // Launch our background threads
        startThreads();

        // Create the timeline that decrements 'seconds' every 1s
        initializeTimeline();

        // ***** Start a round right away *****
        // This ensures the round begins on initialization
        generateNewRound();
    }

    // -------------------------------------------------------------------------
    //                  BACKGROUND THREADS SETUP
    // -------------------------------------------------------------------------
    private void startThreads() {
        Thread chatListener = new Thread(this::listenForChatMessages, "ChatListener");
        chatListener.setDaemon(true);
        chatListener.start();

        Thread drawListener = new Thread(this::listenForDraws, "DrawListener");
        drawListener.setDaemon(true);
        drawListener.start();

        Thread userListener = new Thread(this::listenForUsers, "UserListener");
        userListener.setDaemon(true);
        userListener.start();

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

    // -------------------------------------------------------------------------
    //                  TIMELINE LOGIC
    // -------------------------------------------------------------------------
    private void initializeTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (seconds > 0) {
                seconds--;
                Timer.setText("Time: " + seconds + " s");
            } else {
                Timer.setText("Time's up!");
                timeline.stop();
                // If time is up, we might start a new round or let the host do so
                generateNewRound();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // -------------------------------------------------------------------------
    //                  NEW ROUND & DRAWER SELECTION
    // -------------------------------------------------------------------------
    /**
     * Called on initialization or after a correct guess/time out to begin a new round.
     * We pick a new drawer, pick a new word, and store them in gameSpace for everyone.
     */
    private void generateNewRound() {
        guessedCorrectly = false;
        roundOngoing = true;
        timeline.stop();
        seconds = 61;

        // Clear local canvas
        Platform.runLater(() -> gc.clearRect(0, 0, Canvas.getWidth(), Canvas.getHeight()));

        try {
            // 1) remove any old selectedWord tuple
            Object[] oldWord = gameSpace.getp(
                    new ActualField("game"),
                    new ActualField("selectedWord"),
                    new FormalField(String.class)
            );
            Object[] oldDrawer = gameSpace.getp(
                    new ActualField("game"),
                    new ActualField("drawer"),
                    new FormalField(String.class)
            );
            // (If oldWord != null, we just ignore it since we want a fresh word)

            // 2) pick random user as new drawer
            String newDrawer = pickRandomPlayer();

            // 3) generate a random word
            String newWord = generateRandomWord();

            // 4) put them back
            gameSpace.put("game", "drawer", newDrawer);
            gameSpace.put("game", "selectedWord", newWord);
            gameSpace.put("game", "start");
            gameSpace.put("game", "timerAction", "start");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        timeline.playFromStart();
        isolateDrawerAndGuesser();
    }


    /**
     * Picks a random user from the "user" tuples in gameSpace.
     * If there's only one user, we pick that one by default.
     */
    private String pickRandomPlayer() throws InterruptedException {
        List<Object[]> allUsers = gameSpace.queryAll(
                new ActualField("user"),
                new FormalField(String.class)
        );
        if (allUsers.isEmpty()) {
            // no user found
            return "Unknown";
        }
        // if exactly 1 user
        if (allUsers.size() == 1) {
            return (String) allUsers.get(0)[1];
        }
        // otherwise pick random
        int idx = new Random().nextInt(allUsers.size());
        return (String) allUsers.get(idx)[1];
    }

    /**
     * Generates a random word that we haven't used before,
     * storing it in 'generatedWords' to avoid repeats.
     */
    private String generateRandomWord() {
        String randomWord = "";
        List<String> words = new ArrayList<>();

        // Adjust path if needed, or read from resources
        try (BufferedReader reader = new BufferedReader(
                new FileReader("src/main/resources/Sketchify/words.txt")
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineWords = line.split("\\s+");
                words.addAll(Arrays.asList(lineWords));
            }

            if (words.isEmpty()) {
                throw new IllegalStateException("No words found in words.txt!");
            }

            Random rand = new Random();
            int attempts = 0;
            boolean wordFound = false;
            while (attempts++ < 100 && !wordFound) {
                String candidate = words.get(rand.nextInt(words.size()));
                if (!generatedWords.contains(candidate)) {
                    generatedWords.add(candidate);
                    randomWord = candidate;
                    wordFound = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return randomWord.isEmpty() ? "EMPTY" : randomWord;
    }

    // -------------------------------------------------------------------------
    //                  DRAWER OR GUESSER?
    // -------------------------------------------------------------------------
    /**
     * Called whenever "drawer" or "start" changes in gameSpace,
     * so each client sees who is the drawer and which word is active.
     */
    private void isolateDrawerAndGuesser() {
        String actualDrawer = getDrawer();
        String actualWord = getSelectedWord();

        // If I'm the drawer
        if (myUsername.equalsIgnoreCase(actualDrawer)) {
            isDrawer = true;
            CurrentWord.setText(actualWord);
            CurrentWord.setVisible(true);
            CurrentWordHidden.setVisible(false);
            enableCanvasDrawing();
            guessTextField.setEditable(false);
        } else {
            // I'm a guesser
            isDrawer = false;
            CurrentWord.setVisible(false);
            // Show underscores
            CurrentWordHidden.setText("_ ".repeat(actualWord.length()));
            CurrentWordHidden.setVisible(true);
            disableCanvasDrawing();
        }
    }

    private String getDrawer() {
        try {
            Object[] result = gameSpace.query(
                    new ActualField("game"),
                    new ActualField("drawer"),
                    new FormalField(String.class)
            );
            return (String) result[2];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return "Unknown";
        }
    }

    private String getSelectedWord() {
        try {
            Object[] result = gameSpace.query(
                    new ActualField("game"),
                    new ActualField("selectedWord"),
                    new FormalField(String.class)
            );
            return (String) result[2];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return "";
        }
    }

    // -------------------------------------------------------------------------
    //                  STROKE COLOR CHANGE + CLEAR
    // -------------------------------------------------------------------------
    @FXML
    public void setStrokeBlue(ActionEvent event) {
        gc.setStroke(Color.BLUE);
    }
    @FXML
    public void setStrokeBlack(ActionEvent event) {
        gc.setStroke(Color.BLACK);
    }
    @FXML
    public void setStrokeBrown(ActionEvent event) {
        gc.setStroke(Color.BROWN);
    }
    @FXML
    public void setStrokeGreen(ActionEvent event) {
        gc.setStroke(Color.GREEN);
    }
    @FXML
    public void setStrokeRed(ActionEvent event) {
        gc.setStroke(Color.RED);
    }
    @FXML
    public void setStrokeYellow(ActionEvent event) {
        gc.setStroke(Color.YELLOW);
    }
    @FXML
    public void setStrokePink(ActionEvent event) {
        gc.setStroke(Color.PINK);
    }
    @FXML
    public void setStrokeTurquoise(ActionEvent event) {
        gc.setStroke(Color.TURQUOISE);
    }

    @FXML
    public void setClearCanvas(ActionEvent event) {
        if (isDrawer) {
            try {
                drawSpace.put("draw", 0.0, 0.0, "clear");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    // -------------------------------------------------------------------------
    //                  CANVAS EVENT HANDLERS
    // -------------------------------------------------------------------------
    private void enableCanvasDrawing() {
        Canvas.setOnMousePressed(event -> {
            double x = event.getX();
            double y = event.getY();
            try {
                drawSpace.put("draw", x, y, "start");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
        });

        Canvas.setOnMouseDragged(event -> {
            double x = event.getX();
            double y = event.getY();
            try {
                drawSpace.put("draw", x, y, "draw");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
        });
    }

    private void disableCanvasDrawing() {
        Canvas.setOnMousePressed(null);
        Canvas.setOnMouseDragged(null);
    }

    // -------------------------------------------------------------------------
    //                  GUESS LOGIC
    // -------------------------------------------------------------------------
    @FXML
    public void sendGuess(ActionEvent event) {
        String guess = guessTextField.getText().trim();
        if (!guess.isEmpty()) {
            try {
                chatSpace.put("message", myUsername + ": " + guess);
                Chat.appendText("You: " + guess + "\n");
                guessTextField.clear();
                checkGuess(guess);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    private void checkGuess(String guess) {
        String word = getSelectedWord();
        if (!guessedCorrectly && guess.equalsIgnoreCase(word)) {
            guessedCorrectly = true;
            Platform.runLater(() -> Chat.appendText("Correct guess!\n"));
            // For simplicity, automatically start a new round now
            generateNewRound();
        }
    }

    // -------------------------------------------------------------------------
    //                  LISTENER METHODS
    // -------------------------------------------------------------------------
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
                        Platform.runLater(() -> Chat.appendText(text + "\n"));
                    }
                    lastChatCount = messages.size();
                }
//                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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
                Thread.sleep(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
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
                    lastUserCount = users.size();
                    Set<String> names = new HashSet<>();
                    for (Object[] arr : users) {
                        String name = (String) arr[1];
                        names.add(name);
                    }
                    Platform.runLater(() -> {
                        PlayerList.setText(String.join("\n", names));
                    });
                }
                Thread.sleep(3);
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
                    Object[] lastOne = statuses.get(statuses.size()-1);
                    String status = (String) lastOne[1];
                    // If "start" or "drawer" changes
                    if ("start".equals(status) || "drawer".equals(status)) {
                        Platform.runLater(this::isolateDrawerAndGuesser);
                    }
                }
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void listenForTimerAction() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> timerActions = gameSpace.queryAll(
                        new ActualField("game"),
                        new ActualField("timerAction"),
                        new FormalField(String.class)
                );
                if (!timerActions.isEmpty()) {
                    for (Object[] arr : timerActions) {
                        String action = (String) arr[2];
                        if ("stop".equals(action)) {
                            timeline.stop();
                            seconds = 61;
                        }
                    }
                }
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void listenForGuesses() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> chatMessages = chatSpace.queryAll(
                        new ActualField("message"),
                        new FormalField(String.class)
                );
                if (chatMessages.size() > lastChatCount) {
                    for (int i = lastChatCount; i < chatMessages.size(); i++) {
                        String msg = (String) chatMessages.get(i)[1];
                        // parse guess from "username: guess"
                        String guess = msg.substring(msg.indexOf(":") + 1).trim();
                        if (!guessedCorrectly) {
                            checkGuess(guess);
                        }
                    }
                    lastChatCount = chatMessages.size();
                }
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
