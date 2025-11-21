package library.persistence;

import library.models.User;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryUserNotificationOps(Repository repository) {
	public Map<User, String[]> read() {
		return Collections.unmodifiableMap(repository.userNotifications);
	}

	public Map<User, String[]> read(Predicate<? super Map.Entry<User, String[]>> filter) {
		return repository.userNotifications.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public Optional<String[]> read(User user) {
		return Optional.ofNullable(repository.userNotifications.get(user));
	}

	public String[] readOrThrow(User user) {
		return read(user)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s".formatted(user)));
	}

	public void update(User user, Function<String[], String[]> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.userNotifications().get(user);
			return oldValue != null && Arrays.equals(oldValue, tx.userNotifications().put(user, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(user));
	}

	public void updateAsList(User user, Function<List<String>, List<String>> callback) throws TransactionException {
		update(user, oldValue -> callback.apply(new ArrayList<>(Arrays.asList(oldValue))).toArray(String[]::new));
	}

	public void updateAsList(User user, Consumer<List<String>> callback) throws TransactionException {
		updateAsList(user, list -> {
			callback.accept(list);
			return list;
		});
	}
}
