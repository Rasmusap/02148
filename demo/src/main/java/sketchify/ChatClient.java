package sketchify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

public class ChatClient {
    public static void main(String[] args) {
        String chatURI = "tcp://192.168.0.247:8753/chat?keep";
        System.out.println("[ChatClient] Attempting to connect to " + chatURI + " ...");
        final RemoteSpace chat;
        try {
            chat = new RemoteSpace(chatURI);
            System.out.println("[ChatClient] Connected successfully!");
        } catch (Exception e) {
            System.err.println("[ChatClient] ERROR: Could not connect to " + chatURI);
            e.printStackTrace();
            return;
        }

        Thread reader = new Thread(() -> {
            System.out.println("[ChatClient] Reader thread started. Waiting for new messages...");
            int lastPrinted = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<Object[]> messages = chat.queryAll(
                        new ActualField("message"),
                        new FormalField(String.class)
                    );

                    for (int i = lastPrinted; i < messages.size(); i++) {
                        String text = (String) messages.get(i)[1];
                        System.out.println("[ChatClient] Friend: " + text);
                    }
                    lastPrinted = messages.size();

                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "ChatReader");
        reader.setDaemon(true);
        reader.start();

        System.out.println("[ChatClient] Type messages to send. Press Ctrl+D or Ctrl+C to exit.");
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String line = console.readLine();
                if (line == null) {
                    System.out.println("[ChatClient] EOF detected, exiting.");
                    break;
                }
                try {
                    chat.put("message", line);
                    System.out.println("[ChatClient] You: " + line);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("[ChatClient] Shutting down...");
        reader.interrupt();
    }
}