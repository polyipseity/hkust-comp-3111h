package library.controllers.librarian;

import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.Author;
import library.models.Book;
import library.models.User;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

public class PublishedBooksController implements RequiresLoggedIn, Initializable {
	public TableView<Data> table;
	public DynamicTableController<Keys, Data> tableController;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		keys.put(Keys.TITLE, new TableColumn<>("Title"));
		keys.put(Keys.AUTHOR_FULL_NAME, new TableColumn<>("Author"));
		keys.put(Keys.PUBLISH_DATE, new TableColumn<>("Published On"));
		keys.put(Keys.TIMES_BORROWED, new TableColumn<>("Times Borrowed"));
		keys.put(Keys.ACTIONS, new TableColumn<>("Actions"));
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
								entry.getKey(),
								entry.getValue(),
								switch (entry.getKey().author()) {
									case Author.ByName(final var val) -> val;
									case Author.ByRef(final var val) -> repository.userOps
											.read(val)
											.map(User.Data::fullName)
											.orElseGet(() -> "ERROR: %s".formatted(val.username()));
								}))
				.toList());
	}

	public enum Keys {
		TITLE,
		AUTHOR_FULL_NAME,
		PUBLISH_DATE,
		TIMES_BORROWED,
		ACTIONS
	}

	public record Data(@NotNull Book book,
	                   @NotNull Book.Data bookData,
	                   @NotNull String authorFullName)
			implements Function<@NotNull Keys, DynamicTableController.@NotNull Data> {
		/**
		 * Applies this function to the given argument.
		 *
		 * @param key the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Value(book.title());
				case AUTHOR_FULL_NAME -> new DynamicTableController.Data.Value(authorFullName);
				case PUBLISH_DATE -> new DynamicTableController.Data.Value(
						bookData.publishDate() == null
								? ""
								: TimeUtil.toStringZonedLocal(bookData.publishDate()));
				case TIMES_BORROWED -> new DynamicTableController.Data.Value(String.valueOf(bookData.timesBorrowed()));
				case ACTIONS -> new DynamicTableController.Data.Value("view delete");
			};
		}
	}
}
