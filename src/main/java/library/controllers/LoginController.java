package library.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import library.FXMLs;
import library.Main;
import library.models.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

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
	private void handleLogin() {
		Alert errorAlert = new Alert(Alert.AlertType.ERROR, "");
		errorAlert.initModality(Modality.APPLICATION_MODAL);
		errorAlert.getDialogPane().setContentText("Username/Password Invalid");

		String username = usernameField.getText();
		String password = passwordField.getText();

		User user = new User(username);
		Optional<User.Data> userData = Main.getContext().getRepository().readUser(user);

		// The user with the provided username is not found
		if (userData.isEmpty())
			errorAlert.showAndWait();
		else if (!userData.get().password().equals(password))
			errorAlert.showAndWait();
		else {
			FXMLs fxml = switch (getRole()) {
				case STUDENT_STAFF -> FXMLs.STUDENT_DASHBOARD;
				case AUTHOR -> FXMLs.AUTHOR_DASHBOARD;
				case LIBRARIAN -> FXMLs.LIBRARIAN_DASHBOARD;
			};
			try {
				// TODO: pass user details to Student/Author/Librarian dashboard
				Main.getContext().setScene(fxml.load());
			} catch (IOException e) {
				e.printStackTrace();
			}
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
