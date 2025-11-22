package library.controllers.common;

import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import library.Context;
import library.Main;
import library.controls.UserValidator;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;

public final class MyProfileController implements RequiresLoggedIn, Initializable {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();

	@UnknownNullability
	public Text usernameText;
	@UnknownNullability
	public TextField newNameField, newPasswordField;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		usernameText.setText(getLoggedInUser()._1().username());
	}

	public void updateProfile() {
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
				try {
					repository.userOps.update(getLoggedInUser()._1(), user -> user
							.withPassword(newPassword)
							.withFullName(newFullName));
					Alerts.showInfoDialog("Updated successfully");
				} catch (TransactionException e) {
					Alerts.showErrorDialog(e.getLocalizedMessage());
				}
			}
			case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
		}
	}
}
