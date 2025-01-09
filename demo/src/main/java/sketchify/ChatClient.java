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
        // Must match the server's IP, port, and space name "chat"
        String serverURI = "tcp://10.209.248.40:8753/chat?keep";

        try {
            // 1) Connect to the remote chat space
            RemoteSpace chat = new RemoteSpace(serverURI);
            System.out.println("Connected to the chat server.");

            // 2) Start a thread that queries ALL messages and prints new ones
            Thread reader = new Thread(() -> {
                int lastPrinted = 0;
                while (true) {
                    try {
                        // Gather all messages of the form: ("message", String)
                        List<Object[]> messages = chat.queryAll(
                                new ActualField("message"),
                                new FormalField(String.class)
                        );

                        // Print only new messages
                        for (int i = lastPrinted; i < messages.size(); i++) {
                            String text = (String) messages.get(i)[1];
                            System.out.println("Friend: " + text);
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
            });
            reader.setDaemon(true);
            reader.start();

            // 3) Let user send messages
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Type your messages below (Ctrl+D or Ctrl+C to exit).");
            while (true) {
                System.out.print("");
                String line = input.readLine();
                if (line == null) {
                    break;  // End of stream or Ctrl+D
                }
                // Insert the message tuple, leaving it in the space for others
                chat.put("message", "[Client] " + line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}