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

/**
 * The InformBoardController class is responsible for managing the notification board
 * within the application. It facilitates the display, interaction, and management
 * of user notifications.
 *
 * This controller handles initialization, data loading, and user interactions such as
 * clearing all notifications or closing individual notifications.
 *
 * Implements the {@code RequiresLoggedIn}, {@code Initializable}, and {@code LoadsData} interfaces
 * to ensure proper user session validation and data handling mechanics.
 */
public final class InformBoardController implements RequiresLoggedIn, Initializable, LoadsData {
	//Storing all notifications of user
	private final ObservableList<String> notifications = FXCollections.observableArrayList();
    /**
     * The Notification list.
     */
//Referring to fxml ListView
	@UnknownNullability
	@SuppressWarnings("unused")
	public ListView<String> notificationList;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		notificationList.setCellFactory(_ -> new NotificationCell());

		LoadsData.super.initialize(location, resources);
	}

	@Override
	public void loadData() {
		notifications.setAll(Main.getContext().getRepository().userNotificationOps.readOrThrow(getLoggedInUser()._1()));
		notificationList.setItems(notifications);
	}

    /**
     * Clear all.
     */
    public void clearAll() {
		try {
			Main.getContext().getRepository().userNotificationOps.update(getLoggedInUser()._1(), _ -> new String[0]);
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
		loadData();
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
		loadData();
	}

    /**
     * The type Notification cell.
     */
    class NotificationCell extends ListCell<@Nullable String> {
        /**
         * The Hbox.
         */
        final HBox hbox = new HBox();
        /**
         * The Label.
         */
        final Label label = new Label();
        /**
         * The Pane.
         */
        final Pane pane = new Pane();
        /**
         * The Button.
         */
        final Button button = new Button("Close");

        /**
         * Instantiates a new Notification cell.
         */
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
