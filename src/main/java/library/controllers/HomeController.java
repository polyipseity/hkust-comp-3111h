package library.controllers;

import library.FXMLs;
import library.Main;
import library.controllers.common.RequiresLoggedOut;
import library.models.User;

import java.io.IOException;

/**
 * The HomeController class manages the navigation to the login screen
 * for different user roles. It ensures that the user is logged out
 * before initializing any operations. The class implements the
 * RequiresLoggedOut interface to enforce this requirement.
 *
 * This class provides methods for handling specific roles and
 * transitions the application to the login screen with the appropriate
 * setup based on the role.
 */
public final class HomeController implements RequiresLoggedOut {
    /**
     * Handle student staff.
     *
     * @throws IOException the io exception
     */
    public void handleStudentStaff() throws IOException {
		goToLogin(User.Role.STUDENT_STAFF);
	}

    /**
     * Handle author.
     *
     * @throws IOException the io exception
     */
    public void handleAuthor() throws IOException {
		goToLogin(User.Role.AUTHOR);
	}

    /**
     * Handle librarian.
     *
     * @throws IOException the io exception
     */
    public void handleLibrarian() throws IOException {
		goToLogin(User.Role.LIBRARIAN);
	}

	private void goToLogin(User.Role role) throws IOException {
		Main.getContext().setScene(FXMLs.LOGIN.load(loader -> loader.setControllerFactory(_ -> new LoginController(role))));
	}
}
