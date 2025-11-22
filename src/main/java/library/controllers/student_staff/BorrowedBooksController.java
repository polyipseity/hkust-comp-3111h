package library.controllers.student_staff;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import library.FXMLs;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controls.BorrowBooksControl;
import library.models.Book;
import library.models.Borrow;
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
import java.util.ResourceBundle;
import java.util.function.Function;

public final class BorrowedBooksController implements RequiresLoggedIn, Initializable {
	@UnknownNullability
	public TableView<@Nullable Data> table;
	@UnknownNullability
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	public TableColumn<Data, @Nullable Data> titleCol, authorCol, borrowedOnCol, timeLeftCol, actionsCol;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR_FULL_NAME, authorCol);
		keys.put(Keys.BORROW_DATE, borrowedOnCol);
		keys.put(Keys.TIME_LEFT, timeLeftCol);
		keys.put(Keys.ACTIONS, actionsCol);
		tableController = new DynamicTableController<>(table, keys);

		loadTable();
	}

	public void loadTable() {
		final var repository = Main.getContext().getRepository();
		tableController.setData(
				Main.getContext().getRepository().borrowOps.read(getLoggedInUser()._1()).entrySet().stream()
						.map(entry ->
								new Data(this, entry.getKey(), entry.getValue(), repository.userOps.readFullName(entry.getKey().author())))
						.toList()
		);
	}

	public void readSelectedBook() throws IOException {
		final var context = Main.getContext();

		final var currentRow = table.getSelectionModel().getSelectedItem();
		if (currentRow == null) {
			Alerts.showErrorDialog("Please select a book first.");
			return;
		}

		final var selectedBook = currentRow.book;
		final var title = currentRow.book.title();
		final var author = currentRow.authorFullName;

		switch (context.getBorrowBooksControl().readBook(getLoggedInUser()._1(), selectedBook)) {
			case BorrowBooksControl.ReadResult.Success(String path) -> displayPdfFile(path, title, author);
			case BorrowBooksControl.ReadResult.NewPdfGenerated(String path) -> {
				Alerts.showInfoDialog("PDF file not found, generating a new one...");
				displayPdfFile(path, title, author);
			}
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}

	/**
	 * Executes when the "Return" button of the table in the "My Borrowed Books" tab is pressed.
	 *
	 * @param book The book to be returned by the current user,
	 *             which should be the book pointed to by the button's table row.
	 */
	private void returnButtonAction(Book book) {
		try {
			switch (Main.getContext().getBorrowBooksControl().returnBook(getLoggedInUser()._1(), book)) {
				case BorrowBooksControl.ReturnResult.Success _ -> Alerts.showInfoDialog("Book returned successfully");
				case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog("Unknown error occurred: " + e.getMessage());
		}
	}

	private void displayPdfFile(String path, String title, String author) throws IOException {
		final var controller = new BookViewController(path);

		Stage stage = new Stage();
		stage.setScene(new Scene(FXMLs.STUDENT_STAFF_BOOK_VIEW.load(loader -> loader.setControllerFactory(_ -> controller))));
		stage.setTitle("Reading: " + title + " by " + author);
		stage.setOnShown(controller::createResizeListeners);
		stage.setOnCloseRequest(controller::disposeController);
		stage.show();
	}

	public enum Keys {
		TITLE,
		AUTHOR_FULL_NAME,
		BORROW_DATE,
		TIME_LEFT,
		ACTIONS
	}

	public record Data(BorrowedBooksController controller,
	                   Book book,
	                   Borrow borrow,
	                   String authorFullName)
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
				case AUTHOR_FULL_NAME -> new DynamicTableController.Data.Text(authorFullName);
				case BORROW_DATE -> new DynamicTableController.Data.Text(TimeUtil.toStringZonedLocal(borrow.borrowDate()));
				case TIME_LEFT -> {
					final var borrow = this.borrow; // Do not reference `this` in lambda.
					final var prop = new SimpleStringProperty(TimeUtil.toStringDuration(borrow.durationLeft()));
					final var ret = new DynamicTableController.Data.ObservableText(prop);
					Main.getContext().addSecondTimerListener(ret, _ ->
							prop.setValue(TimeUtil.toStringDuration(borrow.durationLeft())));
					yield ret;
				}
				case ACTIONS -> DynamicTableController.Data.Graphic.ofButtons(
						new Tuple2<>(new SimpleStringProperty("Return"), (_, _) ->
								controller.returnButtonAction(book))
				);
			};
		}
	}
}
