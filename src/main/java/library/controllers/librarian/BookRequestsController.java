package library.controllers.librarian;

import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.BookRequest;
import library.models.User;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class BookRequestsController implements RequiresLoggedIn, Initializable {
	public TableView<Data> table;
	public DynamicTableController<Keys, Data> tableController;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		keys.put(Keys.TITLE, new TableColumn<>("Title"));
		keys.put(Keys.AUTHOR, new TableColumn<>("Author"));
		keys.put(Keys.USER, new TableColumn<>("Requested By"));
		keys.put(Keys.REQUEST_DATE, new TableColumn<>("Request Date"));
		keys.put(Keys.ACTIONS, new TableColumn<>("Actions"));
		tableController = new DynamicTableController<>(table, keys);

		loadTable();
	}

	public void loadTable() {
		tableController.setData(Main.getContext()
				.getRepository()
				.userBookRequestOps
				.read()
				.entrySet()
				.stream()
				.map(entry -> new Data(
						entry.getKey()._1(),
						entry.getKey()._2(),
						entry.getValue()))
				.toList());
	}

	public enum Keys {
		TITLE,
		AUTHOR,
		USER,
		REQUEST_DATE,
		ACTIONS
	}

	public record Data(@NotNull User user,
	                   @NotNull BookRequest bookRequest,
	                   @NotNull BookRequest.Data bookRequestData)
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
				case TITLE -> new DynamicTableController.Data.Text(bookRequest.title());
				case AUTHOR -> new DynamicTableController.Data.Text(bookRequest.author());
				case USER -> new DynamicTableController.Data.Text(user.username());
				case REQUEST_DATE -> new DynamicTableController.Data.Text(
						TimeUtil.toStringZonedLocal(bookRequestData.requestDate()));
				case ACTIONS -> new DynamicTableController.Data.Text("confirm reject");
			};
		}
	}
}
