package library.controllers.author;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import org.jetbrains.annotations.UnknownNullability;

public final class DashboardController extends CommonDashboardController {
	@UnknownNullability
	public Node myBooks;
	@UnknownNullability
	public MyBooksController myBooksController;

	@UnknownNullability
	public Node statusView;
	@UnknownNullability
	public StatusViewController statusViewController;

	public void loadMyBooks() {
		myBooksController.loadTable();
	}

	public void loadStatusView() {
		statusViewController.refresh();
	}
}
