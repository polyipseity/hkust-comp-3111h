package library.controllers.librarian;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.LoadsData;
import library.controllers.common.RequiresLoggedIn;
import library.controls.RequestBooksControl;
import library.models.BookRequest;
import library.models.User;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.TimeUtil;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * The {@code BookRequestsController} class is responsible for managing and displaying
 * user book requests in a tabular format. It implements {@code RequiresLoggedIn},
 * {@code Initializable}, and {@code LoadsData}, ensuring user session validation,
 * proper initialization, and data loading capabilities.
 *
 * The controller binds table columns to data keys via a {@code DynamicTableController},
 * dynamically populating and updating the tabular view based on the current context's
 * book request data.
 *
 * This class also enables sorting by default on the request date column in descending order.
 */
public final class BookRequestsController implements RequiresLoggedIn, Initializable, LoadsData {
    /**
     * The Table.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
    /**
     * The Table controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;
    /**
     * The Title col.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, /**
     * The Author col.
     */
    authorCol, /**
     * The User col.
     */
    userCol, /**
     * The Request date col.
     */
    requestDateCol, /**
     * The Actions col.
     */
    actionsCol;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		// This is a placeholder - the original suggestion was invalid and should not be used
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.AUTHOR, authorCol);
		keys.put(Keys.USER, userCol);
		keys.put(Keys.REQUEST_DATE, requestDateCol);
		keys.put(Keys.ACTIONS, actionsCol);
		tableController = new DynamicTableController<>(table, keys);
		requestDateCol.setSortType(TableColumn.SortType.DESCENDING);
		table.getSortOrder().add(requestDateCol);

		LoadsData.super.initialize(location, resources);
	}

	@Override
	public void loadData() {
		tableController.setData(Main.getContext()
				.getRepository()
				.userBookRequestOps
				.read()
				.entrySet()
				.stream()
				.map(entry -> new Data(
						this,
						entry.getKey()._1(),
						entry.getKey()._2(),
						entry.getValue()))
				.toList());
	}

    /**
     * The enum Keys.
     */
    public enum Keys {
        /**
         * Title keys.
         */
        TITLE,
        /**
         * Author keys.
         */
        AUTHOR,
        /**
         * User keys.
         */
        USER,
        /**
         * Request date keys.
         */
        REQUEST_DATE,
        /**
         * Actions keys.
         */
        ACTIONS
	}

    /**
     * The type Data.
     */
    public record Data(BookRequestsController controller,
	                   User user,
	                   BookRequest bookRequest,
	                   BookRequest.Data bookRequestData)
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
				case TITLE -> new DynamicTableController.Data.Text(bookRequest.title());
				case AUTHOR -> new DynamicTableController.Data.Text(bookRequest.author());
				case USER -> new DynamicTableController.Data.Text(user.username());
				case REQUEST_DATE -> new DynamicTableController.Data.Text(
						TimeUtil.toStringZonedLocal(bookRequestData.requestDate()), bookRequestData.requestDate());
				case ACTIONS -> DynamicTableController.Data.Graphic.ofButtons(
						new Tuple2<>(new ReadOnlyStringWrapper("Confirm"), (_, _) -> {
							try {
								switch (Main.getContext().getRequestBooksControl().approveRequest(user, bookRequest)) {
									case RequestBooksControl.ApproveResult.Success _ -> controller.tableController.removeDatum(this);
								}
							} catch (TransactionException e) {
								Alerts.showErrorDialog(e.getLocalizedMessage());
							}
						}),
						new Tuple2<>(new ReadOnlyStringWrapper("Reject"), (_, _) -> {
							try {
								switch (Main.getContext().getRequestBooksControl().rejectRequest(user, bookRequest)) {
									case RequestBooksControl.RejectResult.Success _ -> controller.tableController.removeDatum(this);
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
