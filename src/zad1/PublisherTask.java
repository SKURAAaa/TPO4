package zad1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import javafx.concurrent.Task;

// Klasa zadania dla wydawcy
public class PublisherTask extends Task<Void> {

    private SocketChannel socketChannel;

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    // Konstruktor
    public PublisherTask(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    protected Void call() throws Exception {
        try {
            // Połączenie z serwerem
            System.out.println(this + " Połączono z serwerem");

            var charset = StandardCharsets.UTF_8;

            // *** Alokacja bufora ***
            // allocateDirect pozwala na wykorzystanie mechanizmów sprzętowych
            // w celu przyspieszenia operacji I/O
            // bufor powinien być przydzielony tylko *raz* i ponownie wykorzystany

            ByteBuffer inBuffer = ByteBuffer.allocateDirect(1024);
            CharBuffer charBuffer;

            // Wysłanie wiadomości "Hi" do serwera
            System.out.println(this + " Wysyłanie wiadomości \"Hi\" do serwera");
            socketChannel.write(charset.encode("Hi"));

            while (true) {
                // Redukcja zużycia zasobów
                Thread.sleep(500);

                // Wyczyszczenie bufora
                inBuffer.clear();

                // Odczyt nowych danych
                int readBytes = socketChannel.read(inBuffer);

                if (readBytes == 0) {
                    // Brak nowych danych
                    continue;
                }
                if (readBytes == -1) {
                    // Kanał został zamknięty przez serwer
                    break;
                }

                // Jeśli są nowe dane
                inBuffer.flip();
                charBuffer = charset.decode(inBuffer);
                String response = charBuffer.toString();

                System.out.println(this + " Serwer: \"" + response + "\"");
                charBuffer.clear();

                switch (response) {
                    case "Hi" -> {
                        // Obsługa odpowiedzi "Hi"
                        break;
                    }
                    case "ADD_TOPIC" -> {
                        // Ignorowanie komunikatu "ADD_TOPIC"
                        System.out.println(this + " Ignorowanie...");
                        break;
                    }
                    case "REMOVE_TOPIC" -> {
                        // Ignorowanie komunikatu "REMOVE_TOPIC"
                        System.out.println(this + " Ignorowanie...");
                        break;
                    }
                    case "OK" -> {
                        // Obsługa odpowiedzi "OK"
                        System.out.println(this + " Serwer: Żądanie pomyślnie obsłużone");
                        break;
                    }
                    case "ERROR" -> {
                        // Obsługa odpowiedzi "ERROR"
                        System.out.println(this + " Serwer: Błąd podczas przetwarzania żądania!");
                        break;
                    }
                    default -> {
                        // Brak akcji dla danej odpowiedzi
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "(Publisher Task)";
    }
}
