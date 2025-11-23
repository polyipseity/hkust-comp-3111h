package library.controllers.librarian;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controls.PublishBooksControl;
import library.models.json.GutendexResponse;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * The BookDownloadController class is responsible for managing the user interface and operations
 * related to searching, displaying, and downloading books from Project Gutenberg. It relies on
 * functionalities provided by the RequiresLoggedIn interface for enforcing authentication.
 *
 * This class initializes and manages a dynamic table view for displaying book data and provides
 * mechanisms for book search and download operations. User interactions are facilitated through
 * various UI components, such as text fields and table columns.
 */
public final class BookDownloadController implements RequiresLoggedIn, Initializable {
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
     * The Bookshelves col.
     */
    bookshelvesCol, /**
     * The Summary col.
     */
    summaryCol;

    /**
     * The Search bar.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextField searchBar;
    /**
     * The Sidebar.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Region sidebar;
    /**
     * The Title text.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Text titleText, /**
     * The Author text.
     */
    authorText, /**
     * The Bookshelves text.
     */
    bookshelvesText, /**
     * The Summary text.
     */
    summaryText;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR, authorCol);
		keys.put(Keys.BOOKSHELVES, bookshelvesCol);
		keys.put(Keys.SUMMARY, summaryCol);
		tableController = new DynamicTableController<>(table, keys);
		table.getSortOrder().add(titleCol);

		RequiresLoggedIn.super.initialize(location, resources);
		loadData();

		final var selectedItem = table.getSelectionModel().selectedItemProperty();
		titleText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.title()).orElse(""));
		authorText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.authorString()).orElse(""));
		bookshelvesText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.bookshelvesString()).orElse(""));
		summaryText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.summariesString()).orElse(""));
	}

    /**
     * Load data.
     */
    @SuppressWarnings("EmptyMethod")
	public void loadData() {
		// noop
	}

    /**
     * Search books.
     */
    public void searchBooks() {
		final var startTime = System.currentTimeMillis();
		final var searchFuture = Main.getContext().getBookDownloadControl().searchProjectGutenberg(searchBar.getText()).thenApply(results -> results.stream().map(Data::new).toList());

		// Show not closeable dialog during search
		final var running = new SimpleBooleanProperty(true);
		searchFuture.whenComplete((_, _) -> Platform.runLater(() -> running.set(false)));
		if (!Alerts.showLoadingDialog("Searching for books...", running)) {
			searchFuture.cancel(true);
			return;
		}

		// Handle completion of the future
		searchFuture.<Runnable>thenApply(data -> {
			final var endTime = System.currentTimeMillis();
			return () -> {
				tableController.setData(data);
				// Show dialog with number of books found and time taken
				Alerts.showInfoDialog("Found %d books in %d ms".formatted(data.size(), endTime - startTime));
			};
		}).exceptionally(throwable -> {
			final var endTime = System.currentTimeMillis();
			return () -> Alerts.showErrorDialog("Search failed after %d ms: %s".formatted(endTime - startTime, throwable.getLocalizedMessage()));
		}).thenAccept(Platform::runLater);
	}

    /**
     * Download selected.
     */
    public void downloadSelected() {
		final var context = Main.getContext();
		switch (table.getSelectionModel().selectedItemProperty().get()) {
			case null -> Alerts.showErrorDialog("No books selected");
			case Data(final var rawBook) -> // Download the selected book
			{
				final var startTime = System.currentTimeMillis();
				final var downloadFuture = context.getBookDownloadControl().downloadProjectGutenberg(rawBook).thenApply(content -> context.getBookDownloadControl().newBook(rawBook, content));

				// Show not closeable dialog during download
				final var running = new SimpleBooleanProperty(true);
				downloadFuture.whenComplete((_, _) -> Platform.runLater(() -> running.set(false)));
				if (!Alerts.showLoadingDialog("Downloading book...", running)) {
					downloadFuture.cancel(true);
					return;
				}

				downloadFuture.<Runnable>thenApply(results -> () -> {
					final var endTime = System.currentTimeMillis();
					try {
						switch (context.getPublishBooksControl().addBook(results._1(), results._2())) {
							case PublishBooksControl.AddBookResult.AlreadyExists val -> {
								if (!Alerts.showConfirmDialog("%s\nOverwrite?".formatted(val.getLocalizedMessage()))) {
									return;
								}
								switch (context.getPublishBooksControl().addBook(results._1(), results._2(), true)) {
									case PublishBooksControl.AddBookResult.AlreadyExists _ -> throw new AssertionError();
									case PublishBooksControl.AddBookResult.Success _ -> {
									}
								}
							}
							case PublishBooksControl.AddBookResult.Success _ -> {
							}
						}
						Alerts.showInfoDialog("Book downloaded in %d ms".formatted(endTime - startTime));
					} catch (TransactionException e) {
						Alerts.showErrorDialog(e.getLocalizedMessage());
					}
				}).exceptionally(throwable -> {
					final var endTime = System.currentTimeMillis();
					return () -> Alerts.showErrorDialog("Failed to download book in %d ms: %s".formatted(endTime - startTime, throwable.getLocalizedMessage()));
				}).thenAccept(Platform::runLater);
			}
		}
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
         * Author keys.
         */
        AUTHOR,
        /**
         * Bookshelves keys.
         */
        BOOKSHELVES,
        /**
         * Summary keys.
         */
        SUMMARY,
	}

    /**
     * The type Data.
     */
    public record Data(
			GutendexResponse.Book book) implements Function<Keys, DynamicTableController.Data> {
		@Override
		public DynamicTableController.Data apply(Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Text(book.title());
				case AUTHOR -> new DynamicTableController.Data.Text(book.authorString());
				case BOOKSHELVES -> new DynamicTableController.Data.Text(book.bookshelvesString());
				case SUMMARY -> new DynamicTableController.Data.Text(book.summariesString());
			};
		}
	}
}
