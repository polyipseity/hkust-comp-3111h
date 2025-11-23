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

/**
 * The RequestBookController class handles the functionality for requesting books in the application.
 * This controller ensures the user is logged in and facilitates sending book request information
 * to the underlying request book system.
 * It implements the RequiresLoggedIn interface to enforce user authentication.
 */
public final class RequestBookController implements RequiresLoggedIn {
	private final Context context = Main.getContext();
	private final User user = this.getLoggedInUser()._1();

    /**
     * The Title field.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextField titleField, /**
     * The Author field.
     */
    authorField;

    /**
     * Request book.
     */
    public void requestBook() {
		String title = titleField.getText();
		String author = authorField.getText();
		try {
			switch (context.getRequestBooksControl().requestBook(user, title, author)) {
				case RequestBooksControl.RequestResult.Success() -> Alerts.showInfoDialog("Book request submitted.");
				case HasMessage ret -> Alerts.showErrorDialog(ret.getLocalizedMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}
}
