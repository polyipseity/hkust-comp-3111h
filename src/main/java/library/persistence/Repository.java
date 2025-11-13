package library.persistence;

import library.models.Author;
import library.models.Book;
import library.models.BookRequest;
import library.models.User;
import library.utils.HasMessage;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.Closeable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Repository implements Closeable {
	@NotNull
	private final DB db;
	@NotNull
	private final HTreeMap<User, User.Data> users;
	@NotNull
	private final HTreeMap<Book, Book.Data> books;

	public Repository(@NotNull Supplier<DBMaker.Maker> dbMaker) {
		this.db = dbMaker.get().transactionEnable().make();

		final var bookRequestS = new BookRequest.S();
		final var bookRequestDataS = new BookRequest.Data.S();
		final var userS = new User.S();
		final var userDataS = new User.Data.S(bookRequestS, bookRequestDataS);
		final var authorS = new Author.S(userS);
		final var borrowS = new Book.Borrow.S();
		final var bookS = new Book.S(authorS);
		final var bookDataS = new Book.Data.S(userS, bookS, borrowS);

		this.users = db.hashMap("users", userS, userDataS).createOrOpen();
		this.books = db.hashMap("books", bookS, bookDataS).createOrOpen();
	}

	public void transact(@NotNull BooleanFunction<TransactData> action) throws TransactionException {
		try {
			var ok = false;
			try {
				ok = action.booleanValueOf(new TransactData(users, books));
			} finally {
				if (!ok) {
					db.rollback();
				}
			}
			if (ok) {
				try {
					db.commit();
					return;
				} catch (Exception exception) {
					db.rollback();
					throw exception;
				}
			}
		} catch (Exception exception) {
			throw new TransactionException(exception);
		}
		throw new TransactionException();
	}

	public void createUser(@NotNull User user, @NotNull User.Data data) throws TransactionException {
		transact(tx -> {
			if (tx.users().containsKey(user)) {
				return false;
			}
			tx.users().put(user, data);
			return true;
		});
	}

	@NotNull
	public Optional<User.Data> readUser(@NotNull User user) {
		return Optional.ofNullable(users.get(user));
	}

	public void updateUser(@NotNull User user, @NotNull Function<User.Data, User.Data> callback) throws TransactionException {
		transact(tx -> {
			final var old = tx.users().get(user);
			if (old == null) {
				return false;
			}
			tx.users().put(user, callback.apply(old));
			return true;
		});
	}

	public void deleteUser(@NotNull User user) throws TransactionException {
		transact(tx -> tx.users().remove(user) != null);
	}

	public void createBook(@NotNull Book book, @NotNull Book.Data data) throws TransactionException {
		transact(tx -> {
			if (tx.books().containsKey(book)) {
				return false;
			}
			tx.books().put(book, data);
			return true;
		});
	}

	@NotNull
	public Optional<Book.Data> readBook(@NotNull Book book) {
		return Optional.ofNullable(books.get(book));
	}

	public void updateBook(@NotNull Book book, @NotNull Function<Book.Data, Book.Data> callback) throws TransactionException {
		transact(tx -> {
			final var old = tx.books().get(book);
			if (old == null) {
				return false;
			}
			tx.books().put(book, callback.apply(old));
			return true;
		});
	}

	public void deleteBook(@NotNull Book book) throws TransactionException {
		transact(tx -> tx.books().remove(book) != null);
	}

	@Override
	public void close() {
		db.close();
	}

	public record TransactData(HTreeMap<User, User.Data> users, HTreeMap<Book, Book.Data> books) {
	}

	public static final class TransactionException extends Exception implements HasMessage {
		/**
		 * Constructs a new exception with {@code null} as its detail message.
		 * The cause is not initialized, and may subsequently be initialized by a
		 * call to {@link #initCause}.
		 */
		public TransactionException() {
			super();
		}

		/**
		 * Constructs a new exception with the specified detail message.  The
		 * cause is not initialized, and may subsequently be initialized by
		 * a call to {@link #initCause}.
		 *
		 * @param message the detail message. The detail message is saved for
		 *                later retrieval by the {@link #getMessage()} method.
		 */
		public TransactionException(String message) {
			super(message);
		}

		/**
		 * Constructs a new exception with the specified detail message and
		 * cause.  <p>Note that the detail message associated with
		 * {@code cause} is <i>not</i> automatically incorporated in
		 * this exception's detail message.
		 *
		 * @param message the detail message (which is saved for later retrieval
		 *                by the {@link #getMessage()} method).
		 * @param cause   the cause (which is saved for later retrieval by the
		 *                {@link #getCause()} method).  (A {@code null} value is
		 *                permitted, and indicates that the cause is nonexistent or
		 *                unknown.)
		 * @since 1.4
		 */
		public TransactionException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Constructs a new exception with the specified cause and a detail
		 * message of {@code (cause==null ? null : cause.toString())} (which
		 * typically contains the class and detail message of {@code cause}).
		 * This constructor is useful for exceptions that are little more than
		 * wrappers for other throwables (for example, {@link
		 * java.security.PrivilegedActionException}).
		 *
		 * @param cause the cause (which is saved for later retrieval by the
		 *              {@link #getCause()} method).  (A {@code null} value is
		 *              permitted, and indicates that the cause is nonexistent or
		 *              unknown.)
		 * @since 1.4
		 */
		public TransactionException(Throwable cause) {
			super(cause);
		}

		@Override
		public @NotNull String getMessage() {
			return switch (super.getMessage()) {
				case "" -> "Database transaction exception";
				case String val -> "Database transaction exception: %s".formatted(val);
			};
		}
	}
}
