package library.controllers.common;

import library.FXMLs;
import library.Main;

import java.io.IOException;

public abstract class CommonDashboardController implements RequiresLoggedIn {
	/**
	 * Log out back to the Home screen.
	 */
	public void goToLogout() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}
}
