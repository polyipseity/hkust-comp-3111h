package library.controllers.librarian;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controls.PublishBooksControl;
import library.models.json.GutendexResponse;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class BookDownloadController implements RequiresLoggedIn, Initializable {
	public TableView<Data> table;
	public DynamicTableController<Keys, Data> tableController;

	public TextField searchBar;
	public Region sidebar;
	public Text titleText;
	public Text authorText;
	public Text bookshelvesText;
	public Text summaryText;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		keys.put(Keys.TITLE, new TableColumn<>("Title"));
		keys.put(Keys.AUTHOR, new TableColumn<>("Author"));
		keys.put(Keys.BOOKSHELVES, new TableColumn<>("Bookshelves"));
		keys.put(Keys.SUMMARY, new TableColumn<>("Summary"));
		tableController = new DynamicTableController<>(table, keys);

		final var selectedItem = table.getSelectionModel().selectedItemProperty();
		titleText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.title()));
		authorText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.authorString()));
		bookshelvesText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.bookshelvesString()));
		summaryText.textProperty().bind(selectedItem.map(item -> item == null ? "" : item.book.summariesString()));

		loadTable();
	}

	public void loadTable() {
		// noop
	}

	public void searchBooks() {
		final var startTime = System.currentTimeMillis();
		final var searchFuture = Main.getContext().getBookDownloadControl().searchProjectGutenberg(searchBar.getText()).thenApply(results -> results.stream().map(Data::new).toList());

		// Show uncloseable dialog during search
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

	public void downloadSelected() {
		final var context = Main.getContext();
		switch (table.getSelectionModel().selectedItemProperty().get()) {
			case null -> Alerts.showErrorDialog("No books selected");
			case Data(final var rawBook) -> // Download the selected book
			{
				final var startTime = System.currentTimeMillis();
				final var downloadFuture = context.getBookDownloadControl().downloadProjectGutenberg(rawBook).thenApply(content -> context.getBookDownloadControl().newBook(rawBook, content));

				// Show uncloseable dialog during download
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
								if (!Alerts.showConfirmDialog("%s\nOverwrite?".formatted(val.getMessage())).map(ButtonType::getButtonData).map(ButtonBar.ButtonData::isDefaultButton).orElse(false)) {
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

	public enum Keys {
		TITLE, AUTHOR, BOOKSHELVES, SUMMARY,
	}

	public record Data(
			@NotNull GutendexResponse.Book book) implements Function<@NotNull Keys, DynamicTableController.@NotNull Data> {
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Text(book.title());
				case AUTHOR -> new DynamicTableController.Data.Text(book.authorString());
				case BOOKSHELVES -> new DynamicTableController.Data.Text(book.bookshelvesString());
				case SUMMARY -> new DynamicTableController.Data.Text(book.summariesString());
			};
		}
	}
}
