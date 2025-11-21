package library.controllers.student_staff;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import library.Context;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.RequestBooksControl;
import library.models.User;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public final class RequestBookController implements RequiresLoggedIn {
	private final Context context = Main.getContext();
	private final User user = this.getLoggedInUser()._1();

	@FXML
	private TextField titleField, authorField;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
	}

	@FXML
	private void requestBookButtonAction() throws TransactionException {
		String title = titleField.getText();
		String author = authorField.getText();
		switch (context.getRequestBooksControl().requestBook(user, title, author)) {
			case RequestBooksControl.RequestResult.Success() -> Alerts.showInfoDialog("Book request submitted.");
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}
}
