package library.controllers.student_staff;

import javafx.application.Platform;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import library.Context;
import library.Main;
import library.controllers.common.CommonDashboardController;
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

public final class DashboardController extends CommonDashboardController implements RequiresLoggedIn {
    private final Context context = Main.getContext();
    private final User user = this.getLoggedInUser()._1();
    Timer timer = new Timer("student_staff_timer");

	@UnknownNullability
	public SplitPane availableBooks;
	@UnknownNullability
	public AvailableBooksController availableBooksController;

	@UnknownNullability
	public BorderPane borrowedBooks;
	@UnknownNullability
    public BorrowedBooksController borrowedBooksController;

    public void loadAvailableBooks() {
	    availableBooksController.loadTable();
    }

    public void loadBorrowedBooks() {
        borrowedBooksController.reload();
    }

    @Override
    public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
        super.initialize(location, resources);

        availableBooksController.setParentController(this);
        borrowedBooksController.setParentController(this);

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
}
