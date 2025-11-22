package library.controllers;

import library.FXMLs;
import library.Main;
import library.controllers.common.RequiresLoggedOut;
import library.models.User;

import java.io.IOException;

public final class HomeController implements RequiresLoggedOut {
	public void handleStudentStaff() throws IOException {
		goToLogin(User.Role.STUDENT_STAFF);
	}

	public void handleAuthor() throws IOException {
		goToLogin(User.Role.AUTHOR);
	}

	public void handleLibrarian() throws IOException {
		goToLogin(User.Role.LIBRARIAN);
	}

	private void goToLogin(User.Role role) throws IOException {
		Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.setControllerFactory(_ -> new LoginController(role))));
	}
}
