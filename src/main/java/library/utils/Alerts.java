package library.utils;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.concurrent.atomic.AtomicBoolean;

public enum Alerts {
	;

	public static void showInfoDialog(String message) {
		new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
	}

	public static void showErrorDialog(String message) {
		new Alert(Alert.AlertType.ERROR, message).showAndWait();
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean showConfirmDialog(String message) {
		return new Alert(Alert.AlertType.CONFIRMATION, message).showAndWait()
				.map(ButtonType::getButtonData)
				.map(ButtonBar.ButtonData::isDefaultButton)
				.orElse(false);
	}

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
