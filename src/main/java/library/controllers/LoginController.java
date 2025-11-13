package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import library.FXMLs;
import library.Main;
import library.models.User;

import java.io.IOException;
import java.util.Optional;

public class LoginController {
	@FXML
	private Label headerLabel;
	@FXML
	private TextField usernameField;
	@FXML
	private PasswordField passwordField;

	private String selectedRole;

	public void setRole(String role) {
		this.selectedRole = role;
		String role_name = switch (role.toLowerCase()) {
			case "student", "staff" -> "Student/Staff";
			case "author" -> "Author";
			case "librarian" -> "Librarian";
			default -> "";
		};
		headerLabel.setText(role_name + " Login");
	}

	@FXML
	private void handleBack(ActionEvent event) throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

	@FXML
	private void handleLogin(ActionEvent event) {
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
			FXMLs fxml = switch (selectedRole.toLowerCase()) {
				case "student" -> FXMLs.STUDENT_DASHBOARD;
				case "author" -> FXMLs.AUTHOR_DASHBOARD;
				case "librarian" -> FXMLs.LIBRARIAN_DASHBOARD;
				default -> FXMLs.HOME;
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
	private void handleGoToRegister(ActionEvent event) throws IOException {
		FXMLLoader loader = FXMLs.REGISTER.loader();
		Parent root = loader.load();
		RegisterController ctrl = loader.getController();
		ctrl.setRole(selectedRole);
		Main.getContext().setScene(root);
	}


	private String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
