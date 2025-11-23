package library.controllers;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import library.FXMLs;
import library.Main;
import library.controllers.common.RequiresLoggedOut;
import library.controls.ManageProfileControl;
import library.models.User;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.Tuple2;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * The type Login controller.
 */
@RequiredArgsConstructor
public final class LoginController implements RequiresLoggedOut, Initializable {
    /**
     * The Role.
     */
    public final User.Role role;
    /**
     * The Header label.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Label headerLabel;
    /**
     * The Username field.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextField usernameField;
    /**
     * The Password field.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public PasswordField passwordField;

	@SuppressWarnings("unused") // for testing
	private LoginController() {
		this.role = User.Role.values()[0];
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedOut.super.initialize(location, resources);
		headerLabel.setText("%s Login".formatted(role.nameCapitalized));
	}

    /**
     * Login.
     *
     * @throws IOException the io exception
     */
    public void login() throws IOException {
		String username = usernameField.getText();
		String password = passwordField.getText();

		final var context = Main.getContext();
		switch (context.getManageProfileControl().login(role, username, password)) {
			case ManageProfileControl.LoginResult.Success(final var user, final var data) -> {
				context.setLoggedInUser(new Tuple2<>(user, data));
				Main.getContext().setScene((switch (role) {
					case STUDENT_STAFF -> FXMLs.STUDENT_STAFF_DASHBOARD;
					case AUTHOR -> FXMLs.AUTHOR_DASHBOARD;
					case LIBRARIAN -> FXMLs.LIBRARIAN_DASHBOARD;
				}).load());
			}
			case HasMessage ret -> Alerts.showErrorDialog(ret.getLocalizedMessage());
		}
	}

    /**
     * Go to home.
     *
     * @throws IOException the io exception
     */
    public void goToHome() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

    /**
     * New: navigate to the standalone Register screen
     *
     * @throws IOException the io exception
     */
    public void goToRegister() throws IOException {
		Main.getContext().setScene(FXMLs.REGISTER.load(loader -> loader.setControllerFactory(_ -> new RegisterController(role))));
	}
}
