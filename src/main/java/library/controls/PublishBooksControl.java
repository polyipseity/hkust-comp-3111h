package library.controls;

import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Control class for publishing books. This class provides methods to add books to a repository,
 * encapsulating transaction handling and data conflict resolution.
 *
 * @param repository the repository used to manage book data
 */
public record PublishBooksControl(Repository repository) {
    /**
     * Add book publish books control . add book result.
     *
     * @param book the book
     * @param data the data
     * @return the publish books control . add book result
     * @throws TransactionException the transaction exception
     */
    public PublishBooksControl.AddBookResult addBook(Book book, Book.Data data) throws TransactionException {
		return addBook(book, data, false);
	}

    /**
     * Add book publish books control . add book result.
     *
     * @param book      the book
     * @param data      the data
     * @param overwrite the overwrite
     * @return the publish books control . add book result
     * @throws TransactionException the transaction exception
     */
    public PublishBooksControl.AddBookResult addBook(Book book, Book.Data data, boolean overwrite) throws TransactionException {
		final var conflictBookData = new AtomicReference<Book.@Nullable Data>();
		try {
			repository.transact(tx -> {
				final var oldBookData = tx.books().put(book, data);
				conflictBookData.set(oldBookData);
				return overwrite || oldBookData == null;
			}, () -> "Failed to add book: %s".formatted(book));
		} catch (TransactionException e) {
			return switch (conflictBookData.get()) {
				case Book.Data data2 -> new AddBookResult.AlreadyExists(data2);
				case null -> throw e;
			};
		}
		return new AddBookResult.Success(conflictBookData.get());
	}

    /**
     * Result type for book operations.
     */
    public sealed interface AddBookResult permits AddBookResult.Success, AddBookResult.AlreadyExists {
        /**
         * The type Success.
         */
        record Success(@Nullable Book.Data conflictBookData) implements AddBookResult {
		}

        /**
         * The type Already exists.
         */
        record AlreadyExists(Book.Data conflictBookData) implements AddBookResult, HasMessage {

			@Override
			public String getMessage() {
				return "Book already exists";
			}
		}
	}
}
