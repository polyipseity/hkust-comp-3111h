package library.persistence;

import library.models.Author;
import library.models.Book;
import org.jetbrains.annotations.NotNull;

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
	public Optional<Book.Data> read(@NotNull Book book) {
		return Optional.ofNullable(repository.books.get(book));
	}

	@NotNull
	public Map<Book, Book.Data> read(@NotNull Predicate<? super Map.Entry<Book, Book.Data>> filter) {
		return repository.books.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@NotNull
	public Map<Book, Book.Data> read(@NotNull Author author) {
		return read(entry -> author.equals(entry.getKey().author()));
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
