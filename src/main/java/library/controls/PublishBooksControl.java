package library.controls;

import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public record PublishBooksControl(@NotNull Repository repository) {
	public @NotNull PublishBooksControl.AddBookResult addBook(@NotNull Book book, @NotNull Book.Data data) throws TransactionException {
		return addBook(book, data, false);
	}

	public @NotNull PublishBooksControl.AddBookResult addBook(@NotNull Book book, @NotNull Book.Data data, boolean overwrite) throws TransactionException {
		final var conflictBookData = new AtomicReference<Book.Data>();
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
		record Success(@Nullable Book.Data conflictBookData) implements AddBookResult {
		}

		record AlreadyExists(@NotNull Book.Data conflictBookData) implements AddBookResult, HasMessage {

			@Override
			public @NotNull String getMessage() {
				return "Book already exists";
			}
		}
	}
}
