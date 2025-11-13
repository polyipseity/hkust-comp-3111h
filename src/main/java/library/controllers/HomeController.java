package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import library.FXMLs;
import library.Main;

public class HomeController {
	@FXML
	private void handleStudent(ActionEvent event) {
		navigateToLogin("student");
	}

	@FXML
	private void handleAuthor(ActionEvent event) {
		navigateToLogin("author");
	}

	@FXML
	private void handleLibrarian(ActionEvent event) {
		navigateToLogin("librarian");
	}

	private void navigateToLogin(String role) {
		try {
			FXMLLoader loader = FXMLs.LOGIN.loader();
			Parent root = loader.load();

			// Pass the role to LoginController
			LoginController ctrl = loader.getController();
			ctrl.setRole(role);

			Main.getContext().setScene(root);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
