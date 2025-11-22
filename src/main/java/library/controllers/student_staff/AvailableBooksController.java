package library.controllers.student_staff;

import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.LoadsData;
import library.controllers.common.RequiresLoggedIn;
import library.controls.BorrowBooksControl;
import library.models.Book;
import library.models.Borrow;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.TimeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.*;
import java.util.function.Function;

public final class AvailableBooksController implements RequiresLoggedIn, Initializable, LoadsData {
	private final Map<Book, Borrow> activeBorrows = new HashMap<>();
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
	@UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, authorCol, publishDateCol, summaryCol;
	@UnknownNullability
	@SuppressWarnings("unused")
	public Node sidebar;
	@UnknownNullability
	@SuppressWarnings("unused")
	public Text titleText, authorText, publishDateText, summaryText;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR_FULL_NAME, authorCol);
		keys.put(Keys.PUBLISH_DATE, publishDateCol);
		keys.put(Keys.SUMMARY, summaryCol);
		tableController = new DynamicTableController<>(table, keys);
		publishDateCol.setSortType(TableColumn.SortType.DESCENDING);
		table.getSortOrder().add(publishDateCol);

		LoadsData.super.initialize(location, resources);

		final var selectedItem = table.getSelectionModel().selectedItemProperty();
		titleText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.title()).orElse("").orElse(""));
		authorText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.authorFullName).orElse("").orElse(""));
		publishDateText.textProperty().bind(selectedItem.map(item -> item == null || item.bookData.publishDate() == null ? "" : TimeUtil.toStringZonedLocal(item.bookData.publishDate())).orElse("").orElse(""));
		summaryText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.bookData.summary()).orElse("").orElse(""));

		final var context = Main.getContext();
		final var repository = context.getRepository();
		context.addSecondTimelineListener(getLoggedInToken(), _ -> activeBorrows.entrySet().removeIf(entry -> {
			if (!entry.getValue().expired()) return false;
			final var key = entry.getKey();
			final var value = repository.bookOps.read(key);
			value.ifPresent(val -> tableController.addDatum(new Data(this, key, val, repository.userOps.readFullName(key.author()))));
			return true;
		}));
	}

	@Override
	public void loadData() {
		final var context = Main.getContext();
		final var repository = context.getRepository();

		activeBorrows.clear();
		activeBorrows.putAll(repository.borrowOps.read(getLoggedInUser()._1()));

		tableController.setData(context.getBorrowBooksControl()
				.getBorrowableBooks(getLoggedInUser()._1())
				.entrySet()
				.stream()
				.map(entry ->
						new Data(this, entry.getKey(), entry.getValue(), repository.userOps.readFullName(entry.getKey().author())))
				.toList());
	}

	/**
	 * Runs each time the "Borrow Book" button is pressed.
	 */
	public void borrowSelectedBook() {
		final var context = Main.getContext();
		final var currentRow = table.getSelectionModel().getSelectedItem();
		if (currentRow == null) {
			Alerts.showErrorDialog("Please select a book first.");
			return;
		}
		final var selectedBook = currentRow.book;

		// Try to get the borrow duration for the book
		Dialog<@Nullable dialogResult> dialog = createDialog();
		Optional<dialogResult> result = dialog.showAndWait();
		if (result.isEmpty()) return; // Exit if user clicks "Cancel"

		// Parse results from input dialog
		int minutes, seconds;
		try {
			minutes = Integer.parseInt(result.get().minutes);
			seconds = Integer.parseInt(result.get().seconds);
		} catch (NumberFormatException e) {
			Alerts.showErrorDialog("Entered values could not be parsed correctly.");
			return;
		}


		try {
			switch (context.getBorrowBooksControl().borrowBook(getLoggedInUser()._1(), selectedBook, minutes, seconds)) {
				case BorrowBooksControl.BorrowResult.Success(final var borrow) -> {
					Alerts.showInfoDialog("Book borrowed successfully");
					// Only after showing the info dialog, change the UI
					tableController.removeDatum(currentRow);
					activeBorrows.put(selectedBook, borrow); // Placed afterwards, in case it expired quickly
				}
				case HasMessage ret -> Alerts.showErrorDialog(ret.getLocalizedMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}

	/**
	 * @return A properly configured input dialog for entering the borrowing duration.
	 */
	private Dialog<@Nullable dialogResult> createDialog() {
		Dialog<@Nullable dialogResult> dialog = new Dialog<>();
		dialog.setTitle("Borrow Duration");
		dialog.setHeaderText("Please enter the borrowing duration (minutes and seconds).");

		TextField minutesTextField = new TextField();
		TextField secondsTextField = new TextField();

		GridPane grid = new GridPane();
		grid.add(new Label("Minutes:"), 0, 0);
		grid.add(new Label("Seconds:"), 0, 1);
		grid.add(minutesTextField, 1, 0);
		grid.add(secondsTextField, 1, 1);
		grid.setHgap(10);
		grid.setVgap(10);
		dialog.getDialogPane().setContent(grid);

		ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);
		dialog.setResultConverter(b ->
				b == buttonTypeOk ? new dialogResult(minutesTextField.getText(), secondsTextField.getText()) : null);

		return dialog;
	}

	public enum Keys {
		TITLE,
		AUTHOR_FULL_NAME,
		PUBLISH_DATE,
		SUMMARY
	}

	public record dialogResult(String minutes, String seconds) {
	}

	public record Data(AvailableBooksController controller,
	                   Book book,
	                   Book.Data bookData,
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
				case PUBLISH_DATE -> new DynamicTableController.Data.Text(
						bookData.publishDate() == null
								? ""
								: TimeUtil.toStringZonedLocal(bookData.publishDate()), bookData.publishDate());
				case SUMMARY -> new DynamicTableController.Data.Text(bookData.summary());
			};
		}
	}
}
