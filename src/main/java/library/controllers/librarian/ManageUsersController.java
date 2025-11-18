package library.controllers.librarian;

import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.RequiresLoggedIn;
import library.models.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class ManageUsersController extends DynamicTableController<ManageUsersController.Data> implements RequiresLoggedIn {
	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		super.initialize(location, resources);
	}

	@Override
	protected @NotNull Map<@NotNull String, @NotNull Header> getKeys() {
		return Map.of(
				"username", new DynamicTableController.Header("Username"),
				"role", new DynamicTableController.Header("Role"),
				"name", new DynamicTableController.Header("Name"),
				"active", new DynamicTableController.Header("Active"),
				"actions", new DynamicTableController.Header("Actions")
		);
	}

	@Override
	protected @NotNull Collection<@NotNull Data> getData() {
		return Main.getContext().getRepository().userOps.read().entrySet().stream().map(entry -> new Data(entry.getKey(), entry.getValue())).toList();
	}

	public record Data(@NotNull User user,
	                   @NotNull User.Data userData) implements Function<@NotNull String, DynamicTableController.@NotNull Data> {

		/**
		 * Applies this function to the given argument.
		 *
		 * @param s the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.@NotNull Data apply(@NotNull String s) {
			return switch (s) {
				case "username" -> new DynamicTableController.Data.Value(user.username());
				case "role" -> new DynamicTableController.Data.Value(userData.role().name);
				case "name" -> new DynamicTableController.Data.Value(userData.fullName());
				case "active" -> new DynamicTableController.Data.Value(String.valueOf(userData.active()));
				case "actions" -> new DynamicTableController.Data.Value("activate/deactivate");
				default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(s));
			};
		}
	}
}
