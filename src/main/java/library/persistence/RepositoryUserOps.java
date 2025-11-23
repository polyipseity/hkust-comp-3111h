package library.persistence;

import library.models.Author;
import library.models.User;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The RepositoryUserOps class provides an abstraction layer for performing CRUD operations
 * on user-related data within a specified Repository instance. It facilitates creating,
 * reading, updating, and deleting user entries, along with the ability to handle transactional
 * operations and conditions.
 */
public record RepositoryUserOps(Repository repository) {
    /**
     * Creates a new user with the specified data within the repository.
     * If the user already exists, a transaction exception is thrown.
     *
     * @param user the user to create
     * @param data the data associated with the user to be created
     * @throws TransactionException if the user already exists or if an error occurs during the transaction
     */
    public void create(User user, User.Data data) throws TransactionException {
		repository.transact(tx -> tx.users().put(user, data) == null, () -> "Already created: %s".formatted(user));
	}

    /**
     * Retrieves an unmodifiable view of all users and their associated data
     * stored in the repository.
     *
     * @return a map containing all users as keys and their corresponding data as values,         wrapped in an unmodifiable view. Modifications to the returned map         are not allowed.
     */
    public Map<User, User.Data> read() {
		return Collections.unmodifiableMap(repository.users);
	}

    /**
     * Retrieves a filtered, unmodifiable view of users and their associated data
     * from the repository based on the provided filter condition.
     *
     * @param filter the condition used to filter the entries in the repository. Only               entries that satisfy the filter will be included in the result.
     * @return a map of users and their corresponding data that match the specified         filter, wrapped in an unmodifiable view. Modifications to the returned         map are not allowed.
     */
    public Map<User, User.Data> read(Predicate<? super Map.Entry<User, User.Data>> filter) {
		return repository.users.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

    /**
     * Retrieves the data associated with the specified user from the repository.
     *
     * @param user the user whose data is to be retrieved
     * @return an {@code Optional} containing the user's data if present in the repository,         otherwise an empty {@code Optional}
     */
    public Optional<User.Data> read(User user) {
		return Optional.ofNullable(repository.users.get(user));
	}

    /**
     * Reads the data associated with the given user. If no data is found,
     * a {@link NoSuchElementException} is thrown.
     *
     * @param user the user whose data is to be read
     * @return the data associated with the specified user
     * @throws NoSuchElementException if the user has no associated data
     */
    public User.Data readOrThrow(User user) {
		return read(user)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s".formatted(user)));
	}

    /**
     * Reads and retrieves the full name of the given author.
     *
     * @param author the author whose full name is to be read. The author can either               be identified by name or by reference.
     * @return the full name of the author if available. If the author is identified         by reference and their data cannot be resolved, an error message is returned.
     */
    public String readFullName(Author author) {
		return switch (author) {
			case Author.ByName(final var val) -> val;
			case Author.ByRef(final var val) ->
					read(val).map(User.Data::fullName).orElseGet(() -> "ERROR: %s".formatted(val.username()));
		};
	}

    /**
     * Updates the specified user in the repository using the provided callback function.
     * The callback is applied to the user's current data to compute the new data.
     * If the user does not exist or the update is unsuccessful due to concurrent modification,
     * a {@code TransactionException} is thrown.
     *
     * @param user     the user to be updated
     * @param callback a function that receives the current data of the user and returns the updated data
     * @throws TransactionException if the user does not exist or the update fails due to a transaction error
     */
    public void update(User user, Function<User.Data, User.Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.users().get(user);
			return oldValue != null && oldValue.equals(tx.users().put(user, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(user));
	}

    /**
     * Updates the specified user in the repository with the provided data.
     * If an expected data object is provided, the update will only proceed
     * if the current data associated with the user matches the expected data.
     * A {@code TransactionException} is thrown if the user does not exist or
     * if the update fails due to a transaction error.
     *
     * @param user     the user to be updated
     * @param data     the new data to associate with the user
     * @param expected the expected data currently associated with the user;                 can be {@code null} to indicate no specific expectation
     * @throws TransactionException if the update fails due to a transaction error
     */
    public void update(User user,
	                   User.Data data,
	                   User.@Nullable Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null
						? tx.users().put(user, data) != null
						: expected.equals(tx.users().put(user, data)),
				() -> "Not found or updated concurrently: %s".formatted(user)
		);
	}

    /**
     * Deletes the specified user from the repository.
     * If an expected data object is provided, the deletion will only proceed
     * if the current data associated with the user matches the expected data.
     *
     * @param user     the user to be deleted
     * @param expected the expected data currently associated with the user;                 can be {@code null} to indicate no specific expectation
     * @throws TransactionException if the deletion fails due to a transaction error
     */
    public void delete(User user,
	                   @Nullable User.Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.users().remove(user) != null : tx.users().remove(user, expected),
				() -> "Already deleted: %s".formatted(user)
		);
	}

    /**
     * Deletes the specified user from the repository.
     *
     * @param user the user to be deleted
     * @throws TransactionException if an error occurs during the deletion
     */
    void delete(User user) throws TransactionException {
		delete(user, null);
	}
}
