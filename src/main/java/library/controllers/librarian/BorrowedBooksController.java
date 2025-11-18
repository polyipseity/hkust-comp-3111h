package library.controllers.librarian;

import javafx.scene.control.TableColumn;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class BorrowedBooksController extends DynamicTableController<BorrowedBooksController.Keys, BorrowedBooksController.Data> implements RequiresLoggedIn {

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull Map<Keys, TableColumn<@NotNull Data, @NotNull Data>> getKeys() {
		return Map.of(
				Keys.TITLE, new TableColumn<>("Title"),
				Keys.BORROWER, new TableColumn<>("Borrower"),
				Keys.BORROW_DATE, new TableColumn<>("Borrowed On"),
				Keys.DURATION_LEFT, new TableColumn<>("Time Left")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext()
				.getRepository()
				.borrowOps
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
		BORROWER,
		BORROW_DATE,
		DURATION_LEFT
	}

	public record Data(@NotNull User user,
	                   @NotNull Book book,
	                   @NotNull Borrow borrow)
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
				case TITLE -> new DynamicTableController.Data.Value(book.title());
				case BORROWER -> new DynamicTableController.Data.Value(user.username());
				case BORROW_DATE -> new DynamicTableController.Data.Value(
						TimeUtil.toStringZonedLocal(borrow.borrowDate()));
				case DURATION_LEFT -> new DynamicTableController.Data.Value(
						TimeUtil.toStringDuration(borrow.durationLeft()));
			};
		}
	}
}
