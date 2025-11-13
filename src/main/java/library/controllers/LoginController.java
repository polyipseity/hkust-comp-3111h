package library.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import library.FXMLs;
import library.Main;
import library.controls.ManageProfileControl;
import library.models.User;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
	@FXML
	private Label headerLabel;
	@FXML
	private TextField usernameField;
	@FXML
	private PasswordField passwordField;

	@Nullable
	private User.Role role;

	public @NotNull User.Role getRole() {
		return Objects.requireNonNull(role);
	}

	public void setRole(@NotNull User.Role role) {
		this.role = role;
		headerLabel.setText("%s Login".formatted(role.name));
	}

	@FXML
	private void handleBack() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

	@FXML
	private void handleLogin() throws IOException {
		String username = usernameField.getText();
		String password = passwordField.getText();

		final var context = Main.getContext();
		switch (context.manageProfile.login(getRole(), username, password)) {
			case ManageProfileControl.LoginResult.Success(final var user, final var data) -> {
				context.setLoggedInUser(new Tuple2<>(user, data));
				Main.getContext().setScene((switch (getRole()) {
					case STUDENT_STAFF -> FXMLs.STUDENT_DASHBOARD;
					case AUTHOR -> FXMLs.AUTHOR_DASHBOARD;
					case LIBRARIAN -> FXMLs.LIBRARIAN_DASHBOARD;
				}).load());
			}
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}

	/**
	 * New: navigate to the standalone Register screen
	 */
	@FXML
	private void handleGoToRegister() throws IOException {
		Main.getContext().setScene(FXMLs.REGISTER.load(loader -> loader.<RegisterController>getController().setRole(getRole())));
	}
}
