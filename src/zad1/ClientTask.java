package zad1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import javafx.application.Platform;
import javafx.concurrent.Task;

// Klasa odpowiadająca za wykonywanie zadań klienta
public class ClientTask extends Task<Void> {

    private Client parent;
    private static final int BUFFER_SIZE = 1024;

    public ClientTask(Client parent) {
        this.parent = parent;
    }

    @Override
    protected Void call() throws Exception {
        try {
            logMessage("Połączono z serwerem");

            var charset = StandardCharsets.UTF_8;

            ByteBuffer inBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            CharBuffer charBuffer;

            // Wysyłanie wiadomości "Hi" do serwera
            logMessage("Wysyłanie wiadomości \"Hi\" do serwera");
            parent.toServer.write(charset.encode("Hi"));

            while (true) {
                Thread.sleep(500);

                inBuffer.clear();
                int readBytes = parent.toServer.read(inBuffer);

                if (readBytes == 0) {
                    continue;
                }
                if (readBytes == -1) {
                    break;
                }

                inBuffer.flip();
                charBuffer = charset.decode(inBuffer);
                String fromServer = charBuffer.toString();
                var fromServerArray = fromServer.split(";");

                logMessage("Serwer: \"" + fromServer + "\"");
                charBuffer.clear();

                String command = fromServerArray[0];
                String arguments = null;
                if (fromServerArray.length > 1) {
                    arguments = fromServerArray[1];
                }

                switch (command) {
                    case "Bye" -> {
                        logMessage("Rozłączanie...");
                        break;
                    }
                    case "MESSAGE" -> {
                        handleMessage(arguments);
                        continue;
                    }
                    case "ADD_TOPIC" -> {
                        addTopic(arguments);
                        continue;
                    }
                    case "ADD_TOPICS" -> {
                        if (arguments != null) {
                            addTopics(arguments);
                        }
                        continue;
                    }
                    case "REMOVE_TOPIC" -> {
                        removeTopic(arguments);
                        continue;
                    }
                    case "INFO" -> {
                        logMessage("Informacja od serwera: \"" + arguments + "\"");
                    }
                    default -> {
                        logMessage("Brak akcji dla polecenia \"" + command + "\"");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Dodaje nowy temat do listy dostępnych tematów
    private void addTopic(String topic) {
        Platform.runLater(() -> parent.availableView.getItems().add(topic));
        logMessage("Dodano nowy temat \"" + topic + "\"");
    }

    // Dodaje nowe tematy do listy dostępnych tematów
    private void addTopics(String topics) {
        var topicArray = topics.split("`");
        Platform.runLater(() -> {
            for (String topic : topicArray) {
                parent.availableView.getItems().add(topic);
                logMessage("Dodano nowy temat \"" + topic + "\"");
            }
        });
    }

    // Usuwa temat z listy dostępnych tematów oraz od subskrypcji
    private void removeTopic(String topic) {
        Platform.runLater(() -> {
            parent.availableView.getItems().remove(topic);
            logMessage("Usunięto temat \"" + topic + "\"");
            parent.subscribedView.getItems().remove(topic);
            logMessage("Anulowano subskrypcję na temat \"" + topic + "\"");
        });
    }

    // Obsługuje przychodzące wiadomości
    private void handleMessage(String message) {
        Platform.runLater(() -> {
            if (!parent.messageField.getText().equals("")) {
                if (parent.messageArea.getText().equals("")) {
                    parent.messageArea.setText(parent.messageField.getText());
                } else {
                    parent.messageArea.appendText('\n' + parent.messageField.getText());
                }
            }
            parent.messageField.setText(message);
        });
        logMessage("Nowa wiadomość: \"" + message + "\"");
    }

    // Metoda pomocnicza do logowania komunikatów
    private void logMessage(String message) {
        System.out.println(this + " " + message);
    }

    @Override
    public String toString() {
        return "(Client Task)";
    }
}
