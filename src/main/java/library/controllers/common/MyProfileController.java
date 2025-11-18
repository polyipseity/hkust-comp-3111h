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

public class MyProfileController {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();
	private final User user = Main.getContext().getLoggedInUser()._1();
	private final User.Data userData = Main.getContext().getLoggedInUser()._2();

	@FXML
	public Text usernameText;
	@FXML
	public TextField newNameField, newPasswordField;

	@FXML
	private void initialize() {
		usernameText.setText(user.username());
	}

	@FXML
	private void updateProfile() {
		String enteredPassword = newPasswordField.getText();
		String enteredFullName = newNameField.getText();
		if (enteredPassword.isEmpty() && enteredFullName.isEmpty()) {
			Alerts.showErrorDialog("Both input fields are empty");
			return;
		}
		String newPassword = enteredPassword.isEmpty() ? userData.password() : enteredPassword;
		String newFullName = enteredFullName.isEmpty() ? userData.fullName() : enteredFullName;
		UserValidator.Result result = UserValidator.DEFAULT.apply(user.username(), newPassword, newFullName);
		switch (result) {
			case UserValidator.Result.Success() -> {
				User.Data newUserData = userData
						.withPassword(newPassword)
						.withFullName(newFullName);
				try {
					repository.userOps.update(user, (_) -> newUserData);
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
