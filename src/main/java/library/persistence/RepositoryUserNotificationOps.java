package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryUserNotificationOps(Repository repository) {
	@NotNull
	public Map<User, String[]> read() {
		return Collections.unmodifiableMap(repository.userNotifications);
	}

	@NotNull
	public Map<User, String[]> read(@NotNull Predicate<? super Map.@NotNull Entry<User, String[]>> filter) {
		return repository.userNotifications.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@NotNull
	public Optional<String[]> read(@NotNull User user) {
		return Optional.ofNullable(repository.userNotifications.get(user));
	}

	public void update(@NotNull User user, @NotNull Function<@NotNull String[], @NotNull String[]> callback) throws TransactionException {
		repository.transact(tx -> {
			tx.userNotifications().put(user, callback.apply(tx.userNotifications().get(user)));
			return true;
		}, () -> "Updated concurrently: %s".formatted(user));
	}

	public void updateAsList(@NotNull User user, @NotNull Function<@NotNull List<String>, @NotNull List<String>> callback) throws TransactionException {
		update(user, oldValue -> callback.apply(new ArrayList<>(Arrays.asList(oldValue))).toArray(String[]::new));
	}
}
