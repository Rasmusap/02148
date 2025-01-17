package Sketchify;

import java.util.List;
import java.util.Scanner;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

public class ChatServer {
    public static void main(String[] args) {

    }

    public void startServer() {
        SpaceRepository repository = new SpaceRepository();

        SequentialSpace chatSpace = new SequentialSpace();
        SequentialSpace drawSpace = new SequentialSpace();
        SequentialSpace gameSpace = new SequentialSpace();

        repository.add("chat", chatSpace);
        repository.add("draw", drawSpace);
        repository.add("game", gameSpace);

        String gateURI = "tcp://127.0.0.1:8753/?keep";
        try {
            repository.addGate(gateURI);
            System.out.println("[Server] Successfully opened gate on " + gateURI);
        } catch (Exception e) {
            System.err.println("[Server] ERROR: Could not open gate on " + gateURI);
            e.printStackTrace();
            return;
        }
        System.out.println("[Server] Spaces available: \"chat\" and \"draw\"");

        new Thread(() -> {
            int lastPrinted = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<Object[]> messages = chatSpace.queryAll(
                            new ActualField("message"),
                            new FormalField(String.class)
                    );
                    for (int i = lastPrinted; i < messages.size(); i++) {
                        String text = (String) messages.get(i)[1];
                        System.out.println("[Server-Chat] sees: " + text);
                    }
                    lastPrinted = messages.size();

//                    Thread.sleep(3);  // small delay
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "ChatLogger").start();

        new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("[Server] Type something here to send a chat message as [Server].");
                while (!Thread.currentThread().isInterrupted()) {
                    String msg = scanner.nextLine();
                    chatSpace.put("message", "[Server] " + msg);
                    System.out.println("[Server] You typed: " + msg + " (sent to chatSpace)");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "ServerConsoleInput").start();

        new Thread(() -> {
            int lastDrawLogged = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<Object[]> draws = drawSpace.queryAll(
                            new ActualField("draw"),
                            new FormalField(Double.class),
                            new FormalField(Double.class),
                            new FormalField(String.class)
                    );
                    for (int i = lastDrawLogged; i < draws.size(); i++) {
                        Object[] tuple = draws.get(i);
                        double x = (double) tuple[1];
                        double y = (double) tuple[2];
                        String action = (String) tuple[3];
//                        System.out.println("[Server-Draw] sees: (" + x + ", " + y + "), action=" + action);
                    }
                    lastDrawLogged = draws.size();

//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "DrawLogger").start();

//        System.out.println("[Server] Running indefinitely... Press Ctrl+C to stop.");
//        while (true) {
//            try {
////                Thread.sleep();
//            } catch (InterruptedException e) {
//                System.err.println("[Server] Interrupted! Stopping server.");
//                Thread.currentThread().interrupt();
//                break;
//            }
//        }
    }
}
