package library.controllers.student_staff;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.FXMLs;
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
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * BorrowedBooksController handles the functionality for managing and displaying
 * the user's borrowed books within the application. It manages the initialization,
 * data loading, and UI interactions for the borrowed books table, enforcing that
 * a user must be logged in to access the features. This controller supports
 * dynamically updating the displayed data and interacting with borrowed books
 * such as reading or returning them.
 *
 * This class implements {@code RequiresLoggedIn}, {@code Initializable}, and
 * {@code LoadsData} interfaces to ensure proper initialization, user session validation,
 * and data handling.
 */
public final class BorrowedBooksController implements RequiresLoggedIn, Initializable, LoadsData {
    /**
     * The Table.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
    /**
     * The Table controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;
    /**
     * The Title col.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, /**
     * The Author col.
     */
    authorCol, /**
     * The Borrowed on col.
     */
    borrowedOnCol, /**
     * The Duration left col.
     */
    durationLeftCol, /**
     * The Actions col.
     */
    actionsCol;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR_FULL_NAME, authorCol);
		keys.put(Keys.BORROW_DATE, borrowedOnCol);
		keys.put(Keys.DURATION_LEFT, durationLeftCol);
		keys.put(Keys.ACTIONS, actionsCol);
		tableController = new DynamicTableController<>(table, keys);
		table.getSortOrder().add(durationLeftCol);

		LoadsData.super.initialize(location, resources);
	}

	@Override
	public void loadData() {
		final var repository = Main.getContext().getRepository();
		tableController.setData(
				Main.getContext().getRepository().borrowOps.read(getLoggedInUser()._1()).entrySet().stream()
						.map(entry ->
								new Data(this, entry.getKey(), entry.getValue(), repository.userOps.readFullName(entry.getKey().author())))
						.toList()
		);
	}

    /**
     * Read selected book.
     *
     * @throws IOException the io exception
     */
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
			case BorrowBooksControl.ReadResult.Success(String path) -> displayPdfFile(path, title, author, selectedBook);
			case BorrowBooksControl.ReadResult.NewPdfGenerated(String path) -> {
				displayPdfFile(path, title, author, selectedBook);
				Alerts.showInfoDialog("PDF file not found, generating a new one...");
			}
			case HasMessage ret -> Alerts.showErrorDialog(ret.getLocalizedMessage());
		}
	}

	private void displayPdfFile(String path, String title, String author, Book book) throws IOException {
		Main.getContext().newWindow(
				BookViewController.WINDOW_TITLE.formatted(title, author),
				stage -> FXMLs.STUDENT_STAFF_BOOK_VIEW.<Parent>load(loader -> loader.setControllerFactory(_ -> new BookViewController(stage, path, book))),
				null
		).show();
	}

    /**
     * The enum Keys.
     */
    public enum Keys {
        /**
         * Title keys.
         */
        TITLE,
        /**
         * Author full name keys.
         */
        AUTHOR_FULL_NAME,
        /**
         * Borrow date keys.
         */
        BORROW_DATE,
        /**
         * Duration left keys.
         */
        DURATION_LEFT,
        /**
         * Actions keys.
         */
        ACTIONS
	}

    /**
     * The type Data.
     */
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
				case BORROW_DATE ->
						new DynamicTableController.Data.Text(TimeUtil.toStringZonedLocal(borrow.borrowDate()), borrow.borrowDate());
				case DURATION_LEFT -> {
					final var borrow = this.borrow; // Do not reference `this` in lambda.
					final var prop = new SimpleStringProperty(TimeUtil.toStringDuration(borrow.durationLeft()));
					final var weakThis = new WeakReference<>(this);
					Main.getContext().addSecondTimelineListener(this, _ -> {
						prop.setValue(TimeUtil.toStringDuration(borrow.durationLeft()));
						if (borrow.expired()) {
							final var this2 = weakThis.get();
							if (this2 == null) return;
							this2.controller.tableController.removeDatum(this2);
						}
					});
					yield new DynamicTableController.Data.ObservableText(prop, borrow::durationLeft);
				}
				case ACTIONS -> DynamicTableController.Data.Graphic.ofButtons(
						new Tuple2<>(new ReadOnlyStringWrapper("Return"), (_, _) -> {
							try {
								switch (Main.getContext().getBorrowBooksControl().returnBook(controller.getLoggedInUser()._1(), book)) {
									case BorrowBooksControl.ReturnResult.Success _ -> {
										Alerts.showInfoDialog("Book returned successfully");
										// Only after the dialog finishes, return the book
										controller.tableController.removeDatum(this);
									}
									case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
								}
							} catch (TransactionException e) {
								Alerts.showErrorDialog(e.getLocalizedMessage());
							}
						})
				);
			};
		}
	}
}
