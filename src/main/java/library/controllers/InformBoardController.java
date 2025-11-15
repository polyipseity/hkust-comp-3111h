package library.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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

import java.util.Arrays;
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
        NotificationList.setStyle("-fx-selection-bar: transparent; -fx-selection-bar-non-focused: transparent;");
        NotificationList.setFocusTraversable(false);
        NotificationList.setCellFactory(param -> new CustomListCell());
        updateNotificationList();
    }

    // Custom ListCell class
    static class CustomListCell extends ListCell<String> {
        private final HBox cellContent = new HBox();
        private final Text messageText = new Text();
        private final Button clearButton = new Button("Clear");

        public CustomListCell() {
            // Configure the rounded rectangle background
            Rectangle background = new Rectangle(680, 50);
            background.setArcWidth(15);
            background.setArcHeight(15);
            background.setFill(Color.WHITE); // Background color
            background.setStroke(Color.LIGHTGRAY); // Light gray border

            StackPane backgroundWrapper = new StackPane(background);
            backgroundWrapper.setPadding(new Insets(10));

            // Configure the message text
            messageText.setStyle("-fx-font-size: 14px; -fx-fill: #333;");

            // Configure the Clear button
            clearButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;");
            clearButton.setFocusTraversable(false); // Prevent focus on the button
            clearButton.setOnAction(event -> getListView().getItems().remove(getItem()));

            // Set up the horizontal layout
            cellContent.setSpacing(10);
            cellContent.setPadding(new Insets(5, 10, 5, 10));
            cellContent.getChildren().addAll(messageText, clearButton);

            // Align the Clear button to the right
            HBox.setMargin(clearButton, new Insets(0, 0, 0, 20));

            // Add the content to the background
            backgroundWrapper.getChildren().add(cellContent);
            setGraphic(backgroundWrapper);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Update the message text
                messageText.setText(item);
                setGraphic(cellContent);
            }
        }
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
