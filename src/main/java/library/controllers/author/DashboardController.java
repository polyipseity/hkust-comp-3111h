package library.controllers.author;

import javafx.fxml.Initializable;
import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;

public final class DashboardController extends CommonDashboardController implements Initializable {
	@UnknownNullability
	public Node myBooks;
	@UnknownNullability
	public MyBooksController myBooksController;

	@UnknownNullability
	public Node publishBooks;
	@UnknownNullability
	public PublishBooksController publishBooksController;

	@UnknownNullability
	public Node statusView;
	@UnknownNullability
	public StatusViewController statusViewController;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		super.initialize(location, resources);
		publishBooksController.setParentController(this);
		myBooksController.setParentController(this);
	}

	public void loadMyBooks() {
		myBooksController.loadTable();
	}

	public void loadStatusView() {
		statusViewController.refresh();
	}
}
