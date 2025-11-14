package library.persistence;

import library.models.*;
import library.utils.HasMessage;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.jetbrains.annotations.NotNull;
import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;
import org.mapdb.serializer.SerializerArrayTuple;

import java.io.Closeable;
import java.util.function.Supplier;

public final class Repository implements Closeable {
	public final RepositoryUserOps userOps = new RepositoryUserOps(this);
	public final RepositoryBookOps bookOps = new RepositoryBookOps(this);
	public final RepositoryUserNotificationOps userNotificationOps = new RepositoryUserNotificationOps(this);
	public final RepositoryBookRequestOps bookRequestOps = new RepositoryBookRequestOps(this);
	public final RepositoryBorrowOps borrowOps = new RepositoryBorrowOps(this);
	@NotNull
	final DB db;
	@NotNull
	final HTreeMap<User, User.Data> users;
	@NotNull
	final HTreeMap<Book, Book.Data> books;
	@NotNull
	final HTreeMap<User, String[]> userNotifications;
	@NotNull
	final BTreeMap<Object[], BookRequest.Data> userBookRequests; // key: (User, BookRequest)
	@NotNull
	final BTreeMap<Object[], Borrow> borrows; // key: (User, Book)

	public Repository(@NotNull Supplier<DBMaker.Maker> dbMaker) {
		this.db = dbMaker.get().transactionEnable().make();

		final var userS = new User.S();
		final var userDataS = new User.Data.S();
		final var authorS = new Author.S(userS);

		final var bookRequestS = new BookRequest.S();
		final var bookRequestDataS = new BookRequest.Data.S();

		final var bookS = new Book.S(authorS);
		final var bookDataS = new Book.Data.S(bookS);
		final var borrowS = new Borrow.S();

		final var this2 = this;
		this.users = db.hashMap("users", userS, userDataS).modificationListener(((key, _, newValue, _) -> {
			if (newValue == null) {
				final var key2 = new Object[]{key};
				this2.userNotifications.remove(key);
				this2.userBookRequests.prefixSubMap(key2).clear();
				this2.borrows.prefixSubMap(key2).clear();
			}
		})).createOrOpen();
		this.books = db.hashMap("books", bookS, bookDataS).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null) {
				switch (newValue) {
					case Book.Data(_, _, _, final Book original, _) when original != null -> {
						switch (this2.books.get(original)) {
							case null -> throw new IllegalStateException("Original book not found");
							case Book.Data data -> this2.books.put(original, data.withOriginalOrModified(key));
						}
					}
					case null, default -> {
					}
				}
			}
			if (newValue == null) {
				for (final var userBookBorrowKey : this2.borrows.getKeys()) {
					if (key.equals(userBookBorrowKey[1])) {
						this2.borrows.remove(userBookBorrowKey);
					}
				}
				switch (oldValue) {
					case Book.Data(_, _, _, final Book other, _) when other != null -> {
						switch (this2.books.get(other)) {
							case null -> throw new IllegalStateException("Other book not found");
							case Book.Data data -> this2.books.put(other, data.withOriginalOrModified(null));
						}
					}
					case null, default -> {
					}
				}
			}
		}).createOrOpen();

		this.userNotifications = db.hashMap("userNotifications", userS, new SerializerArray<>(Serializer.STRING)).valueLoader(_ -> new String[0])
				.modificationListener((key, oldValue, newValue, _) -> {
					if (oldValue == null && newValue != null) {
						if (!users.containsKey(key)) {
							throw new IllegalStateException("User not found");
						}
					}
				})
				.createOrOpen();
		this.userBookRequests = db.treeMap("userBookRequests", new SerializerArrayTuple(userS, bookRequestS), bookRequestDataS)
				.modificationListener((key, oldValue, newValue, _) -> {
					if (oldValue == null && newValue != null) {
						@SuppressWarnings("SuspiciousMethodCalls") final var containsUser = users.containsKey(key[0]);
						if (!containsUser) {
							throw new IllegalStateException("User not found");
						}
					}
				})
				.createOrOpen();
		this.borrows = db.treeMap("borrows", new SerializerArrayTuple(userS, bookS), borrowS)
				.modificationListener((key, oldValue, newValue, _) -> {
					if (oldValue == null && newValue != null) {
						@SuppressWarnings("SuspiciousMethodCalls") final var containsUser = users.containsKey(key[0]);
						if (!containsUser) {
							throw new IllegalStateException("User not found");
						}
						@SuppressWarnings("SuspiciousMethodCalls") final var containsBook = users.containsKey(key[1]);
						if (!containsBook) {
							throw new IllegalStateException("Book not found");
						}
					}
				}).createOrOpen();
	}

	public void transact(@NotNull BooleanFunction<@NotNull TransactData> action) throws TransactionException {
		try {
			var ok = false;
			try {
				ok = action.booleanValueOf(new TransactData(users, books, userNotifications, userBookRequests, borrows));
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

	@Override
	public void close() {
		db.close();
	}

	public record TransactData(@NotNull HTreeMap<User, User.Data> users,
	                           @NotNull HTreeMap<Book, Book.Data> books,
	                           @NotNull HTreeMap<User, String[]> userNotifications,
	                           @NotNull BTreeMap<Object[], BookRequest.Data> userBookRequests,
	                           @NotNull BTreeMap<Object[], Borrow> borrows
	) {
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
