package library.controllers.librarian;

import library.controllers.common.DynamicTableController;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BookDownloadController extends DynamicTableController<BookDownloadController.Data> {
	@Override
	protected @NotNull Map<@NotNull String, @NotNull Header> getKeys() {
		return Map.of(
				"title", new DynamicTableController.Header("Title"),
				"author", new DynamicTableController.Header("Author"),
				"bookshelves", new DynamicTableController.Header("Bookshelves")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return List.of();
	}

	public record Data() implements Function<@NotNull String, DynamicTableController.@NotNull Data> {

		/**
		 * Applies this function to the given argument.
		 *
		 * @param s the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull String s) {
			return switch (s) {
				case "title" -> new DynamicTableController.Data.Value("title");
				case "author" -> new DynamicTableController.Data.Value("author");
				case "bookshelves" -> new DynamicTableController.Data.Value("bookshelves");
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
