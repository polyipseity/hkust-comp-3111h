package library.controllers;

import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;

import java.io.IOException;

public class StudentDashboardController {

	/**
	 * Log out back to the Home screen.
	 */
	@FXML
	private void handleLogout() {
		try {
			Main.getContext().setScene(FXMLs.HOME.load());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
