package library.controllers.librarian;

import javafx.scene.control.TableColumn;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class BookDownloadController extends DynamicTableController<BookDownloadController.Data> implements RequiresLoggedIn {
	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull Map<@NotNull String, @NotNull TableColumn<Data, Data>> getKeys() {
		return Map.of(
				"title", new TableColumn<>("Title"),
				"author", new TableColumn<>("Author"),
				"bookshelves", new TableColumn<>("Bookshelves")
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
