package zad1;

import static zad1.Server.HOST;
import static zad1.Server.PORT;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Klasa odpowiedzialna za interfejs użytkownika klienta
public class Client extends Application {

    @FXML
    protected ListView<String> availableView;

    @FXML
    protected ListView<String> subscribedView;

    @FXML
    private Button subscribeButton;

    @FXML
    private Button unsubscribeButton;

    @FXML
    protected TextArea messageArea;

    @FXML
    protected TextField messageField;

    // Pola nie-FXML

    protected SocketChannel toServer;

    // Metoda obsługująca subskrypcję
    @FXML
    public void subscribe(ActionEvent e) throws IOException {
        String topic = availableView.getSelectionModel().getSelectedItem();
        String message = "SUBSCRIBE;" + topic;
        ChannelHelper.writeToChannel(toServer, message);
        subscribedView.getItems().add(topic);
        availableView.getItems().remove(topic);
        System.out.println(this + " Subskrybowano na \"" + topic + "\"");
    }

    // Metoda obsługująca anulowanie subskrypcji
    @FXML
    public void unsubscribe(ActionEvent e) throws IOException {
        String topic = subscribedView.getSelectionModel().getSelectedItem();
        String message = "UNSUBSCRIBE;" + topic;
        ChannelHelper.writeToChannel(toServer, message);
        subscribedView.getItems().remove(topic);
        availableView.getItems().add(topic);
        System.out.println(this + " Anulowano subskrypcję na \"" + topic + "\"");

    }

    // Metoda inicjalizująca
    public void initialize() throws IOException, InterruptedException {
        // Inicjalizacja list i widoków
        ObservableList<String> availableList = FXCollections.observableArrayList();
        ObservableList<String> subscribedList = FXCollections.observableArrayList();
        availableView.setItems(availableList);
        subscribedView.setItems(subscribedList);

        // Wyłącz przyciski w przypadku braku wyboru
        subscribeButton.disableProperty().bind(
                Bindings.isNull(availableView.getSelectionModel().selectedItemProperty()));
        unsubscribeButton.disableProperty().bind(
                Bindings.isNull(subscribedView.getSelectionModel().selectedItemProperty()));

        // Inicjalizacja kanału gniazda i oczekiwanie na połączenie
        Thread.sleep(1000);
        toServer = SocketChannel.open();
        toServer.configureBlocking(false);
        toServer.connect(new InetSocketAddress(HOST, PORT));
        System.out.println(this + " Łączenie z serwerem...");

        while (!toServer.finishConnect()) {
            // Pasek postępu lub inne operacje do czasu połączenia
        }
        new Thread(new ClientTask(this)).start();
    }

    // Metoda uruchamiania aplikacji
    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        Parent root = FXMLLoader.load(getClass().getResource("Client.fxml"));
        stage.setTitle("Client");
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Metoda zwracająca reprezentację tekstową obiektu
    @Override
    public String toString() {
        return "(Client)"; // Można dodać ID singletona
    }
}
