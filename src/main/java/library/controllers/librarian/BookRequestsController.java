package library.controllers.librarian;

import library.Main;
import library.controllers.common.DynamicTableController;
import library.models.BookRequest;
import library.models.User;
import library.utils.Dates;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class BookRequestsController extends DynamicTableController<BookRequestsController.Data> {
	@Override
	protected @NotNull Map<@NotNull String, @NotNull Header> getKeys() {
		return Map.of(
				"title", new DynamicTableController.Header("Title"),
				"author", new DynamicTableController.Header("Author"),
				"user", new DynamicTableController.Header("Requested By"),
				"requestDate", new DynamicTableController.Header("Request Date"),
				"actions", new DynamicTableController.Header("Actions")
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
						new DynamicTableController.Data.Value(Dates.zonedLocalToString(bookRequestData.requestDate()));
				case "actions" -> new DynamicTableController.Data.Value("confirm reject");
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
