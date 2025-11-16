package library.persistence;

import library.models.Author;
import library.models.Book;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record RepositoryBookOps(Repository repository) {

	public void create(@NotNull Book book, @NotNull Book.Data data) throws TransactionException {
		repository.transact(tx -> tx.books().put(book, data) == null);
	}

	@NotNull
	public Optional<Book.Data> read(@NotNull Book book) {
		return Optional.ofNullable(repository.books.get(book));
	}

	public List<Book> getBooks() {
		List<Book> bookList = new ArrayList<>();
		for (Map.Entry<Book, Book.Data> entry : repository.books.entrySet()) {
			bookList.add(entry.getKey());
		}
		return bookList;
	}

	public List<Book> getBooksWithAuthor(@NotNull Author author2) {
		List<Book> bookList = new ArrayList<>();
		for (Map.Entry<Book, Book.Data> entry : repository.books.entrySet()) {
			if (entry.getKey().author().equals(author2)) {
				bookList.add(entry.getKey());
			}
		}
		return bookList;
	}

	public List<Book> getBooksWithTitle(@NotNull String title) {
		List<Book> bookList = new ArrayList<>();
		for (Map.Entry<Book, Book.Data> entry : repository.books.entrySet()) {
			if (entry.getKey().title().equals(title)) {
				bookList.add(entry.getKey());
			}
		}
		return bookList;
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
