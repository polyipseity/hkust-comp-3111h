package library.controllers;

import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;
import library.controllers.common.DependsOnRole;
import library.models.User;

import java.io.IOException;

public class HomeController {
	@FXML
	private void initialize() {
		Main.getContext().setLoggedInUser(null);
	}

	@FXML
	private void handleStudentStaff() throws IOException {
		navigateToLogin(User.Role.STUDENT_STAFF);
	}

	@FXML
	private void handleAuthor() throws IOException {
		navigateToLogin(User.Role.AUTHOR);
	}

	@FXML
	private void handleLibrarian() throws IOException {
		navigateToLogin(User.Role.LIBRARIAN);
	}

	private void navigateToLogin(User.Role role) throws IOException {
		Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.<DependsOnRole>getController().setRole(role)));
	}
}
