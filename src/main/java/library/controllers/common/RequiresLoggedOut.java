package library.controllers.common;

import javafx.fxml.Initializable;
import library.Main;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public interface RequiresLoggedOut extends Initializable {
	@Override
	default void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		Main.getContext().setLoggedInUser(null);
	}
}
