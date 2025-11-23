package library.controllers.student_staff;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import library.controllers.common.InformBoardController;
import library.controllers.common.RequiresLoggedIn;
import org.jetbrains.annotations.UnknownNullability;

/**
 * DashboardController is a final class that extends CommonDashboardController and implements
 * RequiresLoggedIn. It serves as the controller for handling functionality in the user dashboard,
 * including operations like loading available books, borrowed books, and the information board.
 *
 * This controller is specifically designed to manage different aspects of a user's dashboard
 * and relies on sub-controllers to handle specific components.
 */
public final class DashboardController extends CommonDashboardController implements RequiresLoggedIn {
    /**
     * The Available books.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node availableBooks;
    /**
     * The Available books controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public AvailableBooksController availableBooksController;

    /**
     * The Borrowed books.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node borrowedBooks;
    /**
     * The Borrowed books controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public BorrowedBooksController borrowedBooksController;

    /**
     * The Inform board.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node informBoard;
    /**
     * The Inform board controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public InformBoardController informBoardController;

    /**
     * Load available books.
     */
    public void loadAvailableBooks() {
		availableBooksController.loadData();
	}

    /**
     * Load borrowed books.
     */
    public void loadBorrowedBooks() {
		borrowedBooksController.loadData();
	}

    /**
     * Load inform board.
     */
    public void loadInformBoard() {
		informBoardController.loadData();
	}
}
