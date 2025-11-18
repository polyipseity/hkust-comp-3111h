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

public class BookDownloadController extends DynamicTableController<BookDownloadController.Keys, BookDownloadController.Data> implements RequiresLoggedIn {
	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull Map<Keys, TableColumn<@NotNull Data, @NotNull Data>> getKeys() {
		return Map.of(
				Keys.TITLE, new TableColumn<>("Title"),
				Keys.AUTHOR, new TableColumn<>("Author"),
				Keys.BOOKSHELVES, new TableColumn<>("Bookshelves")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return List.of();
	}

	public enum Keys {
		TITLE,
		AUTHOR,
		BOOKSHELVES,
	}

	public record Data() implements Function<@NotNull Keys, DynamicTableController.@NotNull Data> {

		/**
		 * Applies this function to the given argument.
		 *
		 * @param key the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Value("title");
				case AUTHOR -> new DynamicTableController.Data.Value("author");
				case BOOKSHELVES -> new DynamicTableController.Data.Value("bookshelves");
			};
		}
	}
}
