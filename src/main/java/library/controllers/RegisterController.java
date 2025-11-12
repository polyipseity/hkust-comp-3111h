package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import library.Main;
import library.models.BookRequest;
import library.models.User;
import library.persistence.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterController {

	@FXML
	private Label headerLabel;

	@FXML
	private TextField usernameField, passwordField, fullNameField;

	private String selectedRole;

	public void setRole(String role) {
		this.selectedRole = role;
		headerLabel.setText(capitalize(role) + " Register");
	}

	@FXML
	private void handleBack(ActionEvent event) throws IOException {
		Parent home = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
		Main.getPrimaryStage().setScene(new Scene(home, 640, 480));
	}

	@FXML
	private void handleGoToLogin(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
		Parent root = loader.load();
		LoginController ctrl = loader.getController();
		ctrl.setRole(selectedRole);
		Main.getPrimaryStage().setScene(new Scene(root, 640, 480));
	}

	@FXML
	private void handleRegistration(ActionEvent event) throws IOException {
		Alert errorAlert = new Alert(Alert.AlertType.ERROR, "");
		errorAlert.initModality(Modality.APPLICATION_MODAL);

		Alert infoAlert = new Alert(Alert.AlertType.INFORMATION, "");
		infoAlert.initModality(Modality.APPLICATION_MODAL);

		String username = usernameField.getText();
		String password = passwordField.getText();
		String fullName = fullNameField.getText();

		if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
			errorAlert.getDialogPane().setContentText("One or more input fields are blank");
			errorAlert.showAndWait();
		} else {
			User newUser = new User(username);
			User.Role role = switch (this.selectedRole) {
				case "student" -> User.Role.STUDENT_STAFF;
				case "author" -> User.Role.AUTHOR;
				case "librarian" -> User.Role.LIBRARIAN;
				default -> throw new IllegalStateException("Unexpected value: " + this.selectedRole);
			};
			User.Data newUserData = new User.Data(password, true, User.Role.STUDENT_STAFF,
					fullName, new ArrayList<>(), new HashMap<>());
			try {
				Main.getRepository().createUser(newUser, newUserData);
				infoAlert.getDialogPane().setContentText("Account successfully created");
				infoAlert.showAndWait();
			} catch (Repository.TransactionException e) {
				errorAlert.getDialogPane().setContentText("Duplicate username found in database");
				errorAlert.showAndWait();
			}
		}
	}

	private String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
