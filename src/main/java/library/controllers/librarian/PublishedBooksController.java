package library.controllers.librarian;

import javafx.scene.control.TableColumn;
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
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class PublishedBooksController extends DynamicTableController<PublishedBooksController.Keys, PublishedBooksController.Data> implements RequiresLoggedIn {

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull Map<Keys, TableColumn<@NotNull Data, @NotNull Data>> getKeys() {
		return Map.of(
				Keys.TITLE, new TableColumn<>("Title"),
				Keys.AUTHOR_FULL_NAME, new TableColumn<>("Author"),
				Keys.PUBLISH_DATE, new TableColumn<>("Published On"),
				Keys.TIMES_BORROWED, new TableColumn<>("Times Borrowed"),
				Keys.ACTIONS, new TableColumn<>("Actions")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		final var repository = Main.getContext().getRepository();
		return repository.bookOps
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
				.toList();
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
