package library.persistence;

import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryBorrowOps(Repository repository) {
	@NotNull
	public Map<Tuple2<User, Book>, Borrow> read() {
		return read(_ -> true);
	}

	@NotNull
	public Map<Tuple2<User, Book>, Borrow> read(@NotNull Predicate<? super Map.@NotNull Entry<Tuple2<User, Book>, Borrow>> filter) {
		return repository.borrows.entrySet().stream().map(entry -> Map.entry(new Tuple2<>((User) entry.getKey()[0], (Book) entry.getKey()[1]), entry.getValue())).filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@NotNull
	public Optional<Borrow> read(@NotNull User user, @NotNull Book book) {
		return Optional.ofNullable(repository.borrows.get(new Object[]{user, book}));
	}

	@NotNull
	public Borrow readOrThrow(@NotNull User user, @NotNull Book book) {
		return read(user, book)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s, %s".formatted(user, book)));
	}

	@NotNull
	public Map<Book, Borrow> read(@NotNull User user) {
		return repository.borrows.prefixSubMap(new Object[]{user}).entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(entry -> (Book) entry.getKey()[1], Map.Entry::getValue));
	}

	@NotNull
	public Map<User, Borrow> read(@NotNull Book book) {
		return repository.borrows.entrySet().stream()
				.filter(entry -> book.equals(entry.getKey()[1]))
				.collect(Collectors.toUnmodifiableMap(entry -> (User) entry.getKey()[0], Map.Entry::getValue));
	}

	public void create(@NotNull User user, @NotNull Book book, @NotNull Borrow data) throws TransactionException {
		repository.transact(tx -> tx.borrows().put(new Object[]{user, book}, data) == null, () -> "Already created: %s, %s".formatted(user, book));
	}

	public void update(@NotNull User user, @NotNull Book book, @NotNull Function<@NotNull Borrow, @NotNull Borrow> callback) throws TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, book};
			final var oldValue = tx.borrows().get(key);
			return oldValue != null && oldValue.equals(tx.borrows().put(key, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s,  %s".formatted(user, book));
	}

	public void delete(@NotNull User user, @NotNull Book book) throws TransactionException {
		repository.transact(tx -> tx.borrows().remove(new Object[]{user, book}) != null, () -> "Already deleted: %s, %s".formatted(user, book));
	}

	public void delete(@NotNull User user) throws TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.borrows().prefixSubMap(new Object[]{user}).clear();
			return true;
		}, () -> "User not found: %s".formatted(user));
	}
}
