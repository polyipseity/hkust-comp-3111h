package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record RepositoryUserNotificationOps(Repository repository) {
	@NotNull
	public Optional<String[]> read(@NotNull User user) {
		return Optional.ofNullable(repository.userNotifications.get(user));
	}

	public void update(@NotNull User user, @NotNull Function<@NotNull String[], @NotNull String[]> callback) throws TransactionException {
		repository.transact(tx -> {
			tx.userNotifications().put(user, callback.apply(tx.userNotifications().get(user)));
			return true;
		});
	}

	public void updateAsList(@NotNull User user, @NotNull Function<@NotNull List<String>, @NotNull List<String>> callback) throws TransactionException {
		update(user, oldValue -> callback.apply(new ArrayList<>(Arrays.asList(oldValue))).toArray(String[]::new));
	}
}
