package library.controllers.author;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;

public final class DashboardController extends CommonDashboardController {
	@UnknownNullability
	public Node myBooks;
	@UnknownNullability
	public MyBooksController myBooksController;

	@UnknownNullability
	public Node publishBooks;
	@UnknownNullability
	public PublishBooksController publishBooksController;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		super.initialize(location, resources);

		myBooksController.setParentController(this);
		publishBooksController.setParentController(this);
	}
}
