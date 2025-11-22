package library.controllers.student_staff;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import library.controllers.common.InformBoardController;
import library.controllers.common.RequiresLoggedIn;
import org.jetbrains.annotations.UnknownNullability;

public final class DashboardController extends CommonDashboardController implements RequiresLoggedIn {
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

	public void loadInformBoard() {
		informBoardController.loadData();
	}
}
