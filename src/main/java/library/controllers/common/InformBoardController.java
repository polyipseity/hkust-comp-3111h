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
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;

public final class InformBoardController implements RequiresLoggedIn, Initializable {
	//Storing all notifications of user
	private final ObservableList<String> notifications = FXCollections.observableArrayList();
	//Referring to fxml ListView
	@UnknownNullability
	@SuppressWarnings("unused")
	public ListView<String> notificationList;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		notificationList.setCellFactory(_ -> new NotificationCell());

		updateNotificationList();
	}

	private void updateNotificationList() {
		notifications.setAll(Main.getContext().getRepository().userNotificationOps.readOrThrow(getLoggedInUser()._1()));
		notificationList.setItems(notifications);
	}

	public void clearAll() {
		try {
			Main.getContext().getRepository().userNotificationOps.update(getLoggedInUser()._1(), _ -> new String[0]);
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
		updateNotificationList();
	}

	private void CloseNotif(int index) {
		try {
			Main.getContext().getRepository().userNotificationOps.updateAsList(
					getLoggedInUser()._1(),
					current -> {
						current.remove(index);
					});
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
		updateNotificationList();
	}

	class NotificationCell extends ListCell<@Nullable String> {
		final HBox hbox = new HBox();
		final Label label = new Label();
		final Pane pane = new Pane();
		final Button button = new Button("Close");

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
				setGraphic(null);
			} else {
				label.setText(item != null ? item : "<null>");
				setGraphic(hbox);
			}
		}
	}
}
