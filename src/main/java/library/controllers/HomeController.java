package library.controllers;

import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;
import library.models.User;

public class HomeController {
	@FXML
	private void initialize() {
		Main.getContext().setLoggedInUser(null);
	}

	@FXML
	private void handleStudent() {
		navigateToLogin(User.Role.STUDENT_STAFF);
	}

	@FXML
	private void handleAuthor() {
		navigateToLogin(User.Role.AUTHOR);
	}

	@FXML
	private void handleLibrarian() {
		navigateToLogin(User.Role.LIBRARIAN);
	}

	private void navigateToLogin(User.Role role) {
		try {
			Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.<LoginController>getController().setRole(role)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
