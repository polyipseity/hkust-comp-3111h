package library.persistence;

import library.models.Book;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryBookOps(Repository repository) {

	public void create(Book book, Book.Data data) throws TransactionException {
		repository.transact(tx -> tx.books().put(book, data) == null, () -> "Already created: %s".formatted(book));
	}

	public Map<Book, Book.Data> read() {
		return Collections.unmodifiableMap(repository.books);
	}

	public Map<Book, Book.Data> read(Predicate<? super Map.Entry<Book, Book.Data>> filter) {
		return repository.books.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public Optional<Book.Data> read(Book book) {
		return Optional.ofNullable(repository.books.get(book));
	}

	public Book.Data readOrThrow(Book book) {
		return read(book).orElseThrow(() -> new NoSuchElementException("Not found: %s".formatted(book)));
	}

	public void update(Book book, Function<Book.Data, Book.Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.books().get(book);
			return oldValue != null && oldValue.equals(tx.books().put(book, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(book));
	}

	public void update(Book book, Book.Data data, Book.@Nullable Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.books().put(book, data) != null : expected.equals(tx.books().put(book, data)),
				() -> "Not found or updated concurrently: %s".formatted(book)
		);
	}

	public void delete(Book book, @Nullable Book.Data expected) throws TransactionException {
		repository.transact(tx -> expected == null ? tx.books().remove(book) != null : tx.books().remove(book, expected), () -> "Already deleted: %s".formatted(book));
	}

	void delete(Book book) throws TransactionException {
		delete(book, null);
	}
}
