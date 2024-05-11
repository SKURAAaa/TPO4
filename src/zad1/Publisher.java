package zad1;

import static zad1.Server.HOST;
import static zad1.Server.PORT;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Klasa odpowiadająca za interfejs użytkownika publikującego

public class Publisher extends Application {
    @FXML
    private Button publishButton;

    @FXML
    private Button unpublishButton;

    @FXML
    private Button sendButton;

    @FXML
    private TextField topicField;

    @FXML
    private ListView<String> topicView;

    @FXML
    private TextField messageField;

    // pola niefxmlowe
    private SocketChannel toServer;

    public static void main(String[] args) {
        launch(args);
    }

    // Metoda obsługująca dodanie tematu
    @FXML
    public void addTopic(String topic) {
        topicView.getItems().add(topic);
    }

    // Metoda obsługująca usunięcie tematu
    @FXML
    public void removeTopic(String topic) {
        topicView.getItems().remove(topic);
    }

    // Metoda obsługująca publikację tematu
    @FXML
    public void publish(ActionEvent e) throws IOException {
        String topic = topicField.getText();
        if (topicView.getItems().contains(topic))
            return;
        String message = "PUBLISH;" + topic;
        ChannelHelper.writeToChannel(toServer, message);
        addTopic(topic);
        System.out.println(this + " Opublikowano temat \"" + topic + "\"");
    }

    // Metoda obsługująca anulowanie publikacji tematu
    @FXML
    public void unpublish(ActionEvent e) throws IOException {
        String topic = topicView.getSelectionModel().getSelectedItem();
        if (topic == null) {
            System.out.println(this + " Wybierz temat do anulowania publikacji!");
            return;
        }
        String message = "UNPUBLISH;" + topic;
        ChannelHelper.writeToChannel(toServer, message);
        removeTopic(topic);
        System.out.println(this + " Anulowano publikację tematu \"" + topic + "\"");
    }

    // Metoda obsługująca wysłanie wiadomości
    @FXML
    public void send(ActionEvent e) throws IOException {
        String message = messageField.getText();
        String topic = topicView.getSelectionModel().getSelectedItem();
        if (topic == null) {
            System.out.println(this + " Wybierz temat przed wysłaniem wiadomości!");
            return;
        }
        message = "MESSAGE;" + topic + "`" + message;
        ChannelHelper.writeToChannel(toServer, message);
        System.out.println(this + " Wysłano wiadomość \"" + message + "\" na temat \"" + topic + "\"");
    }

    // Metoda inicjalizująca
    public void initialize() throws IOException, InterruptedException {
        // Inicjalizacja widoku tematów
        topicView.setItems(FXCollections.observableArrayList());

        // Wyłącz przycisk wysyłania w przypadku braku wyboru tematu
        sendButton.disableProperty().bind(
                Bindings.isNull(topicView.getSelectionModel().selectedItemProperty()));

        // Inicjalizacja kanału gniazda i oczekiwanie na połączenie
        Thread.sleep(1000);
        toServer = SocketChannel.open();
        toServer.configureBlocking(false);
        toServer.connect(new InetSocketAddress(HOST, PORT));
        System.out.println(this + " Łączenie z serwerem...");

        while (!toServer.finishConnect()) {
            // pasek postępu lub inne operacje do czasu połączenia
        }
        new Thread(new PublisherTask(toServer)).start();
    }

    // Metoda uruchamiania aplikacji
    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        Parent root = FXMLLoader.load(getClass().getResource("Publisher.fxml"));
        stage.setTitle("Publisher");
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Metoda zwracająca reprezentację tekstową obiektu
    @Override
    public String toString() {
        return "(Publisher)";
    }
}
