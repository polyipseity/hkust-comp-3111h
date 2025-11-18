package library.controllers.librarian;

import javafx.scene.control.TableColumn;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.Book;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class PendingApprovalsController extends DynamicTableController<String, PendingApprovalsController.Data> implements RequiresLoggedIn {
	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull Map<String, TableColumn<@NotNull Data, @NotNull Data>> getKeys() {
		return Map.of(
				"title", new TableColumn<>("Title"),
				"author", new TableColumn<>("Author"),
				"summary", new TableColumn<>("Summary"),
				"actions", new TableColumn<>("Actions")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext().getRepository().bookOps.read(entry -> entry.getValue().approvalStatus() == Book.ApprovalStatus.PENDING).entrySet().stream().map(entry -> new Data(entry.getKey(), entry.getValue())).toList();
	}

	public record Data(@NotNull Book book,
	                   @NotNull Book.Data bookData) implements Function<@NotNull String, DynamicTableController.@NotNull Data> {

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
				case "author" -> new DynamicTableController.Data.Value(book.author().toString());
				case "summary" -> new DynamicTableController.Data.Value(bookData.summary());
				case "actions" -> new DynamicTableController.Data.Value("view approve reject");
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
