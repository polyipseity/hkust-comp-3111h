package library.persistence;

import library.models.Book;
import library.models.Borrow;
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
 * This class provides operations for managing borrowing data in a repository.
 * It includes operations to read, create, update, and delete borrowings,
 * as well as pruning expired borrows.
 */
public record RepositoryBorrowOps(Repository repository) {
	// Only care about expired entries if reading is required

    /**
     * Reads and retrieves all entries from the repository that fulfill a default condition.
     *
     * @return a map where each key is a tuple of {@link User} and {@link Book}, and each value is a {@link Borrow} object         representing the borrowing details for the corresponding user and book.
     */
    public Map<Tuple2<User, Book>, Borrow> read() {
		return read(_ -> true);
	}

    /**
     * Reads and retrieves entries from the repository that meet the specified filter condition.
     *
     * @param filter a predicate used to filter the entries by the condition defined by the caller.
     * @return a map where each key is a tuple of {@code User} and {@code Book}, and each value is a {@code Borrow} object         representing the borrowing details for the corresponding user and book.
     */
    public Map<Tuple2<User, Book>, Borrow> read(Predicate<? super Map.Entry<Tuple2<User, Book>, Borrow>> filter) {
		return repository.borrows.entrySet().stream()
				.filter(entry -> !entry.getValue().expired())
				.map(entry -> Map.entry(new Tuple2<>((User) entry.getKey()[0], (Book) entry.getKey()[1]), entry.getValue()))
				.filter(filter)
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

    /**
     * Read optional.
     *
     * @param user the user
     * @param book the book
     * @return the optional
     */
    public Optional<Borrow> read(User user, Book book) {
		return Optional.ofNullable(repository.borrows.get(new Object[]{user, book}))
				.filter(Predicate.not(Borrow::expired));
	}

    /**
     * Read or throw borrow.
     *
     * @param user the user
     * @param book the book
     * @return the borrow
     */
    public Borrow readOrThrow(User user, Book book) {
		return read(user, book)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s, %s".formatted(user, book)));
	}

    /**
     * Read map.
     *
     * @param user the user
     * @return the map
     */
    public Map<Book, Borrow> read(User user) {
		return repository.borrows.prefixSubMap(new Object[]{user}).entrySet().stream()
				.filter(entry -> !entry.getValue().expired())
				.collect(Collectors.toUnmodifiableMap(entry -> (Book) entry.getKey()[1], Map.Entry::getValue));
	}

    /**
     * Read map.
     *
     * @param book the book
     * @return the map
     */
    public Map<User, Borrow> read(Book book) {
		return repository.borrows.entrySet().stream()
				.filter(entry -> !entry.getValue().expired())
				.filter(entry -> book.equals(entry.getKey()[1]))
				.collect(Collectors.toUnmodifiableMap(entry -> (User) entry.getKey()[0], Map.Entry::getValue));
	}

    /**
     * Create.
     *
     * @param user the user
     * @param book the book
     * @param data the data
     * @throws TransactionException the transaction exception
     */
    public void create(User user, Book book, Borrow data) throws TransactionException {
		repository.transact(tx -> {
			final var oldBorrow = tx.borrows().put(new Object[]{user, book}, data);
			return oldBorrow == null || oldBorrow.expired();
		}, () -> "Already created: %s, %s".formatted(user, book));
	}

    /**
     * Update.
     *
     * @param user     the user
     * @param book     the book
     * @param callback the callback
     * @throws TransactionException the transaction exception
     */
    public void update(User user, Book book, Function<Borrow, Borrow> callback) throws TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, book};
			final var oldValue = tx.borrows().get(key);
			return oldValue != null && !oldValue.expired() && oldValue.equals(tx.borrows().put(key, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s,  %s".formatted(user, book));
	}

    /**
     * Update.
     *
     * @param user     the user
     * @param book     the book
     * @param data     the data
     * @param expected the expected
     * @throws TransactionException the transaction exception
     */
    public void update(User user,
	                   Book book,
	                   Borrow data,
	                   @Nullable Borrow expected) throws TransactionException {
		repository.transact(
				tx -> {
					final var key = new Object[]{user, book};
					final var oldBorrow = tx.borrows().put(key, data);
					return expected == null
							? oldBorrow != null && !oldBorrow.expired()
							: expected.equals(oldBorrow) || expected.expired();
				},
				() -> "Not found or updated concurrently: %s,  %s".formatted(user, book)
		);
	}

    /**
     * Delete.
     *
     * @param user     the user
     * @param book     the book
     * @param expected the expected
     * @throws TransactionException the transaction exception
     */
    public void delete(User user,
	                   Book book,
	                   @Nullable Borrow expected) throws TransactionException {
		repository.transact(
				tx -> {
					final var oldBorrow = tx.borrows().remove(new Object[]{user, book});
					return expected == null
							? oldBorrow != null && !oldBorrow.expired()
							: expected.equals(oldBorrow) || expected.expired();
				},
				() -> "Already deleted: %s, %s".formatted(user, book)
		);
	}

    /**
     * Delete.
     *
     * @param user the user
     * @param book the book
     * @throws TransactionException the transaction exception
     */
    void delete(User user, Book book) throws TransactionException {
		delete(user, book, null);
	}

    /**
     * Delete.
     *
     * @param user the user
     * @throws TransactionException the transaction exception
     */
    public void delete(User user) throws TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.borrows().prefixSubMap(new Object[]{user}).clear();
			return true;
		}, () -> "User not found: %s".formatted(user));
	}

    /**
     * Prune.
     *
     * @throws TransactionException the transaction exception
     */
    public void prune() throws TransactionException {
		repository.transact(tx -> {
			// Prune all borrows based on if expired
			for (final var borrowEntry : tx.borrows().entrySet()) {
				if (!borrowEntry.getValue().expired()) continue;
				tx.borrows().remove(borrowEntry.getKey(), borrowEntry.getValue());
			}
			return true;
		}, () -> "Error while pruning");
	}
}
