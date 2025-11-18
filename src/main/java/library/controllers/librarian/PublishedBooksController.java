package library.controllers.librarian;

import library.Main;
import library.controllers.common.DynamicTableController;
import library.models.Author;
import library.models.Book;
import library.models.User;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class PublishedBooksController extends DynamicTableController<PublishedBooksController.Data> {
	@Override
	protected @NotNull Map<@NotNull String, @NotNull Header> getKeys() {
		return Map.of(
				"title", new Header("Title"),
				"authorFullName", new Header("Author"),
				"publishDate", new Header("Published On"),
				"timesBorrowed", new Header("Times Borrowed"),
				"actions", new Header("Actions")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		final var repository = Main.getContext().getRepository();
		return repository.bookOps.read(entry -> entry.getValue().published()).entrySet().stream().map(entry ->
				new Data(entry.getKey(), entry.getValue(), switch (entry.getKey().author()) {
					case Author.ByName(final var val) -> val;
					case Author.ByRef(final var val) ->
							repository.userOps.read(val).map(User.Data::fullName).orElseGet(() -> "ERROR: %s".formatted(val.username()));
				})).toList();
	}

	public record Data(@NotNull Book book, @NotNull Book.Data bookData,
	                   @NotNull String authorFullName) implements Function<@NotNull String, DynamicTableController.@NotNull Data> {
		/**
		 * Applies this function to the given argument.
		 *
		 * @param s the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull String s) {
			return switch (s) {
				case "title" -> new DynamicTableController.Data.Value(book.title());
				case "authorFullName" -> new DynamicTableController.Data.Value(authorFullName);
				case "publishDate" ->
						new DynamicTableController.Data.Value(bookData.publishDate() == null ? "" : TimeUtil.toStringZonedLocal(bookData.publishDate()));
				case "timesBorrowed" -> new DynamicTableController.Data.Value(String.valueOf(bookData.timesBorrowed()));
				case "actions" -> new DynamicTableController.Data.Value("view delete");
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
