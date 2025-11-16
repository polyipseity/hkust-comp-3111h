package library.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import library.Main;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class InformBoardController {
    Repository repository = Main.getContext().getRepository();

    //Refering to fxml ListView
    @FXML
    private ListView<String> NotificationList;

    //Storing all notifications of user
    private ObservableList<String> notifications = FXCollections.observableArrayList();

    private final User user = Main.getContext().getLoggedInUser()._1();
    @FXML
    private void initialize() {
        NotificationList.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/Notification.css")).toExternalForm());
        NotificationList.setFocusTraversable(false);
        updateNotificationList();
    }

    private void updateNotificationList(){
        Optional<String[]> opt = repository.userNotificationOps.read(user);
        notifications.setAll(opt.get());
        NotificationList.setItems(notifications);
    }

    @FXML
    private void ClearAll() throws TransactionException {
        repository.userNotificationOps.update(user, current -> new String[0]);
        updateNotificationList();
    }

    @FXML
    private void AddNotif() throws TransactionException {
        repository.userNotificationOps.update(user, currentNotifications -> {
            String[] updated = Arrays.copyOf(currentNotifications, currentNotifications.length + 1);
            updated[updated.length - 1] = "New notification message " + updated.length;
            return updated;});
        updateNotificationList();
    }
}
