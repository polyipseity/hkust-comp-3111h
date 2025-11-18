package library.controllers.common;

import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;

import java.io.IOException;

public abstract class DashboardController implements RequiresLoggedOut {
	/**
	 * Log out back to the Home screen.
	 */
	@FXML
	private void goToLogout() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}
}
