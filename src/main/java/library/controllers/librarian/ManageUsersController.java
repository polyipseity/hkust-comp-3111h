package library.controllers.librarian;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.controls.ManageUsersControl;
import library.models.User;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.Tuple2;
import lombok.With;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class ManageUsersController implements RequiresLoggedIn, Initializable {
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
	@UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> usernameCol, roleCol, nameCol, activeCol, actionsCol;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.USERNAME, usernameCol);
		keys.put(Keys.ROLE, roleCol);
		keys.put(Keys.NAME, nameCol);
		keys.put(Keys.ACTIVE, activeCol);
		keys.put(Keys.ACTIONS, actionsCol);
		tableController = new DynamicTableController<>(table, keys);

		loadTable();
	}

	public void loadTable() {
		tableController.setData(Main.getContext()
				.getRepository()
				.userOps
				.read()
				.entrySet()
				.stream()
				.map(entry -> new Data(
						this,
						entry.getKey(),
						entry.getValue()))
				.toList());
	}

	public enum Keys {
		USERNAME,
		ROLE,
		NAME,
		ACTIVE,
		ACTIONS
	}

	@With
	public record Data(ManageUsersController controller,
	                   User user,
	                   User.Data userData)
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
				case USERNAME -> new DynamicTableController.Data.Text(user.username());
				case ROLE -> new DynamicTableController.Data.Text(userData.role().name);
				case NAME -> new DynamicTableController.Data.Text(userData.fullName());
				case ACTIVE -> new DynamicTableController.Data.Text(String.valueOf(userData.active()));
				case ACTIONS ->
						user.equals(controller.getLoggedInUser()._1()) ? DynamicTableController.Data.Graphic.ofButtons() : DynamicTableController.Data.Graphic.ofButtons(
								userData.active() ? new Tuple2<>(new ReadOnlyStringWrapper("Deactivate"), (_, _) -> {
									try {
										switch (Main.getContext().getManageUsersControl().deactivateUser(user)) {
											case ManageUsersControl.DeactivateResult.Success(final var data) ->
													controller.tableController.replaceDatum(this, withUserData(data));
										}
									} catch (TransactionException e) {
										Alerts.showErrorDialog(e.getLocalizedMessage());
									}
								}) : new Tuple2<>(new ReadOnlyStringWrapper("Activate"), (_, _) -> {
									try {
										switch (Main.getContext().getManageUsersControl().activateUser(user)) {
											case ManageUsersControl.ActivateResult.Success(final var data) ->
													controller.tableController.replaceDatum(this, withUserData(data));
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
