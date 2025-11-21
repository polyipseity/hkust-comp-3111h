package library.controllers.author;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
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
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.TimeUtil;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class MyBooksController implements RequiresLoggedIn {
	@UnknownNullability
	public TableView<@Nullable Data> table;
	@UnknownNullability
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	public TableColumn<Data, @Nullable Data> titleColumn, statusColumn, dateColumn, readersColumn, summaryColumn;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleColumn);
		keys.put(Keys.STATUS, statusColumn);
		keys.put(Keys.PUBLISH_DATE, dateColumn);
		keys.put(Keys.TIMES_BORROWED, readersColumn);
		keys.put(Keys.SUMMARY, summaryColumn);
		tableController = new DynamicTableController<>(table, keys);

		loadTable();
	}

	public void loadTable() {
		final var author = new Author.ByRef(getLoggedInUser()._1());
		tableController.setData(Main.getContext().getRepository().bookOps
				.read(entry -> entry.getValue().active() && author.equals(entry.getKey().author()))
				.entrySet().stream().map(entry -> new MyBooksController.Data(this, entry.getKey(), entry.getValue()))
				.toList());
	}

    @FXML
    private void AuthorViewBook() {
	    final var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
	    try {
		    Main.getContext().newWindow(
				    TextViewController.WINDOW_TITLE.formatted(selected.book.title()),
				    FXMLs.COMMON_TEXT_VIEW.load(loader -> loader.<TextViewController>getController().setContent(selected.bookData.content())),
				    null
		    ).show();
	    } catch (IOException e) {
		    Alerts.showErrorDialog(e.getLocalizedMessage());
	    }
    }

	@FXML
	private void AuthorModifyBook() throws IOException {
		final var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
	        Alerts.showErrorDialog("Please select a book first.");
	        return;
        }

            // Load the FXML file for the new window's content
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/author/ModifyWindow.fxml"));
		Parent root = fxmlLoader.load();

            //Passing content to new window
            ModifyWindowController controller = fxmlLoader.getController();
		controller.setBookEntry(new Tuple2<>(selected.book, selected.bookData));
		controller.modifyCallback = this::loadTable;

		// Create a new Stage (window)
		final var window = Main.getContext().newWindow(
				"Modify Book",
				root,
				null
		);

            // Show the new window
		window.show();
    }

	@FXML
	private void AuthorDeleteBook() {
		final var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
		if (!deleteConfirmation(selected.book.title())) {
			return;
		}

		try {
			switch (Main.getContext().getManageBooksControl().deleteBook(selected.book, getLoggedInUser()._2().role())) {
				//Reload table after deleting a book
				case ManageBooksControl.DeleteResult.Success _ -> loadTable();
				case HasMessage message -> Alerts.showErrorDialog(message.getMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}

	private boolean deleteConfirmation(String message) {
        Optional<ButtonType> result = Alerts.showConfirmDialog("Delete \"" + message + "\"?");
        // Return true if confirmed
        return !(result.isPresent() && result.get() == ButtonType.CANCEL);
    }

	public enum Keys {
		TITLE,
		STATUS,
		PUBLISH_DATE,
		TIMES_BORROWED,
		SUMMARY
	}

	public record Data(MyBooksController controller,
	                   Book book,
	                   Book.Data bookData)
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
				case TIMES_BORROWED -> new DynamicTableController.Data.Text(String.valueOf(bookData.timesBorrowed()));
				case SUMMARY -> new DynamicTableController.Data.Text(bookData.summary());
			};
		}
	}
}
