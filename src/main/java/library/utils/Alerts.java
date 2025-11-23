package library.utils;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for displaying various types of alert dialogs in a JavaFX application.
 * Provides static methods to show informational, error, confirmation, and loading dialogs.
 * These methods help standardize user interactions and ensure consistent dialog behavior
 * throughout the application.
 */
public enum Alerts {
	;

    /**
     * Displays an informational dialog to the user with the given message.
     * This method blocks the calling thread until the dialog is dismissed.
     *
     * @param message the message to be displayed in the informational dialog
     */
    public static void showInfoDialog(String message) {
		new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
	}

    /**
     * Displays an error dialog with the provided message.
     *
     * @param message the message to be displayed in the error dialog
     */
    public static void showErrorDialog(String message) {
		new Alert(Alert.AlertType.ERROR, message).showAndWait();
	}

    /**
     * Displays a confirmation dialog with the specified message and returns
     * the user's decision.
     *
     * @param message the message to be displayed in the confirmation dialog
     * @return {@code true} if the user confirms (presses the default button),         or {@code false} otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean showConfirmDialog(String message) {
		return new Alert(Alert.AlertType.CONFIRMATION, message).showAndWait()
				.map(ButtonType::getButtonData)
				.map(ButtonBar.ButtonData::isDefaultButton)
				.orElse(false);
	}

    /**
     * Displays a loading dialog with the specified message while binding its cursor
     * state to the provided {@code ObservableBooleanValue}. The dialog supports cancellation
     * and automatically closes when the observable value transitions to {@code false}.
     *
     * @param message the message to be displayed in the loading dialog
     * @param running an {@code ObservableBooleanValue} indicating the running state                of the process; when it changes to {@code false}, the dialog closes
     * @return {@code true} if the dialog closed due to the observable value changing to         {@code false}, otherwise {@code false}
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean showLoadingDialog(String message, ObservableBooleanValue running) {
		final var alert = new Alert(Alert.AlertType.NONE, message);
		final var dialog = alert.getDialogPane();

		dialog.getButtonTypes().add(ButtonType.CANCEL);
		dialog.cursorProperty().bind(
				Bindings.when(running)
						.then(Cursor.WAIT)
						.otherwise(Cursor.DEFAULT)
		);

		final var completed = new AtomicBoolean(false);
		running.addListener((_, oldValue, newValue) -> {
			if (oldValue && !newValue) {
				completed.set(true);
				alert.close();
			}
		});
		alert.showAndWait();
		return completed.get();
	}
}
