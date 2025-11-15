package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import library.FXMLs;
import library.Main;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.RepositoryBookOps;

import java.io.IOException;
import java.util.List;

public class AuthorDashboardController {
    private final Repository repository = Main.getContext().getRepository();

	/**
	 * Log out back to the Home screen.
	 */
	@FXML
	private void handleLogout() throws IOException {
		Main.getContext().setScene(FXMLs.HOME.load());
	}
}
