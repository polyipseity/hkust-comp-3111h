package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
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

	@NotNull
	public String[] readOrThrow(@NotNull User user) {
		return read(user)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s".formatted(user)));
	}

	public void update(@NotNull User user, @NotNull Function<@NotNull String[], @NotNull String[]> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.userNotifications().get(user);
			return oldValue != null && Arrays.equals(oldValue, tx.userNotifications().put(user, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(user));
	}

	public void updateAsList(@NotNull User user, @NotNull Function<@NotNull List<String>, @NotNull List<String>> callback) throws TransactionException {
		update(user, oldValue -> callback.apply(new ArrayList<>(Arrays.asList(oldValue))).toArray(String[]::new));
	}

	public void updateAsList(@NotNull User user, @NotNull Consumer<@NotNull List<String>> callback) throws TransactionException {
		updateAsList(user, list -> {
			callback.accept(list);
			return list;
		});
	}
}
