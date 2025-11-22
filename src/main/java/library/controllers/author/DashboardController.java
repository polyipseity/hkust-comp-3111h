package library.controllers.author;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
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

	public void loadMyBooks() {
		myBooksController.loadTable();
	}

	public void loadStatusView() {
		statusViewController.refresh();
	}
}
