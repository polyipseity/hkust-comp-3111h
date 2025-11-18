package library.controllers.librarian;

import javafx.scene.control.TableColumn;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.BookRequest;
import library.models.User;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class BookRequestsController extends DynamicTableController<BookRequestsController.Keys, BookRequestsController.Data> implements RequiresLoggedIn {

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
				Keys.USER, new TableColumn<>("Requested By"),
				Keys.REQUEST_DATE, new TableColumn<>("Request Date"),
				Keys.ACTIONS, new TableColumn<>("Actions")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext()
				.getRepository()
				.userBookRequestOps
				.read()
				.entrySet()
				.stream()
				.map(entry -> new Data(
						entry.getKey()._1(),
						entry.getKey()._2(),
						entry.getValue()))
				.toList();
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
				case TITLE -> new DynamicTableController.Data.Value(bookRequest.title());
				case AUTHOR -> new DynamicTableController.Data.Value(bookRequest.author());
				case USER -> new DynamicTableController.Data.Value(user.username());
				case REQUEST_DATE -> new DynamicTableController.Data.Value(
						TimeUtil.toStringZonedLocal(bookRequestData.requestDate()));
				case ACTIONS -> new DynamicTableController.Data.Value("confirm reject");
			};
		}
	}
}
