package library.controllers.common;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
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
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public final class InformBoardController implements RequiresLoggedIn, Initializable {
	//Storing all notifications of user
	private final ObservableList<String> notifications = FXCollections.observableArrayList();
	//Referring to fxml ListView
	@UnknownNullability
	public ListView<String> notificationList;
	Repository repository = Main.getContext().getRepository();

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		notificationList.setCellFactory(_ -> new NotificationCell());

		updateNotificationList();
	}

	private void updateNotificationList() {
		Optional<String[]> opt = repository.userNotificationOps.read(getLoggedInUser()._1());
		notifications.setAll(opt.get());
		notificationList.setItems(notifications);
	}

	public void clearAll() {
		try {
			repository.userNotificationOps.update(getLoggedInUser()._1(), current -> new String[0]);
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
		updateNotificationList();
	}

	private void CloseNotif(int index) {
		try {
			repository.userNotificationOps.updateAsList(
					getLoggedInUser()._1(),
					current -> {
						current.remove(index);
					});
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
		updateNotificationList();
	}

		/*
    private void AddNotif() {
	    repository.userNotificationOps.update(getLoggedInUser()._1(), currentNotifications -> {
            String[] updated = Arrays.copyOf(currentNotifications, currentNotifications.length + 1);
            updated[updated.length - 1] = "New notification message " + updated.length;
            return updated;});
        updateNotificationList();
    }
		 */

	class NotificationCell extends ListCell<@Nullable String> {
		HBox hbox = new HBox();
		Label label = new Label();
		Pane pane = new Pane();
		Button button = new Button("Close");
		@Nullable
		String lastItem;

		public NotificationCell() {
			super();
			hbox.getChildren().addAll(label, pane, button);
			HBox.setHgrow(pane, Priority.ALWAYS);
			button.setOnAction(_ -> CloseNotif(getIndex()));
		}

		@Override
		protected void updateItem(@Nullable String item, boolean empty) {
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
}
