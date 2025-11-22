package library.controllers.author;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import library.controllers.common.InformBoardController;
import org.jetbrains.annotations.UnknownNullability;

public final class DashboardController extends CommonDashboardController {
	@UnknownNullability
	@SuppressWarnings("unused")
	public Node myBooks;
	@UnknownNullability
	@SuppressWarnings("unused")
	public MyBooksController myBooksController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node statusView;
	@UnknownNullability
	@SuppressWarnings("unused")
	public StatusViewController statusViewController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node informBoard;
	@UnknownNullability
	@SuppressWarnings("unused")
	public InformBoardController informBoardController;

	public void loadMyBooks() {
		myBooksController.loadData();
	}

	public void loadStatusView() {
		statusViewController.loadData();
	}

	public void loadInformBoard() {
		informBoardController.loadData();
	}
}
