package library.controllers.librarian;

import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class BookDownloadController implements RequiresLoggedIn, Initializable {
	public TableView<@NotNull Data> table;
	public DynamicTableController<Keys, Data> tableController;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		keys.put(Keys.TITLE, new TableColumn<>("Title"));
		keys.put(Keys.AUTHOR, new TableColumn<>("Author"));
		keys.put(Keys.BOOKSHELVES, new TableColumn<>("Bookshelves"));
		tableController = new DynamicTableController<>(table, keys);

		loadTable();
	}

	public void loadTable() {
		tableController.setData(List.of());
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
