package library.controllers;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import library.FXMLs;
import library.Main;
import library.controllers.common.RequiresLoggedOut;
import library.controls.ManageProfileControl;
import library.controls.UserValidator;
import library.models.User;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@RequiredArgsConstructor
public final class RegisterController implements RequiresLoggedOut, Initializable {
	public final User.Role role;
	@UnknownNullability
	@SuppressWarnings("unused")
	public Label headerLabel;
	@UnknownNullability
	@SuppressWarnings("unused")
	public TextField usernameField, passwordField, fullNameField;

	@SuppressWarnings("unused") // for testing
	private RegisterController() {
		this.role = User.Role.values()[0];
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedOut.super.initialize(location, resources);
		headerLabel.setText("%s Register".formatted(role.nameCapitalized));
	}

	public void register() throws IOException {
		String username = usernameField.getText();
		String password = passwordField.getText();
		String fullName = fullNameField.getText();

		final var context = Main.getContext();
		try {
			switch (context.getManageProfileControl().register(UserValidator.DEFAULT, role, username, password, fullName)) {
				case ManageProfileControl.RegisterResult.Success val -> {
					Alerts.showInfoDialog(val.getLocalizedMessage());
					goToLogin();
				}
				case HasMessage val -> Alerts.showErrorDialog(val.getLocalizedMessage());
			}
		} catch (TransactionException exception) {
			Alerts.showErrorDialog(exception.getLocalizedMessage());
		}
	}

	public void goToHome() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

	public void goToLogin() throws IOException {
		Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.setControllerFactory(_ -> new LoginController(role))));
	}
}
