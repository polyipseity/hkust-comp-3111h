package library.controllers.common;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import library.Context;
import library.Main;
import library.controls.UserValidator;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public class MyProfileController implements RequiresLoggedIn {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();

	@FXML
	public Text usernameText;
	@FXML
	public TextField newNameField, newPasswordField;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		usernameText.setText(getLoggedInUser()._1().username());
	}

	@FXML
	private void updateProfile() {
		String enteredPassword = newPasswordField.getText();
		String enteredFullName = newNameField.getText();
		if (enteredPassword.isEmpty() && enteredFullName.isEmpty()) {
			Alerts.showErrorDialog("Both input fields are empty");
			return;
		}
		String newPassword = enteredPassword.isEmpty() ? getLoggedInUser()._2().password() : enteredPassword;
		String newFullName = enteredFullName.isEmpty() ? getLoggedInUser()._2().fullName() : enteredFullName;
		UserValidator.Result result = UserValidator.DEFAULT.apply(getLoggedInUser()._1().username(), newPassword, newFullName);
		switch (result) {
			case UserValidator.Result.Success() -> {
				User.Data newUserData = getLoggedInUser()._2()
						.withPassword(newPassword)
						.withFullName(newFullName);
				try {
					repository.userOps.update(getLoggedInUser()._1(), (_) -> newUserData);
					Alerts.showInfoDialog("Updated successfully");
				} catch (TransactionException e) {
					Alerts.showErrorDialog("Update Failed: " + e.getMessage());
				}
			}
			case UserValidator.Result.BadFullName(String message) -> Alerts.showErrorDialog(message);
			case UserValidator.Result.BadPassword(String message) -> Alerts.showErrorDialog(message);
			default -> throw new IllegalStateException("Unexpected value: " + result);
		}
	}
}
