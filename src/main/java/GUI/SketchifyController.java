package GUI;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
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

public class SketchifyController implements Initializable {

    // ---------------------------------------
    // FXML fields from "sketchify-page.fxml"
    // ---------------------------------------
    @FXML
    private Canvas Canvas;

    @FXML
    private AnchorPane CanvasBackground;

    @FXML
    private TextArea PlayerList;  // or use for guessed words, etc.

    @FXML
    private Text PlayerListTitle;

    @FXML
    private AnchorPane SceneMain;

    @FXML
    private Text Timer;

    @FXML
    private Text Title;

    @FXML
    private AnchorPane Tools;

    @FXML
    private Button blackButton;

    @FXML
    private Button blueButton;

    @FXML
    private Button brownButton;

    @FXML
    private Button greenButton;

    @FXML
    private Button pinkButton;

    @FXML
    private Button redButton;

    @FXML
    private Button turquoiseButton;

    @FXML
    private Button yellowButton;

    @FXML
    private TextField guessTextField;

    @FXML
    private TextArea Chat;

    @FXML
    private Text CurrentWord;


    // ---------------------------------------
    // Fields taken from "App"
    // ---------------------------------------
    RemoteSpace drawSpace;
    RemoteSpace chatSpace;
    RemoteSpace gameSpace;

    private Set<String> generatedWords = new HashSet<>();
    private String word1;
    private String word2;
    private String word3;
    private String selectedWord = "";

    private String currentDrawer = "";

    private boolean isGuessed = false;
    private int lastDrawCount = 0;
    private int lastChatCount = 0;
    private int lastUserCount = 0;
    private int seconds = 60;  // or 120, etc.
    private Timeline timeline;

    private GraphicsContext gc;
    private double x = 0;
    private double y = 0;
    private String actiontype = "";
    private List<Object[]> userList;


    // Optional: track if the user guessed
    private boolean guessedCorrectly = false;

    // ---------------------------------------
    // Called automatically after FXML is loaded
    // ---------------------------------------
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Initialize random word.
        word1 = generateRandomWord();
        selectedWord = word1;

        CurrentWord.setText(word1);

        //Chat & player list areas not editable by user
        Chat.setEditable(false);
        PlayerList.setEditable(false);

        //Set up RemoteSpaces
//        String chatURI = "tcp://127.0.0.1:8753/chat?keep";
//        String serverURI = "tcp://127.0.0.1:8753/draw?keep";
//        String gameURI = "tcp://127.0.0.1:8753/game?keep";
//        try {
//            chatSpace = new RemoteSpace(chatURI);
//            drawSpace = new RemoteSpace(serverURI);
//            gameSpace = new RemoteSpace(gameURI);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;


//        }



        //Canvas and GraphicsContext
        gc = Canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // Or handle mouse events here directly:
        setUpDrawingEvents();
        initializeTimeline(); // same logic as before
    }

    private void startThreads() {
        Thread chatListener = new Thread(this::listenForChatMessages, "ChatListener");
        chatListener.setDaemon(true);
        chatListener.start();

        Thread drawListener = new Thread(this::listenForDraws, "DrawListener");
        drawListener.setDaemon(true);
        drawListener.start();

//        Thread userListener = new Thread(this::listenForUsers, "UserListener");
//        userListener.setDaemon(true);
//        userListener.start();
    }

    // Example of capturing mouse events directly
    private void setUpDrawingEvents() {
        // Mouse pressed
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

        // Mouse dragged
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

    // Timer logic
    public void initializeTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (seconds > 0) {
                seconds--;
                Timer.setText("Time: " + seconds + " s");
            } else {
                Timer.setText("Time's up!");
                timeline.stop();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ---------------------------------------
    // Chat & Draw Listeners
    // ---------------------------------------
    private void listenForChatMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Object[]> messages = chatSpace.queryAll(
                        new ActualField("message"),
                        new FormalField(String.class),
                        new FormalField(String.class)
                );
                if (messages.size() > lastChatCount) {
                    for (int i = lastChatCount; i < messages.size(); i++) {
                        String sender = (String) messages.get(i)[1];
                        String text = (String) messages.get(i)[2];
                        if (!"you".equals(sender)) {
                            // Update UI on FX thread
                            Platform.runLater(() -> {
                                Chat.appendText("Friend: " + text + "\n");
                                Chat.setScrollTop(Double.MAX_VALUE);
                            });
                        }
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
                            if ("start".equals(action)) {
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
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ---------------------------------------
    // Example "Send chat message" method
    // (In FXML, you could wire a button to call this)
    // ---------------------------------------

    @FXML
    public void sendGuess(ActionEvent event) {
        String guess = guessTextField.getText().trim();
        if (!guess.isEmpty()) {
            try {
                // Put message into chatSpace with the format you use
                chatSpace.put("message", "you", guess);

                // Append to local chat display
                Chat.appendText("You: " + guess + "\n");

                // Clear the text field
                guessTextField.clear();
                checkGuess(guess);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }


    // Checking guess logic from "App"
    private void checkGuess(String message) {
        if (message.trim().equalsIgnoreCase(selectedWord)) {
            // Maybe show in the PlayerList area or console
            Platform.runLater(() -> {
                Chat.appendText("You guessed the word correctly!\n");
            });
            guessedCorrectly = true;
        }
    }

    // ---------------------------------------
    // Clean up on stop
    // ---------------------------------------
//    public void stopThreads() {
//        if (chatListener != null) {
//            chatListener.interrupt();
//        }
//        if (drawListener != null) {
//            drawListener.interrupt();
//        }
//        if (timeline != null) {
//            timeline.stop();
//        }
//    }

    // ---------------------------------------
    // Generate random words
    // ---------------------------------------
    public String generateRandomWord() {
        String randomWord = "";
        List<String> words = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new FileReader("src/main/resources/Sketchify/words.txt")
        )) {
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
    public void setSpaces(RemoteSpace chatSpaceIn, RemoteSpace gameSpaceIn, RemoteSpace drawSpaceIn) throws InterruptedException {
        this.chatSpace = chatSpaceIn;
        this.gameSpace = gameSpaceIn;
        this.drawSpace = drawSpaceIn;
        startThreads();
        updatePlayerList();
//        System.out.println("Host game chatSpace contain "
//                + chatSpace.queryAll().toString()
//                + " and gameSpace "
//                + gameSpace.queryAll(new ActualField("user"), new FormalField(String.class)).toString()
//                + " and drawSpace "
//                + drawSpace.queryAll().toString());
    }

    private void updatePlayerList() {
        try {
            // Query all tuples of the form ("user", <String>)
            userList = gameSpace.queryAll(
                    new ActualField("user"),
                    new FormalField(String.class)
            );

            // Build a display string
            StringBuilder sb = new StringBuilder();
            for (Object[] userTuple : userList) {
                String username = (String) userTuple[1];
                sb.append(username).append("\n");
            }

            // Show them in the lobbyTextArea
            PlayerList.setText(sb.toString());
            PlayerList.setEditable(false);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}
