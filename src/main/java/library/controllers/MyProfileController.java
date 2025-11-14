package library.controllers;

import javafx.fxml.FXML;
import javafx.scene.text.Text;
import library.Main;
import library.models.User;

public class MyProfileController {
	@FXML
	public Text usernameText;
	private User currentUser;
	private User.Data currentUserData;

	@FXML
	private void initialize() {
		assert Main.getContext().getLoggedInUser() != null;
		currentUser = Main.getContext().getLoggedInUser()._1();
		currentUserData = Main.getContext().getLoggedInUser()._2();
		usernameText.setText(currentUser.username());
	}
}
