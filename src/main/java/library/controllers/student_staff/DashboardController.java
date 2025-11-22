package library.controllers.student_staff;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import library.Context;
import library.Main;
import library.controllers.common.CommonDashboardController;
import library.controllers.common.InformBoardController;
import library.controllers.common.RequiresLoggedIn;
import library.controls.BorrowBooksControl;
import library.models.Book;
import library.models.User;
import library.persistence.TransactionException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public final class DashboardController extends CommonDashboardController implements RequiresLoggedIn, Initializable {
	final Timer timer = new Timer("student_staff_timer");
	private final Context context = Main.getContext();
	private final User user = this.getLoggedInUser()._1();

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node availableBooks;
	@UnknownNullability
	@SuppressWarnings("unused")
	public AvailableBooksController availableBooksController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node borrowedBooks;
	@UnknownNullability
	@SuppressWarnings("unused")
	public BorrowedBooksController borrowedBooksController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node informBoard;
	@UnknownNullability
	@SuppressWarnings("unused")
	public InformBoardController informBoardController;

	public void loadAvailableBooks() {
		availableBooksController.loadData();
	}

	public void loadBorrowedBooks() {
		borrowedBooksController.loadData();
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		super.initialize(location, resources);

		availableBooksController.setParentController(this);

		context.getBorrowBooksControl().returnExpiredBooks(user);
		loadAvailableBooks();
		loadBorrowedBooks();

		context
				.getBorrowBooksControl()
				.getBorrowDurations(user)
				.forEach((book, duration) -> timer.schedule(createReturnTask(book), duration.toMillis()));
	}

	private TimerTask createReturnTask(Book book) {
		return new TimerTask() {
			@Override
			public void run() {
				try {
					BorrowBooksControl.ReturnResult result =
							context.getBorrowBooksControl().returnBook(user, book);
					// Only reload tabs if a book is actually returned
					if (result instanceof BorrowBooksControl.ReturnResult.Success) {
						Platform.runLater(() -> {
							loadAvailableBooks();
							loadBorrowedBooks();
						});
					}
				} catch (TransactionException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	public void scheduleReturn(Book book, long millis) {
		timer.schedule(createReturnTask(book), millis);
	}

	public void loadInformBoard() {
		informBoardController.loadData();
	}
}
