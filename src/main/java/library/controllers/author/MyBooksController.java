package library.controllers.author;

import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.FXMLs;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.LoadsData;
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

/**
 * Controller responsible for managing the view and interactions within the "My Books" screen.
 * <p>
 * This controller handles the display, modification, deletion, and viewing of books associated with the logged-in user.
 * It uses a dynamic table to display data and binds buttons to the selection status of the table entries.
 * </p>
 * <p>
 * Implements:
 * <ul>
 *   <li>{@link RequiresLoggedIn}: Ensures that the user is logged in before interacting with the controller.</li>
 *   <li>{@link Initializable}: Allows injection of initialization logic during the associated FXML loading.</li>
 *   <li>{@link LoadsData}: Handles data loading operations for dynamically fetching and refreshing the table data when required.</li>
 * </ul>
 */
public final class MyBooksController implements RequiresLoggedIn, Initializable, LoadsData {

    /**
     * The main table view displaying the list of books owned by the author.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;

    /**
     * Helper controller for managing the dynamic columns and data of the table.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;

    /**
     * Table columns corresponding to book attributes.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, /**
     * The Status col.
     */
    statusCol, /**
     * The Date col.
     */
    dateCol, /**
     * The Readers col.
     */
    readersCol, /**
     * The Summary col.
     */
    summaryCol;

    /**
     * Action buttons for interacting with the selected book.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Button viewButton, /**
     * The Modify button.
     */
    modifyButton, /**
     * The Delete button.
     */
    deleteButton;

	/**
	 * Initializes the controller class.
	 * <p>
	 * Sets up the table columns, maps them to their respective keys, configures sorting,
	 * and establishes bindings for button disable properties based on the selection model.
	 * </p>
	 *
	 * @param location  The location used to resolve relative paths for the root object, or null if the location is not known.
	 * @param resources The resources used to localize the root object, or null if the root object was not localized.
	 */
	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.STATUS, statusCol);
		keys.put(Keys.PUBLISH_DATE, dateCol);
		keys.put(Keys.READERS, readersCol);
		keys.put(Keys.SUMMARY, summaryCol);
		tableController = new DynamicTableController<>(table, keys);
		dateCol.setSortType(TableColumn.SortType.DESCENDING);
		table.getSortOrder().add(dateCol);

		LoadsData.super.initialize(location, resources);

		final var selectedItem = table.getSelectionModel().selectedItemProperty();
		viewButton.disableProperty().bind(selectedItem.isNull());
		
		// Disable modify and delete if no item is selected OR if the selected book has active borrows
		modifyButton.disableProperty().bind(selectedItem.map(item -> item == null || !item.borrows.isEmpty()).orElse(true));
		deleteButton.disableProperty().bind(selectedItem.map(item -> item == null || !item.borrows.isEmpty()).orElse(true));
	}

	/**
	 * Loads and refreshes the data displayed in the table.
	 * <p>
	 * Fetches books from the repository that match the currently logged-in author and are active.
	 * Also retrieves associated borrow records to populate the table data model.
	 * </p>
	 */
	@Override
	public void loadData() {
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

    /**
     * Opens a new window to view the content of the selected book.
     *
     * @throws IOException If the FXML file for the text view cannot be loaded.
     */
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

    /**
     * Opens a modal window to modify the selected book's details.
     * <p>
     * This action is only permitted if the book has no active borrows.
     * Upon closing the modify window, the table data is refreshed.
     * </p>
     *
     * @throws IOException If the FXML file for the modify window cannot be loaded.
     */
    public void modifyBook() throws IOException {
		final var selected = table.getSelectionModel().getSelectedItem();
		if (selected == null || !selected.borrows.isEmpty()) {
			// Button should be disabled
			return;
		}
		var newWindow = Main.getContext().newWindow(
				"Modify Book",
				// Load the FXML file for the new window's content
				stage -> FXMLs.AUTHOR_MODIFY_WINDOW.<Parent>load(loader ->
						loader.setControllerFactory(_ -> new ModifyWindowController(stage, selected.book, selected.bookData, this::loadData))),
				null
		);
        newWindow.setOnHidden(windowEvent -> loadData());
        newWindow.setHeight(250);
        newWindow.setWidth(480);
        newWindow.setResizable(false);
        newWindow.show();
	}

    /**
     * Deletes the selected book after user confirmation.
     * <p>
     * This action is only permitted if the book has no active borrows.
     * If the deletion is successful, the table is reloaded.
     * Errors are displayed via an alert dialog.
     * </p>
     */
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
				case ManageBooksControl.DeleteResult.Success _ -> loadData();
				case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}

    /**
     * Enumeration representing the columns (keys) available in the My Books table.
     */
    public enum Keys {
        /**
         * Title of the book.
         */
        TITLE,
        /**
         * Current approval or visibility status of the book.
         */
        STATUS,
        /**
         * Date when the book was published.
         */
        PUBLISH_DATE,
        /**
         * Number of current active readers/borrows.
         */
        READERS,
        /**
         * Short summary of the book.
         */
        SUMMARY
	}

    /**
     * Data record representing a single row in the table.
     * <p>
     * Acts as a container for the Book entity, its data, and borrow statistics.
     * Implements {@link Function} to map {@link Keys} to displayable {@link DynamicTableController.Data}.
     * </p>
     *
     * @param controller The parent controller instance.
     * @param book       The book entity identifier.
     * @param bookData   The detailed data of the book.
     * @param borrows    A map of users to their borrow records for this book.
     */
    public record Data(MyBooksController controller,
	                   Book book,
	                   Book.Data bookData,
	                   Map<User, Borrow> borrows)
			implements Function<Keys, DynamicTableController.Data> {
		/**
         * Maps a {@link Keys} enumeration value to a corresponding {@link DynamicTableController.Data} instance.
         * Provides data in a structured format suitable for display in a table.
         *
         * @param key The specific {@link Keys} value that identifies the type of data required.
         * @return A corresponding {@link DynamicTableController.Data} representation of the requested data.
         */
		@Override
		public DynamicTableController.Data apply(Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Text(book.title());
				case STATUS -> new DynamicTableController.Data.Text(bookData.approvalStatus().name);
				case PUBLISH_DATE -> new DynamicTableController.Data.Text(
						bookData.publishDate() == null
								? ""
								: TimeUtil.toStringZonedLocal(bookData.publishDate()), bookData.publishDate());
				case READERS -> new DynamicTableController.Data.Text(String.valueOf(borrows.size()), borrows.size());
				case SUMMARY -> new DynamicTableController.Data.Text(bookData.summary());
			};
		}
	}
}
