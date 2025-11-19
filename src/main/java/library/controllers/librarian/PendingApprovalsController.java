package library.controllers.librarian;

import javafx.scene.control.TableColumn;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controls.ManageBooksControl;
import library.models.Book;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;
import java.util.function.Function;

public class PendingApprovalsController extends DynamicTableController<PendingApprovalsController.Keys, PendingApprovalsController.Data> implements RequiresLoggedIn {
	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull SequencedMap<Keys, TableColumn<Data, Data>> getKeys() {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		keys.put(Keys.TITLE, new TableColumn<>("Title"));
		keys.put(Keys.AUTHOR, new TableColumn<>("Author"));
		keys.put(Keys.SUMMARY, new TableColumn<>("Summary"));
		keys.put(Keys.ACTIONS, new TableColumn<>("Actions"));
		return keys;
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext()
				.getRepository()
				.bookOps
				.read(entry -> entry.getValue().approvalStatus() == Book.ApprovalStatus.PENDING)
				.entrySet()
				.stream()
				.map(entry -> new Data(
						entry.getKey(),
						entry.getValue()))
				.toList();
	}

	public enum Keys {
		TITLE,
		AUTHOR,
		SUMMARY,
		ACTIONS
	}

	public record Data(@NotNull Book book,
	                   @NotNull Book.Data bookData)
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
				case AUTHOR -> new DynamicTableController.Data.Value(book.author().toString());
				case SUMMARY -> new DynamicTableController.Data.Value(bookData.summary());
				case ACTIONS -> new DynamicTableController.Data.Buttons(List.of(
						new Tuple2<>("View", _ -> {

						}),
						new Tuple2<>("Approve", _ -> {
							try {
								switch (Main.getContext().getManageBooks().approveBook(book)) {
									case ManageBooksControl.ApproveResult.Success _ -> {
									}
								}
							} catch (TransactionException e) {
								Alerts.showErrorDialog(e.getMessage());
							}
						}),
						new Tuple2<>("Reject", _ -> {
							try {
								switch (Main.getContext().getManageBooks().rejectBook(book)) {
									case ManageBooksControl.RejectResult.Success _ -> {

									}
								}
							} catch (TransactionException e) {
								Alerts.showErrorDialog(e.getMessage());
							}
						})
				));
			};
		}
	}
}
