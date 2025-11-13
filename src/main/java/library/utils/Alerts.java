package library.utils;

import javafx.scene.control.Alert;
import org.jetbrains.annotations.NotNull;

public enum Alerts {
	;

	public static void showInfoDialog(@NotNull String message) {
		new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
	}

	public static void showErrorDialog(@NotNull String message) {
		new Alert(Alert.AlertType.ERROR, message).showAndWait();
	}
}
