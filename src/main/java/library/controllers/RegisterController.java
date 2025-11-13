package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import library.FXMLs;
import library.Main;
import library.models.User;
import library.persistence.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class RegisterController {

	@FXML
	private Label headerLabel;

	@FXML
	private TextField usernameField, passwordField, fullNameField;

	@Nullable
	private User.Role role;

	public @NotNull User.Role getRole() {
		return Objects.requireNonNull(role);
	}

	public void setRole(@NotNull User.Role role) {
		this.role = role;
		headerLabel.setText("%s Register".formatted(role.name));
	}

	@FXML
	private void handleBack(ActionEvent event) throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

	@FXML
	private void handleGoToLogin(ActionEvent event) throws IOException {
		Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.<RegisterController>getController().setRole(getRole())));
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
			User.Data newUserData = new User.Data(password, true, getRole(),
					fullName, new ArrayList<>(), new HashMap<>());
			try {
				Main.getContext().getRepository().createUser(newUser, newUserData);
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
