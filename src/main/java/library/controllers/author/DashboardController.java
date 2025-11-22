package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
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

    @FXML
    private Tab statusViewTab;

	public void loadMyBooks() {
		myBooksController.loadData();
	}

	public void loadStatusView() {
        if(statusViewTab.isSelected()){
            statusViewController.loadData();
        }
	}

	public void loadInformBoard() {
		informBoardController.loadData();
	}
}
