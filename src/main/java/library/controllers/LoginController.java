package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import library.Main;
import library.models.User;

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
		Parent home = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Home.fxml")));
		Main.getPrimaryStage().setScene(new Scene(home, 640, 480));
	}

	@FXML
	private void handleLogin(ActionEvent event) {
		Alert errorAlert = new Alert(Alert.AlertType.ERROR, "");
		errorAlert.initModality(Modality.APPLICATION_MODAL);
		errorAlert.getDialogPane().setContentText("Username/Password Invalid");

		String username = usernameField.getText();
		String password = passwordField.getText();

		User user = new User(username);
		Optional<User.Data> userData = Main.getRepository().readUser(user);

		// The user with the provided username is not found
		if (userData.isEmpty())
			errorAlert.showAndWait();
		else if (!userData.get().password().equals(password))
			errorAlert.showAndWait();
		else {
			String fxml = switch (selectedRole.toLowerCase()) {
				case "student" -> "/fxml/StudentDashboard.fxml";
				case "author" -> "/fxml/AuthorDashboard.fxml";
				case "librarian" -> "/fxml/LibrarianDashboard.fxml";
				default -> "/fxml/Home.fxml";
			};
			try {
				// TODO: pass user details to Student/Author/Librarian dashboard
				Parent dash = FXMLLoader.load(getClass().getResource(fxml));
				Main.getPrimaryStage().setScene(new Scene(dash, 1000, 700));
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
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
		Parent root = loader.load();
		RegisterController ctrl = loader.getController();
		ctrl.setRole(selectedRole);
		Main.getPrimaryStage().setScene(new Scene(root, 640, 480));
	}


	private String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
