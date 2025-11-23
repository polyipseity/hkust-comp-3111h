package library.controllers.common;

import javafx.fxml.Initializable;
import library.Main;
import library.models.User;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * RequiresLoggedIn is an interface that serves as a marker to enforce and provide
 * functionalities for controllers that require a logged-in user context.
 *
 * Classes implementing this interface gain access to utility methods to retrieve
 * information about the currently logged-in user and their session token. Additionally,
 * the interface ensures that subclasses are properly initialized with a mechanism to
 * enforce user authentication.
 */
public interface RequiresLoggedIn extends Initializable {
	@Override
	default void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		getLoggedInUser();
	}

    /**
     * Gets logged in user.
     *
     * @return the logged in user
     */
    default Tuple2<User, User.Data> getLoggedInUser() {
		return Objects.requireNonNull(Main.getContext().getLoggedInUser());
	}

    /**
     * Gets logged in token.
     *
     * @return the logged in token
     */
    default Object getLoggedInToken() {
		return Objects.requireNonNull(Main.getContext().getLoggedInToken());
	}
}
