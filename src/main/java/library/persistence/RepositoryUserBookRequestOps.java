package library.persistence;

import library.models.BookRequest;
import library.models.User;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class provides operations to manage the association between users and their book requests
 * in a repository. It includes methods for creating, reading, updating, and deleting user-book
 * request mappings, as well as handling transactional operations with appropriate exception handling.
 */
public record RepositoryUserBookRequestOps(Repository repository) {
    /**
     * Creates a new record for the specified user and book request with the provided data.
     * If a record already exists, a {@link TransactionException} is thrown.
     *
     * @param user        the user associated with the book request
     * @param bookRequest the book request to be created
     * @param data        the data associated with the book request
     * @throws TransactionException if the transaction fails or a record already exists for the provided user and book request
     */
    public void create(User user, BookRequest bookRequest, BookRequest.Data data) throws TransactionException {
		repository.transact(tx -> tx.userBookRequests().put(new Object[]{user, bookRequest}, data) == null, () -> "Already created: %s, %s".formatted(user, bookRequest));
	}

    /**
     * Reads all user-book request data from the repository.
     *
     * @return a map where the key is a tuple of a user and a book request,         and the value is the associated book request data.
     */
    public Map<Tuple2<User, BookRequest>, BookRequest.Data> read() {
		return read(_ -> true);
	}

    /**
     * Reads the user-book request records that satisfy the given filter condition.
     * The method processes the data from the repository's user-book requests, applies the given filter,
     * and transforms it into an unmodifiable map where each entry key is a tuple containing a user
     * and a book request, and the value is the associated data.
     *
     * @param filter a predicate to filter the map entries based on specific conditions
     * @return an unmodifiable map containing filtered entries with each key as a tuple of user and         book request, and each value as the corresponding data
     */
    public Map<Tuple2<User, BookRequest>, BookRequest.Data> read(Predicate<? super Map.Entry<Tuple2<User, BookRequest>, BookRequest.Data>> filter) {
		return repository.userBookRequests.entrySet().stream().map(entry -> Map.entry(new Tuple2<>((User) entry.getKey()[0], (BookRequest) entry.getKey()[1]), entry.getValue())).filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

    /**
     * Reads and retrieves the data associated with a specific user's book request.
     * If no matching data exists, an empty {@code Optional} is returned.
     *
     * @param user        the user associated with the book request
     * @param bookRequest the book request to be read
     * @return an {@code Optional} containing the data of the specified book request if found,         or an empty {@code Optional} if no matching data exists
     */
    public Optional<BookRequest.Data> read(User user, BookRequest bookRequest) {
		return Optional.ofNullable(repository.userBookRequests.get(new Object[]{user, bookRequest}));
	}

    /**
     * Reads the book request data for the specified user and book request.
     * If no data is found, a {@link NoSuchElementException} is thrown.
     *
     * @param user        the user for whom the book request data is being read
     * @param bookRequest the book request whose data is being read
     * @return the book request data associated with the specified user and book request
     * @throws NoSuchElementException if no data is found for the specified user and book request
     */
    public BookRequest.Data readOrThrow(User user, BookRequest bookRequest) {
		return read(user, bookRequest)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s, %s".formatted(user, bookRequest)));
	}

    /**
     * Reads and retrieves a map of book requests and their associated data for a specific user.
     *
     * @param user the user for whom the book requests and data are being read
     * @return an unmodifiable map where the keys are book requests and the values are the associated data
     */
    public Map<BookRequest, BookRequest.Data> read(User user) {
		return repository.userBookRequests.prefixSubMap(new Object[]{user}).entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> (BookRequest) entry.getKey()[1], Map.Entry::getValue));
	}

    /**
     * Updates the data associated with a specific user's book request using the provided callback function.
     * The update is performed within a transactional context, and concurrent updates are detected.
     * If the old value is null or if the update occurs concurrently, the transaction will not succeed.
     *
     * @param user        the user associated with the book request to be updated
     * @param bookRequest the book request whose data is to be updated
     * @param callback    a function that takes the current book request data and returns the new data to be stored
     * @throws TransactionException if the transaction fails or the record is updated concurrently
     */
    public void update(User user, BookRequest bookRequest, Function<BookRequest.Data, BookRequest.Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, bookRequest};
			final var oldValue = tx.userBookRequests().get(key);
			return oldValue != null && oldValue.equals(tx.userBookRequests().put(key, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s, %s".formatted(user, bookRequest));
	}

    /**
     * Updates the data associated with a specific user's book request in a transactional context.
     * If the provided expected data does not match the current data or if the update occurs concurrently,
     * the transaction will fail, and a {@link TransactionException} is thrown.
     *
     * @param user        the user associated with the book request to be updated
     * @param bookRequest the book request whose data is to be updated
     * @param data        the new data to be associated with the specified book request
     * @param expected    the expected current data of the book request, or null if no specific expectation is provided
     * @throws TransactionException if the transaction fails or the update occurs concurrently
     */
    public void update(User user,
	                   BookRequest bookRequest,
	                   BookRequest.Data data,
	                   BookRequest.@Nullable Data expected) throws TransactionException {
		repository.transact(
				tx -> {
					final var key = new Object[]{user, bookRequest};
					return expected == null
							? tx.userBookRequests().put(key, data) != null
							: expected.equals(tx.userBookRequests().put(key, data));
				},
				() -> "Not found or updated concurrently: %s, %s".formatted(user, bookRequest)
		);
	}

    /**
     * Deletes a user-book request record from the repository. If an expected data is provided,
     * the deletion will only occur if the current data matches the expected value.
     *
     * @param user        the user associated with the book request
     * @param bookRequest the book request to be deleted
     * @param expected    the expected current data of the book request, or null if no specific expectation is provided
     * @throws TransactionException if the transaction fails or the record does not match the expected value
     */
    public void delete(User user,
	                   BookRequest bookRequest,
	                   @Nullable BookRequest.Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.userBookRequests().remove(new Object[]{user, bookRequest}) != null : tx.userBookRequests().remove(new Object[]{user, bookRequest}, expected),
				() -> "Already deleted: %s, %s".formatted(user, bookRequest)
		);
	}

    /**
     * Deletes a user-book request record from the repository.
     * The deletion operates on the specified user and book request.
     *
     * @param user        the user associated with the book request to be deleted
     * @param bookRequest the book request to be deleted
     * @throws TransactionException if the transaction fails
     */
    void delete(User user, BookRequest bookRequest) throws TransactionException {
		delete(user, bookRequest, null);
	}

    /**
     * Deletes all book requests associated with the specified user from the repository.
     * This operation is performed within a transactional context.
     * If the user does not exist in the repository, the transaction fails with a specific message.
     *
     * @param user the user whose book requests are to be deleted
     * @throws TransactionException if the transaction fails or the user is not found
     */
    public void delete(User user) throws TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.userBookRequests().prefixSubMap(new Object[]{user}).clear();
			return true;
		}, () -> "User not found: %s".formatted(user));
	}
}
