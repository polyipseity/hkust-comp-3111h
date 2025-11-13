package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;

import java.io.IOException;

public class AuthorDashboardController {

	/**
	 * Called by FXMLLoader after all @FXML fields are injected.
	 */
	@FXML
	public void initialize() {
	}

	/**
	 * Log out back to the Home screen.
	 */
	@FXML
	private void handleLogout(ActionEvent event) {
		try {
			Main.getContext().setScene(FXMLs.HOME.load());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
