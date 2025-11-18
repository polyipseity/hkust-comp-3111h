package library.controllers.librarian;

import library.Main;
import library.controllers.common.DynamicTableController;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.utils.Dates;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class BorrowedBooksController extends DynamicTableController<BorrowedBooksController.Data> {
	@Override
	protected @NotNull Map<@NotNull String, @NotNull Header> getKeys() {
		return Map.of(
				"title", new DynamicTableController.Header("Title"),
				"borrower", new DynamicTableController.Header("Borrower"),
				"borrowDate", new DynamicTableController.Header("Borrowed On"),
				"durationLeft", new DynamicTableController.Header("Time Left")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext().getRepository().borrowOps.read().entrySet().stream().map(entry -> new Data(entry.getKey()._1(), entry.getKey()._2(), entry.getValue())).toList();
	}

	public record Data(@NotNull User user, @NotNull Book book,
	                   @NotNull Borrow borrow) implements Function<@NotNull String, DynamicTableController.@NotNull Data> {

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
				case "borrower" -> new DynamicTableController.Data.Value(user.username());
				case "borrowDate" -> new DynamicTableController.Data.Value(Dates.zonedLocalToString(borrow.borrowDate()));
				case "durationLeft" -> new DynamicTableController.Data.Value(borrow.durationLeft().toString());
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
