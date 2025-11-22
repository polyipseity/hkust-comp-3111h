package library.controllers.common;

import javafx.fxml.Initializable;
import library.Main;
import library.models.User;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public interface RequiresLoggedIn extends Initializable {
	@Override
	default void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		getLoggedInUser();
	}

	default Tuple2<User, User.Data> getLoggedInUser() {
		return Objects.requireNonNull(Main.getContext().getLoggedInUser());
	}

	default Object getLoggedInToken() {
		return Objects.requireNonNull(Main.getContext().getLoggedInToken());
	}
}
