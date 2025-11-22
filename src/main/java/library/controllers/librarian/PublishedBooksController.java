package library.controllers.librarian;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.FXMLs;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controllers.common.TextViewController;
import library.controls.ManageBooksControl;
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
import java.util.ResourceBundle;
import java.util.function.Function;

public final class PublishedBooksController implements RequiresLoggedIn, Initializable {
	@UnknownNullability
	public TableView<@Nullable Data> table;
	@UnknownNullability
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	public TableColumn<Data, @Nullable Data> titleCol, authorFullNameCol, publishDateCol, timesBorrowedCol, actionsCol;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR_FULL_NAME, authorFullNameCol);
		keys.put(Keys.PUBLISH_DATE, publishDateCol);
		keys.put(Keys.TIMES_BORROWED, timesBorrowedCol);
		keys.put(Keys.ACTIONS, actionsCol);
		tableController = new DynamicTableController<>(table, keys);

		loadTable();
	}

	public void loadTable() {
		final var repository = Main.getContext().getRepository();
		tableController.setData(repository.bookOps
				.read(entry -> entry.getValue().published())
				.entrySet()
				.stream()
				.map(entry ->
						new Data(
								this,
								entry.getKey(),
								entry.getValue(),
								repository.userOps.readFullName(entry.getKey().author())))
				.toList());
	}

	public enum Keys {
		TITLE,
		AUTHOR_FULL_NAME,
		PUBLISH_DATE,
		TIMES_BORROWED,
		ACTIONS
	}

	public record Data(PublishedBooksController controller,
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
								: TimeUtil.toStringZonedLocal(bookData.publishDate()));
				case TIMES_BORROWED -> new DynamicTableController.Data.Text(String.valueOf(bookData.timesBorrowed()));
				case ACTIONS -> DynamicTableController.Data.Graphic.ofButtons(
						new Tuple2<>(new SimpleStringProperty("View"), (_, _) -> {
							try {
								Main.getContext().newWindow(
										TextViewController.WINDOW_TITLE.formatted(book.title(), book.author().id()),
										FXMLs.COMMON_TEXT_VIEW.<Parent>load(loader -> loader.setControllerFactory(_ -> new TextViewController(bookData.content()))),
										null
								).show();
							} catch (IOException e) {
								Alerts.showErrorDialog(e.getLocalizedMessage());
							}
						}),
						new Tuple2<>(new SimpleStringProperty("Delete"), (_, _) -> {
							if (!Alerts.showConfirmDialog("Delete '%s'? All borrowing privileges for this book will be revoked.".formatted(book.title()))) {
								return;
							}
							try {
								switch (Main.getContext().getManageBooksControl().deleteBook(book, controller.getLoggedInUser()._2().role())) {
									case ManageBooksControl.DeleteResult.Success _ -> controller.tableController.removeDatum(this);
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
