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

public record RepositoryBorrowOps(Repository repository) {
	public Map<Tuple2<User, Book>, Borrow> read() {
		return read(_ -> true);
	}

	public Map<Tuple2<User, Book>, Borrow> read(Predicate<? super Map.Entry<Tuple2<User, Book>, Borrow>> filter) {
		return repository.borrows.entrySet().stream().map(entry -> Map.entry(new Tuple2<>((User) entry.getKey()[0], (Book) entry.getKey()[1]), entry.getValue())).filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public Optional<Borrow> read(User user, Book book) {
		return Optional.ofNullable(repository.borrows.get(new Object[]{user, book}));
	}

	public Borrow readOrThrow(User user, Book book) {
		return read(user, book)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s, %s".formatted(user, book)));
	}

	public Map<Book, Borrow> read(User user) {
		return repository.borrows.prefixSubMap(new Object[]{user}).entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(entry -> (Book) entry.getKey()[1], Map.Entry::getValue));
	}

	public Map<User, Borrow> read(Book book) {
		return repository.borrows.entrySet().stream()
				.filter(entry -> book.equals(entry.getKey()[1]))
				.collect(Collectors.toUnmodifiableMap(entry -> (User) entry.getKey()[0], Map.Entry::getValue));
	}

	public void create(User user, Book book, Borrow data) throws TransactionException {
		repository.transact(tx -> tx.borrows().put(new Object[]{user, book}, data) == null, () -> "Already created: %s, %s".formatted(user, book));
	}

	public void update(User user, Book book, Function<Borrow, Borrow> callback) throws TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, book};
			final var oldValue = tx.borrows().get(key);
			return oldValue != null && oldValue.equals(tx.borrows().put(key, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s,  %s".formatted(user, book));
	}

	public void update(User user,
	                   Book book,
	                   Borrow data,
	                   @Nullable Borrow expected) throws TransactionException {
		repository.transact(
				tx -> {
					final var key = new Object[]{user, book};
					return expected == null
							? tx.borrows().put(key, data) != null
							: expected.equals(tx.borrows().put(key, data));
				},
				() -> "Not found or updated concurrently: %s,  %s".formatted(user, book)
		);
	}

	public void delete(User user,
	                   Book book,
	                   @Nullable Borrow expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.borrows().remove(new Object[]{user, book}) != null : tx.borrows().remove(new Object[]{user, book}, expected),
				() -> "Already deleted: %s, %s".formatted(user, book)
		);
	}

	void delete(User user, Book book) throws TransactionException {
		delete(user, book, null);
	}

	public void delete(User user) throws TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.borrows().prefixSubMap(new Object[]{user}).clear();
			return true;
		}, () -> "User not found: %s".formatted(user));
	}
}
