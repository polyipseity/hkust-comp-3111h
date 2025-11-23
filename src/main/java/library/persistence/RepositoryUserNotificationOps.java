package library.persistence;

import library.models.User;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides operations for interacting with user notifications stored in a repository.
 * This class facilitates reading, updating, and managing notifications associated with users.
 */
public record RepositoryUserNotificationOps(Repository repository) {
    /**
     * Reads the user notifications from the repository and returns them as an unmodifiable map.
     * Each entry in the map corresponds to a user and their associated array of notification strings.
     *
     * @return an unmodifiable map where the keys are {@code User} instances and the values are arrays of {@code String}         representing the notifications for each user.
     */
    public Map<User, String[]> read() {
		return Collections.unmodifiableMap(repository.userNotifications);
	}

    /**
     * Reads user notifications from the repository based on the given filter and returns them as an unmodifiable map.
     * Each entry in the map corresponds to a user and their associated array of notification strings.
     *
     * @param filter a predicate to apply to each map entry of user notifications; only entries satisfying the predicate will be included
     * @return an unmodifiable map where the keys are {@code User} instances and the values are arrays of {@code String}         representing the notifications that meet the filter criteria
     */
    public Map<User, String[]> read(Predicate<? super Map.Entry<User, String[]>> filter) {
		return repository.userNotifications.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

    /**
     * Reads the notifications associated with the specified user from the repository.
     * If no notifications are found for the user, an empty {@code Optional} is returned.
     *
     * @param user the user whose notifications are to be read
     * @return an {@code Optional} containing the array of notification strings         associated with the specified user; or an empty {@code Optional} if no         notifications are found
     */
    public Optional<String[]> read(User user) {
		return Optional.ofNullable(repository.userNotifications.get(user));
	}

    /**
     * Reads the notifications for the specified user from the repository. If no notifications are
     * found for the user, a {@code NoSuchElementException} is thrown.
     *
     * @param user the user whose notifications are to be read
     * @return an array of notification strings associated with the specified user
     * @throws NoSuchElementException if no notifications are found for the specified user
     */
    public String[] readOrThrow(User user) {
		return read(user)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s".formatted(user)));
	}

    /**
     * Updates the notification settings for the specified user in the repository.
     * The update is performed as a transactional operation. The specified callback
     * function is applied to the current array of notifications associated with the user
     * to determine the new array of notifications. If the value is updated successfully,
     * the transaction is committed. If either the user is not found or concurrent updates occur,
     * an exception is thrown.
     *
     * @param user     the user whose notifications are to be updated
     * @param callback a function that accepts the current array of notifications for the user                 and returns the new array of notifications to be stored
     * @throws TransactionException if the transaction fails or concurrent modification occurs
     */
    public void update(User user, Function<String[], String[]> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.userNotifications().get(user);
			return oldValue != null && Arrays.equals(oldValue, tx.userNotifications().put(user, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(user));
	}

    /**
     * Updates the notification settings for the given user by applying the provided callback function.
     * The callback function accepts a list of notifications, modifies it as necessary, and returns
     * the updated list. The updated list will replace the current notification @param user the user
     *
     * @param callback the callback
     * @throws TransactionException the transaction exception
     */
    public void updateAsList(User user, Function<List<String>, List<String>> callback) throws TransactionException {
		update(user, oldValue -> callback.apply(new ArrayList<>(Arrays.asList(oldValue))).toArray(String[]::new));
	}

    /**
     * Updates the notification settings for the specified user as a transactional operation.
     * The specified callback is applied to the current list of notifications associated with the user.
     * The transaction is committed if the update is successful, or an exception is thrown in case of failure.
     *
     * @param user     the user whose notification list is to be
     * @param callback the callback
     * @throws TransactionException the transaction exception
     */
    public void updateAsList(User user, Consumer<List<String>> callback) throws TransactionException {
		updateAsList(user, list -> {
			callback.accept(list);
			return list;
		});
	}
}
