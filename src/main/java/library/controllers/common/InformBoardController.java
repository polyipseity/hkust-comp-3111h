package library.controllers.common;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import library.Main;
import library.persistence.Repository;
import library.persistence.TransactionException;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public final class InformBoardController implements RequiresLoggedIn {
    Repository repository = Main.getContext().getRepository();

    //Referring to fxml ListView
    @FXML
    private ListView<String> NotificationList;

    //Storing all notifications of user
    private final ObservableList<String> notifications = FXCollections.observableArrayList();

    class NotificationCell extends ListCell<String> {
        HBox hbox = new HBox();
        Label label = new Label();
        Pane pane = new Pane();
        Button button = new Button("Close");
        String lastItem;

        public NotificationCell() {
            super();
            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setOnAction(_ -> {
                try {
	                CloseNotif(getIndex());
                } catch (TransactionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty) {
                lastItem = null;
                setGraphic(null);
            } else {
                lastItem = item;
                label.setText(item != null ? item : "<null>");
                setGraphic(hbox);
            }
        }
    }

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

        NotificationList.setCellFactory(_ -> new NotificationCell());
		NotificationList.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/common/Notification.css")).toExternalForm());
		NotificationList.setFocusTraversable(false);
		updateNotificationList();
	}

	private void updateNotificationList() {
		Optional<String[]> opt = repository.userNotificationOps.read(getLoggedInUser()._1());
        notifications.setAll(opt.get());
        NotificationList.setItems(notifications);
    }

    @FXML
    private void ClearAll() throws TransactionException {
	    repository.userNotificationOps.update(getLoggedInUser()._1(), current -> new String[0]);
        updateNotificationList();
    }

    @FXML
    private void AddNotif() throws TransactionException {
	    repository.userNotificationOps.update(getLoggedInUser()._1(), currentNotifications -> {
            String[] updated = Arrays.copyOf(currentNotifications, currentNotifications.length + 1);
            updated[updated.length - 1] = "New notification message " + updated.length;
            return updated;});
        updateNotificationList();
    }

    private void CloseNotif(int index) throws TransactionException {
	    repository.userNotificationOps.updateAsList(
                getLoggedInUser()._1(),
			    current -> { current.remove(index); });
        updateNotificationList();
    }
}
