package zad1;

import static zad1.ChannelHelper.writeToChannel;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

    // Adres i port serwera
    public static final String HOST = "localhost";
    public static final int PORT = 9000;

    // Mapowanie kanałów dla poszczególnych tematów oraz mapowanie wiadomości dla poszczególnych kanałów
    private static HashMap<String, List<SocketChannel>> topicChannelsMap = new HashMap<>();
    private static HashMap<SocketChannel, Queue<String>> channelMessagesMap = new HashMap<>();

    // Metoda main
    public static void main(String[] args) throws IOException, InterruptedException {
        new Server();
    }

    // Konstruktor serwera
    Server() throws IOException, InterruptedException {
        try (var serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new InetSocketAddress(HOST, PORT));
            serverSocketChannel.configureBlocking(false);
            var selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println(this + " Oczekiwanie na połączenia...");

            // Główna pętla serwera
            while (true) {
                Thread.sleep(500);
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(serverSocketChannel, selector);
                        continue;
                    }

                    if (key.isReadable()) {
                        readData(key);
                        continue;
                    }

                    if (key.isWritable()) {
                        writeData(key);
                        continue;
                    }
                }
            }
        }
    }

    // Metoda do zapisu danych
    private void writeData(SelectionKey key) throws IOException, InterruptedException {
        var socketChannel = (SocketChannel) key.channel();
        var queue = channelMessagesMap.get(socketChannel);
        String message = queue.poll();
        if (message != null) {
            writeToChannel(socketChannel, message);
        }
    }

    // Metoda do odczytu danych
    private void readData(SelectionKey key) {
        var socketChannel = (SocketChannel) key.channel();
        handleRequest(socketChannel);
    }

    // Metoda do akceptowania nowego połączenia
    private void acceptConnection(ServerSocketChannel serverSocketChannel, Selector selector)
            throws IOException, InterruptedException {
        System.out.println(this + " Nowe połączenie odebrane");
        var socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        var clientKey = socketChannel.register(selector, OP_READ | OP_WRITE);

        var clientQueue = new LinkedBlockingQueue<String>();
        channelMessagesMap.put((SocketChannel) clientKey.channel(), clientQueue);

        var topics = topicChannelsMap.keySet();
        if (!topics.isEmpty()) {
            String message = "ADD_TOPICS;";
            for (String topic : topics) {
                message += topic + '`';
            }
            clientQueue.put(message);
        }
    }

    // Stałe do obsługi kodowania i rozmiaru bufora
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 1024;

    private ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
    private StringBuilder sb = new StringBuilder();

    // Metoda obsługująca zapytania od klientów
    private void handleRequest(SocketChannel socketChannel) {
        if (!socketChannel.isOpen())
            return;

        bb.clear();
        sb.setLength(0);

        try {
            int b = socketChannel.read(bb);
            if (b <= 0)
                return;

            bb.flip();
            var charBuffer = CHARSET.decode(bb);

            while (charBuffer.hasRemaining()) {
                sb.append(charBuffer.get());
            }

            var requestArray = sb.toString().split(";");
            String command = requestArray[0];
            String arguments = "";
            if (requestArray.length > 1) {
                arguments = requestArray[1];
                arguments = arguments.replace("\n", "").replace("\r", "");
            }

            System.out.println(this + " Zapytanie otrzymane: " + command
                    + (arguments.equals("") ? "" : " \"" + arguments + "\""));

            switch (command) {
                case "Hello" -> {
                    writeToChannel(socketChannel, "Hello");
                }
                case "Goodbye" -> {
                    sayGoodbyeAndDisconnect(socketChannel);
                }
                case "PUBLISH" -> {
                    publishAndNotifySubscribers(socketChannel, arguments);
                }
                case "UNPUBLISH" -> {
                    unpublishAndNotifySubscribers(socketChannel, arguments);
                }
                case "MESSAGE" -> {
                    registerAndNotifySubscribers(socketChannel, arguments);
                }
                case "SUBSCRIBE" -> {
                    handleSubscribeRequest(socketChannel, arguments);
                }
                case "UNSUBSCRIBE" -> {
                    handleUnsubscribeRequest(socketChannel, arguments);
                }
                default -> {
                    System.out.println(command);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metoda do żegnania klienta i rozłączania
    private void sayGoodbyeAndDisconnect(SocketChannel socketChannel) throws IOException {
        writeToChannel(socketChannel, "Goodbye");
        System.out.println(this + " Żegnanie klienta...");

        socketChannel.close();
        socketChannel.socket().close();
    }

    // Metoda obsługująca subskrypcję
    private void handleSubscribeRequest(SocketChannel socketChannel, String arguments)
            throws IOException {
        String topic = arguments;
        var channels = topicChannelsMap.get(topic);
        if (channels == null) {
            topicChannelsMap.put(topic, Arrays.asList(socketChannel));
        } else {
            channels.add(socketChannel);
        }
        writeToChannel(socketChannel,
                "INFO;Pomyślnie zasubskrybowano na \"" + topic + "\"");
    }

    // Metoda obsługująca anulowanie subskrypcji
    private void handleUnsubscribeRequest(SocketChannel socketChannel, String arguments)
            throws IOException {
        String topic = arguments;
        var channels = topicChannelsMap.get(topic);
        if (channels == null) {
            writeToChannel(socketChannel, "Brak takiego tematu \"" + topic + "\"");
        } else {
            channels.remove(socketChannel);
        }
        writeToChannel(socketChannel,
                "INFO;Pomyślnie anulowano subskrypcję na \"" + topic + "\"");
    }

    // Metoda do publikowania i powiadamiania subskrybentów
    private void publishAndNotifySubscribers(SocketChannel publisher, String arguments) {
        String topic = arguments;
        topicChannelsMap.put(topic, new ArrayList<>());
        System.out.println(this + " Dodano nowy temat \"" + topic + "\"");

        var allChannels = channelMessagesMap.keySet();
        for (SocketChannel sc : allChannels) {
            channelMessagesMap.get(sc).add("ADD_TOPIC;" + topic);
        }
        channelMessagesMap.get(publisher).add("OK");
    }

    // Metoda do usuwania tematu i powiadamiania subskrybentów
    private void unpublishAndNotifySubscribers(SocketChannel publisher, String arguments) {
        String topic = arguments;
        topicChannelsMap.remove(topic);
        System.out.println(this + " Usunięto temat \"" + topic + "\"");

        var allChannels = channelMessagesMap.keySet();
        for (SocketChannel sc : allChannels) {
            channelMessagesMap.get(sc).add("REMOVE_TOPIC;" + topic);
        }
        channelMessagesMap.get(publisher).add("OK");
    }

    // Metoda do rejestrowania i powiadamiania subskrybentów o nowej wiadomości
    private void registerAndNotifySubscribers(SocketChannel publisher, String arguments) {
        var argArray = arguments.split("`");
        String topic = argArray[0];
        String message = "MESSAGE;" + argArray[1];
        var subscribers = topicChannelsMap.get(topic);
        for (SocketChannel sc : subscribers) {
            channelMessagesMap.get(sc).add(message);
        }
        channelMessagesMap.get(publisher).add("OK");
        System.out.println(this + " Dodano wiadomość \"" + message + "\" do tematu \"" + topic + "\"");
    }

    @Override
    public String toString() {
        return "(Mój Serwer)";
    }
}
