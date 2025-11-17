package library.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public enum Alerts {
	;

	public static void showInfoDialog(@NotNull String message) {
		new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
	}

	public static void showErrorDialog(@NotNull String message) {
		new Alert(Alert.AlertType.ERROR, message).showAndWait();
	}

    public static Optional<ButtonType> showConfirmDialog(String message) {
        return new Alert(Alert.AlertType.CONFIRMATION, message).showAndWait();
    }
}
