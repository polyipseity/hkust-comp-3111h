package library.controllers;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import library.FXMLs;
import library.Main;
import library.controllers.common.DependsOnRole;
import library.controllers.common.RequiresLoggedOut;
import library.controls.ManageProfileControl;
import library.models.User;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.util.Objects;

public final class LoginController implements DependsOnRole, RequiresLoggedOut {
	@UnknownNullability
	public Label headerLabel;
	@UnknownNullability
	public TextField usernameField;
	@UnknownNullability
	public PasswordField passwordField;

	@Nullable
	private User.Role role;

	@Override
	public User.Role getRole() {
		return Objects.requireNonNull(role);
	}

	@Override
	public void setRole(User.Role role) {
		this.role = role;
		headerLabel.setText("%s Login".formatted(role.nameCapitalized));
	}

	public void login() throws IOException {
		String username = usernameField.getText();
		String password = passwordField.getText();

		final var context = Main.getContext();
		switch (context.getManageProfileControl().login(getRole(), username, password)) {
			case ManageProfileControl.LoginResult.Success(final var user, final var data) -> {
				context.setLoggedInUser(new Tuple2<>(user, data));
				Main.getContext().setScene((switch (getRole()) {
					case STUDENT_STAFF -> FXMLs.STUDENT_STAFF_DASHBOARD;
					case AUTHOR -> FXMLs.AUTHOR_DASHBOARD;
					case LIBRARIAN -> FXMLs.LIBRARIAN_DASHBOARD;
				}).load());
			}
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}

	public void goToHome() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

	/**
	 * New: navigate to the standalone Register screen
	 */
	public void goToRegister() throws IOException {
		Main.getContext().setScene(FXMLs.REGISTER.load(loader -> loader.<DependsOnRole>getController().setRole(getRole())));
	}
}
