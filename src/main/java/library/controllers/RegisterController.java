package library.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import library.FXMLs;
import library.Main;
import library.controls.ManageProfileControl;
import library.controls.UserValidator;
import library.models.User;
import library.persistence.Repository;
import library.utils.Alerts;
import library.utils.HasMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public class RegisterController implements DependsOnRole {

	@FXML
	private Label headerLabel;

	@FXML
	private TextField usernameField, passwordField, fullNameField;

	@Nullable
	private User.Role role;

	@FXML
	private void initialize() {
		Main.getContext().setLoggedInUser(null);
	}

	@Override
	public @NotNull User.Role getRole() {
		return Objects.requireNonNull(role);
	}

	@Override
	public void setRole(@NotNull User.Role role) {
		this.role = role;
		headerLabel.setText("%s Register".formatted(role.name));
	}

	@FXML
	private void handleBack() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}

	@FXML
	private void handleGoToLogin() throws IOException {
		Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.<DependsOnRole>getController().setRole(getRole())));
	}

	@FXML
	private void handleRegistration() throws IOException {
		String username = usernameField.getText();
		String password = passwordField.getText();
		String fullName = fullNameField.getText();

		final var context = Main.getContext();
		try {
			switch (context.manageProfile.register(UserValidator.DEFAULT, getRole(), username, password, fullName)) {
				case ManageProfileControl.RegisterResult.Success val -> {
					Alerts.showInfoDialog(val.getMessage());
					context.setScene(FXMLs.LOGIN.load(loader -> loader.<DependsOnRole>getController().setRole(getRole())));
				}
				case HasMessage val -> Alerts.showErrorDialog(val.getMessage());
			}
		} catch (Repository.TransactionException exception) {
			Alerts.showErrorDialog(exception.getMessage());
		}
	}
}
