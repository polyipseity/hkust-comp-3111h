package library.controllers.librarian;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.FXMLs;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.LoadsData;
import library.controllers.common.RequiresLoggedIn;
import library.controllers.common.TextViewController;
import library.controls.ManageBooksControl;
import library.models.Book;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class PendingApprovalsController implements RequiresLoggedIn, Initializable, LoadsData {
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
	@UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, authorCol, summaryCol, actionsCol;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR, authorCol);
		keys.put(Keys.SUMMARY, summaryCol);
		keys.put(Keys.ACTIONS, actionsCol);
		tableController = new DynamicTableController<>(table, keys);
		table.getSortOrder().add(titleCol);

		LoadsData.super.initialize(location, resources);
	}

	@Override
	public void loadData() {
		tableController.setData(Main.getContext()
				.getRepository()
				.bookOps
				.read(entry -> entry.getValue().approvalStatus() == Book.ApprovalStatus.PENDING)
				.entrySet()
				.stream()
				.map(entry -> new Data(
						this,
						entry.getKey(),
						entry.getValue()))
				.toList());
	}

	public enum Keys {
		TITLE,
		AUTHOR,
		SUMMARY,
		ACTIONS
	}

	public record Data(PendingApprovalsController controller,
	                   Book book,
	                   Book.Data bookData)
			implements Function<Keys, DynamicTableController.Data> {
		/**
		 * Applies this function to the given argument.
		 *
		 * @param key the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.Data apply(Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Text(book.title());
				case AUTHOR -> new DynamicTableController.Data.Text(book.author().id());
				case SUMMARY -> new DynamicTableController.Data.Text(bookData.summary());
				case ACTIONS -> DynamicTableController.Data.Graphic.ofButtons(
						new Tuple2<>(new ReadOnlyStringWrapper("View"), (_, _) -> {
							try {
								Main.getContext().newWindow(
										TextViewController.WINDOW_TITLE.formatted(book.title(), book.author().id()),
										FXMLs.COMMON_TEXT_VIEW.<Parent>load(loader -> loader.setControllerFactory(_ -> new TextViewController(bookData.content()))),
										null
								).show();
							} catch (IOException e) {
								Alerts.showErrorDialog(e.getLocalizedMessage());
							}
						}),
						new Tuple2<>(new ReadOnlyStringWrapper("Approve"), (_, _) -> {
							try {
								switch (Main.getContext().getManageBooksControl().approveBook(book)) {
									case ManageBooksControl.ApproveResult.Success _ -> controller.tableController.removeDatum(this);
								}
							} catch (TransactionException e) {
								Alerts.showErrorDialog(e.getLocalizedMessage());
							}
						}),
						new Tuple2<>(new ReadOnlyStringWrapper("Reject"), (_, _) -> {
							try {
								switch (Main.getContext().getManageBooksControl().rejectBook(book)) {
									case ManageBooksControl.RejectResult.Success _ -> controller.tableController.removeDatum(this);
								}
							} catch (TransactionException e) {
								Alerts.showErrorDialog(e.getLocalizedMessage());
							}
						})
				);
			};
		}
	}
}
