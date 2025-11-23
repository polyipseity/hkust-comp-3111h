package library.controllers.common;

import library.FXMLs;
import library.Main;

import java.io.IOException;

/**
 * An abstract base controller providing common functionality for dashboard controllers.
 * <p>
 * This class serves as a foundation for specific dashboard controllers by implementing
 * shared functionalities and enforcing that all subclasses be associated with logged-in users.
 * It implements the {@code RequiresLoggedIn} interface, ensuring integration with user
 * authentication and session management.
 */
public abstract class CommonDashboardController implements RequiresLoggedIn {
    /**
     * Log out back to the Home screen.
     *
     * @throws IOException the io exception
     */
    public void goToLogout() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}
}
