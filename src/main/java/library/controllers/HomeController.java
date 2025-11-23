package library.controllers;

import library.FXMLs;
import library.Main;
import library.controllers.common.RequiresLoggedOut;
import library.models.User;

import java.io.IOException;

/**
 * The type Home controller.
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
