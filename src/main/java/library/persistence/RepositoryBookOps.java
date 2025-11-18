package library.persistence;

import library.models.Book;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryBookOps(Repository repository) {

	public void create(@NotNull Book book, @NotNull Book.Data data) throws TransactionException {
		repository.transact(tx -> tx.books().put(book, data) == null);
	}

	@NotNull
	public Map<Book, Book.Data> read() {
		return Collections.unmodifiableMap(repository.books);
	}

	@NotNull
	public Map<Book, Book.Data> read(@NotNull Predicate<? super Map.@NotNull Entry<Book, Book.Data>> filter) {
		return repository.books.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@NotNull
	public Optional<Book.Data> read(@NotNull Book book) {
		return Optional.ofNullable(repository.books.get(book));
	}

	public void update(@NotNull Book book, @NotNull Function<Book.@NotNull Data, Book.@NotNull Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.books().get(book);
			if (oldValue == null) return false;
			tx.books().put(book, callback.apply(oldValue));
			return true;
		});
	}

	public void delete(@NotNull Book book) throws TransactionException {
		repository.transact(tx -> tx.books().remove(book) != null);
	}
}
