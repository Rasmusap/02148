package sketchify;

import java.util.List;
import java.util.Scanner;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

public class ChatServer {
    public static void main(String[] args) {
        // 1) Create a repository for tuple spaces
        SpaceRepository repository = new SpaceRepository();

        // 2) Create a chat space and add it to the repository
        SequentialSpace chatSpace = new SequentialSpace();
        repository.add("chat", chatSpace);

        // 3) Add a gate for client connections (same IP/port as you want)
        String gateURI = "tcp://192.168.0.212:8753/?keep";
        repository.addGate(gateURI);
        System.out.println("Chat server is running on " + gateURI);

        // 4) Thread that polls the space and prints new messages to server console
        new Thread(() -> {
            int lastPrinted = 0; // how many messages we've already logged
            while (true) {
                try {
                    // Get ALL current messages: ("message", <String>)
                    List<Object[]> messages = chatSpace.queryAll(
                            new ActualField("message"),
                            new FormalField(String.class)
                    );

                    // Print only new messages
                    for (int i = lastPrinted; i < messages.size(); i++) {
                        Object[] msg = messages.get(i);
                        String text = (String) msg[1];
                        System.out.println("Server sees: " + text);
                    }
                    lastPrinted = messages.size();

                    // Small sleep to avoid spamming the CPU
                    Thread.sleep(300);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // 5) Thread that reads from the server's console and puts messages into the space
        new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    String msg = scanner.nextLine();          // Read text from server console
                    chatSpace.put("message", "[Server] " + msg);  // Put it in the space
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // 6) Keep the server alive indefinitely
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
