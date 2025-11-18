package library.controllers.librarian;

import javafx.scene.control.TableColumn;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.SequencedMap;
import java.util.function.Function;

public class ManageUsersController extends DynamicTableController<ManageUsersController.Keys, ManageUsersController.Data> implements RequiresLoggedIn {

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull SequencedMap<Keys, TableColumn<Data, Data>> getKeys() {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, Data>>();
		keys.put(Keys.USERNAME, new TableColumn<>("Username"));
		keys.put(Keys.ROLE, new TableColumn<>("Role"));
		keys.put(Keys.NAME, new TableColumn<>("Name"));
		keys.put(Keys.ACTIVE, new TableColumn<>("Active"));
		keys.put(Keys.ACTIONS, new TableColumn<>("Actions"));
		return keys;
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext()
				.getRepository()
				.userOps
				.read()
				.entrySet()
				.stream()
				.map(entry -> new Data(
						entry.getKey(),
						entry.getValue()))
				.toList();
	}

	public enum Keys {
		USERNAME,
		ROLE,
		NAME,
		ACTIVE,
		ACTIONS
	}

	public record Data(@NotNull User user,
	                   @NotNull User.Data userData)
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
				case USERNAME -> new DynamicTableController.Data.Value(user.username());
				case ROLE -> new DynamicTableController.Data.Value(userData.role().name);
				case NAME -> new DynamicTableController.Data.Value(userData.fullName());
				case ACTIVE -> new DynamicTableController.Data.Value(String.valueOf(userData.active()));
				case ACTIONS -> new DynamicTableController.Data.Value("activate/deactivate");
			};
		}
	}
}
