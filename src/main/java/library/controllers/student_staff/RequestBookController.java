package library.controllers.student_staff;

import javafx.scene.control.TextField;
import library.Context;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.RequestBooksControl;
import library.models.User;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import org.jetbrains.annotations.UnknownNullability;

public final class RequestBookController implements RequiresLoggedIn {
	private final Context context = Main.getContext();
	private final User user = this.getLoggedInUser()._1();

	@UnknownNullability
	public TextField titleField, authorField;

	public void requestBook() throws TransactionException {
		String title = titleField.getText();
		String author = authorField.getText();
		switch (context.getRequestBooksControl().requestBook(user, title, author)) {
			case RequestBooksControl.RequestResult.Success() -> Alerts.showInfoDialog("Book request submitted.");
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}
}
