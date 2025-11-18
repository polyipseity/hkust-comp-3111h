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

public class BookRequestsController extends DynamicTableController<BookRequestsController.Data> implements RequiresLoggedIn {
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
				"user", new TableColumn<>("Requested By"),
				"requestDate", new TableColumn<>("Request Date"),
				"actions", new TableColumn<>("Actions")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext().getRepository().userBookRequestOps.read().entrySet().stream().map(entry -> new Data(entry.getKey()._1(), entry.getKey()._2(), entry.getValue())).toList();
	}

	public record Data(@NotNull User user, @NotNull BookRequest bookRequest,
	                   @NotNull BookRequest.Data bookRequestData) implements Function<@NotNull String, DynamicTableController.@NotNull Data> {

		/**
		 * Applies this function to the given argument.
		 *
		 * @param s the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull String s) {
			return switch (s) {
				case "title" -> new DynamicTableController.Data.Value(bookRequest.title());
				case "author" -> new DynamicTableController.Data.Value(bookRequest.author());
				case "user" -> new DynamicTableController.Data.Value(user.username());
				case "requestDate" ->
						new DynamicTableController.Data.Value(TimeUtil.toStringZonedLocal(bookRequestData.requestDate()));
				case "actions" -> new DynamicTableController.Data.Value("confirm reject");
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
