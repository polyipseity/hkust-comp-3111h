package library.controllers.common;

import javafx.fxml.Initializable;
import library.Main;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The RequiresLoggedOut interface is used to mark controllers or components
 * that require the user to be logged out in order to function. It provides
 * default initialization behavior to ensure no user is logged in when the
 * component is initialized.
 */
public interface RequiresLoggedOut extends Initializable {
	@Override
	default void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		Main.getContext().setLoggedInUser(null);
	}
}
