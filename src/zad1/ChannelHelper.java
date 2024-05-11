package zad1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ChannelHelper {

    // Kodowanie znaków
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    // Prywatny konstruktor dla klasy narzędziowej
    private ChannelHelper() {}

    /**
     * Metoda zapisująca wiadomość do kanału.
     *
     * @param socketChannel Kanał, do którego ma zostać zapisana wiadomość
     * @param message       Wiadomość do zapisania
     * @return Liczba zapisanych bajtów
     * @throws IOException Jeśli wystąpi błąd wejścia-wyjścia
     */
    public static int writeToChannel(SocketChannel socketChannel, String message)
            throws IOException {
        // Zakodowanie wiadomości do postaci bajtowej
        ByteBuffer encodedMessage = CHARSET.encode(CharBuffer.wrap(message));
        // Zapisanie bajtów do kanału
        return socketChannel.write(encodedMessage);
    }
}
