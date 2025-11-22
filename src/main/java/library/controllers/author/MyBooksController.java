package library.controllers.author;

import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.FXMLs;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controllers.common.TextViewController;
import library.controls.ManageBooksControl;
import library.models.Author;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.TimeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class MyBooksController implements RequiresLoggedIn, Initializable {
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
	@UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, statusCol, dateCol, readersCol, summaryCol;
	@UnknownNullability
	@SuppressWarnings("unused")
	public Button viewButton, modifyButton, deleteButton;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.STATUS, statusCol);
		keys.put(Keys.PUBLISH_DATE, dateCol);
		keys.put(Keys.READERS, readersCol);
		keys.put(Keys.SUMMARY, summaryCol);
		tableController = new DynamicTableController<>(table, keys);

		final var selectedItem = table.getSelectionModel().selectedItemProperty();
		viewButton.disableProperty().bind(selectedItem.isNull());
		modifyButton.disableProperty().bind(selectedItem.map(item -> item == null || !item.borrows.isEmpty()).orElse(true));
		deleteButton.disableProperty().bind(selectedItem.map(item -> item == null || !item.borrows.isEmpty()).orElse(true));

		loadTable();
	}

	public void loadTable() {
		final var author = new Author.ByRef(getLoggedInUser()._1());
		final var repository = Main.getContext().getRepository();
		tableController.setData(repository.bookOps
				.read(entry -> entry.getValue().active() && author.equals(entry.getKey().author()))
				.entrySet().stream().map(entry ->
						new MyBooksController.Data(
								this,
								entry.getKey(),
								entry.getValue(),
								repository.borrowOps.read(entry.getKey())
						))
				.toList());
	}

	public void viewBook() throws IOException {
		final var selected = table.getSelectionModel().getSelectedItem();
		if (selected == null) {
			// Button should be disabled
			return;
		}
		Main.getContext().newWindow(
				TextViewController.WINDOW_TITLE.formatted(selected.book.title(), selected.book.author().id()),
				FXMLs.COMMON_TEXT_VIEW.<Parent>load(loader -> loader.setControllerFactory(_ -> new TextViewController(selected.bookData.content()))),
				null
		).show();
	}

	public void modifyBook() throws IOException {
		final var selected = table.getSelectionModel().getSelectedItem();
		if (selected == null || !selected.borrows.isEmpty()) {
			// Button should be disabled
			return;
		}
		Main.getContext().newWindow(
				"Modify Book",
				// Load the FXML file for the new window's content
				stage -> FXMLs.AUTHOR_MODIFY_WINDOW.<Parent>load(loader ->
						loader.setControllerFactory(_ -> new ModifyWindowController(stage, selected.book, selected.bookData, this::loadTable))),
				null
		).show();
	}

	public void deleteBook() {
		final var selected = table.getSelectionModel().getSelectedItem();
		if (selected == null || !selected.borrows.isEmpty()) {
			// Button should be disabled
			return;
		}

		if (!Alerts.showConfirmDialog("Delete '%s'?".formatted(selected.book.title()))) {
			return;
		}
		try {
			switch (Main.getContext().getManageBooksControl().deleteBook(selected.book, getLoggedInUser()._2().role())) {
				//Reload table after deleting a book
				case ManageBooksControl.DeleteResult.Success _ -> loadTable();
				case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}

	public enum Keys {
		TITLE,
		STATUS,
		PUBLISH_DATE,
		READERS,
		SUMMARY
	}

	public record Data(MyBooksController controller,
	                   Book book,
	                   Book.Data bookData,
	                   Map<User, Borrow> borrows)
			implements Function<Keys, DynamicTableController.Data> {
		/**
		 * Applies this function to the given argument.
		 *
		 * @param key the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.Data apply(Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Text(book.title());
				case STATUS -> new DynamicTableController.Data.Text(bookData.approvalStatus().name);
				case PUBLISH_DATE -> new DynamicTableController.Data.Text(
						bookData.publishDate() == null
								? ""
								: TimeUtil.toStringZonedLocal(bookData.publishDate()));
				case READERS -> new DynamicTableController.Data.Text(String.valueOf(borrows.size()));
				case SUMMARY -> new DynamicTableController.Data.Text(bookData.summary());
			};
		}
	}
}
