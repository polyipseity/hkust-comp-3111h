// src/main/java/library/controllers/LibrarianDashboardController.java
package library.controllers;

import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;

import java.io.IOException;

public class LibrarianDashboardController {

	/**
	 * Log out back to the Home screen.
	 */
	@FXML
	private void handleLogout() throws IOException {
			Main.getContext().setScene(FXMLs.HOME.load());
	}
}
