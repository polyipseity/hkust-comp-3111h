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

/**
 * A controller class for handling the profile-related operations of the currently logged-in user.
 * This class requires the user to be logged in and is responsible for initializing UI elements
 * and managing profile updates such as changing the username or password.
 */
public final class MyProfileController implements RequiresLoggedIn, Initializable {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();

    /**
     * The Username text.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Text usernameText;
    /**
     * The New name field.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextField newNameField, /**
     * The New password field.
     */
    newPasswordField;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		usernameText.setText(getLoggedInUser()._1().username());
	}

    /**
     * Update profile.
     */
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
				} catch (TransactionException e) {
					Alerts.showErrorDialog(e.getLocalizedMessage());
				}
			}
			case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
		}
		Alerts.showInfoDialog("Updated successfully");
	}
}
