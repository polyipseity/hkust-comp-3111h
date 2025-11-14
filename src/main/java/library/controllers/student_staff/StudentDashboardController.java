package library.controllers.student_staff;

import javafx.fxml.FXML;
import library.FXMLs;
import library.Main;

import java.io.IOException;

public class StudentDashboardController {

	/**
	 * Log out back to the Home screen.
	 */
	@FXML
	private void handleLogout() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}
}
