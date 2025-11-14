package library.persistence;

import library.models.*;
import library.utils.HasMessage;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.jetbrains.annotations.NotNull;
import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;
import org.mapdb.serializer.SerializerArrayTuple;

import java.io.Closeable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Repository implements Closeable {
	@NotNull
	private final DB db;
	@NotNull
	private final HTreeMap<User, User.Data> users;
	@NotNull
	private final HTreeMap<Book, Book.Data> books;
	@NotNull
	private final HTreeMap<User, String[]> userNotifications;
	@NotNull
	private final BTreeMap<Object[], BookRequest.Data> userBookRequests; // key: (User, BookRequest)
	@NotNull
	private final BTreeMap<Object[], Borrow> userBookBorrows; // key: (User, Book)

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

		this.userNotifications = db.hashMap("userNotifications", userS, new SerializerArray<>(Serializer.STRING)).valueLoader(_ -> new String[0]).createOrOpen();
		this.userBookRequests = db.treeMap("userBookRequests", new SerializerArrayTuple(userS, bookRequestS), bookRequestDataS).createOrOpen();
		this.userBookBorrows = db.treeMap("userBookBorrows", new SerializerArrayTuple(userS, bookS), borrowS).createOrOpen();

		this.users = db.hashMap("users", userS, userDataS).modificationListener(((key, _, newValue, _) -> {
			if (newValue != null) return;
			final var key2 = new Object[]{key};
			userNotifications.remove(key);
			userBookRequests.prefixSubMap(key2).clear();
			userBookBorrows.prefixSubMap(key2).clear();
		})).createOrOpen();
		this.books = db.hashMap("books", bookS, bookDataS).modificationListener((key, _, newValue, _) -> {
			if (newValue != null) return;
			for (final var userBookBorrowKey : userBookBorrows.getKeys()) {
				if (key.equals(userBookBorrowKey[1])) {
					userBookBorrows.remove(userBookBorrowKey);
				}
			}
		}).createOrOpen();
	}

	public void transact(@NotNull BooleanFunction<@NotNull TransactData> action) throws TransactionException {
		try {
			var ok = false;
			try {
				ok = action.booleanValueOf(new TransactData(users, books, userNotifications, userBookRequests, userBookBorrows));
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
			if (tx.users.containsKey(user)) {
				return false;
			}
			tx.users.put(user, data);
			return true;
		});
	}

	@NotNull
	public Optional<User.Data> readUser(@NotNull User user) {
		return Optional.ofNullable(users.get(user));
	}

	public void updateUser(@NotNull User user, @NotNull Function<User.@NotNull Data, User.@NotNull Data> callback) throws TransactionException {
		transact(tx -> {
			final var oldValue = tx.users.get(user);
			if (oldValue == null) {
				return false;
			}
			tx.users.put(user, callback.apply(oldValue));
			return true;
		});
	}

	public void deleteUser(@NotNull User user) throws TransactionException {
		transact(tx -> tx.users.remove(user) != null);
	}

	public void updateUserNotification(@NotNull User user, @NotNull Function<@NotNull String[], @NotNull String[]> callback) throws TransactionException {
		transact(tx -> {
			if (!tx.users.containsKey(user)) {
				return false;
			}
			tx.userNotifications.put(user, callback.apply(tx.userNotifications.get(user)));
			return true;
		});
	}

	public void updateUserNotificationList(@NotNull User user, @NotNull Function<@NotNull List<String>, @NotNull List<String>> callback) throws TransactionException {
		updateUserNotification(user, oldValue -> callback.apply(new ArrayList<>(Arrays.asList(oldValue))).toArray(String[]::new));
	}

	@NotNull
	public Optional<BookRequest.Data> readUserBookRequest(@NotNull User user, @NotNull BookRequest bookRequest) {
		return Optional.ofNullable(userBookRequests.get(new Object[]{user, bookRequest}));
	}

	@NotNull
	public Map<BookRequest, BookRequest.Data> readUserBookRequest(@NotNull User user) {
		return userBookRequests.prefixSubMap(new Object[]{user}).entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> (BookRequest) entry.getKey()[1], Map.Entry::getValue));
	}

	public void createUserBookRequest(@NotNull User user, @NotNull BookRequest bookRequest, @NotNull BookRequest.Data data) throws TransactionException {
		transact(tx -> {
			final var key = new Object[]{user, bookRequest};
			if (!tx.users.containsKey(user) || tx.userBookRequests.containsKey(key)) {
				return false;
			}
			tx.userBookRequests.put(key, data);
			return true;
		});
	}

	public void updateUserBookRequest(@NotNull User user, @NotNull BookRequest bookRequest, @NotNull Function<BookRequest.@NotNull Data, BookRequest.@NotNull Data> callback) throws TransactionException {
		transact(tx -> {
			final var key = new Object[]{user, bookRequest};
			final var oldValue = tx.userBookRequests.get(key);
			if (oldValue == null) {
				return false;
			}
			tx.userBookRequests.put(key, callback.apply(oldValue));
			return true;
		});
	}

	public void deleteUserBookRequest(@NotNull User user, @NotNull BookRequest bookRequest) throws TransactionException {
		transact(tx -> tx.userBookRequests.remove(new Object[]{user, bookRequest}) != null);
	}

	public void deleteUserBookRequest(@NotNull User user) throws TransactionException {
		transact(tx -> {
			if (!tx.users.containsKey(user)) return false;
			tx.userBookRequests.prefixSubMap(new Object[]{user}).clear();
			return true;
		});
	}

	public void createBook(@NotNull Book book, @NotNull Book.Data data) throws TransactionException {
		transact(tx -> {
			if (tx.books.containsKey(book)) {
				return false;
			}
			tx.books.put(book, data);
			return true;
		});
	}

	@NotNull
	public Optional<Book.Data> readBook(@NotNull Book book) {
		return Optional.ofNullable(books.get(book));
	}

	public void updateBook(@NotNull Book book, @NotNull Function<Book.@NotNull Data, Book.@NotNull Data> callback) throws TransactionException {
		transact(tx -> {
			final var oldValue = tx.books.get(book);
			if (oldValue == null) {
				return false;
			}
			tx.books.put(book, callback.apply(oldValue));
			return true;
		});
	}

	public void deleteBook(@NotNull Book book) throws TransactionException {
		transact(tx -> tx.books.remove(book) != null);
	}

	@NotNull
	public Optional<Borrow> readUserBookBorrow(@NotNull User user, @NotNull Book book) {
		return Optional.ofNullable(userBookBorrows.get(new Object[]{user, book}));
	}

	@NotNull
	public Map<Book, Borrow> readUserBookBorrow(@NotNull User user) {
		return userBookBorrows.prefixSubMap(new Object[]{user}).entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(entry -> (Book) entry.getKey()[1], Map.Entry::getValue));
	}

	public void createUserBookBorrow(@NotNull User user, @NotNull Book book, @NotNull Borrow data) throws TransactionException {
		transact(tx -> {
			final var key = new Object[]{user, book};
			if (!tx.users.containsKey(user) || tx.userBookBorrows.containsKey(key)) {
				return false;
			}
			tx.userBookBorrows.put(key, data);
			return true;
		});
	}

	public void updateUserBookBorrow(@NotNull User user, @NotNull Book book, @NotNull Function<@NotNull Borrow, @NotNull Borrow> callback) throws TransactionException {
		transact(tx -> {
			final var key = new Object[]{user, book};
			final var oldValue = tx.userBookBorrows.get(key);
			if (oldValue == null) {
				return false;
			}
			tx.userBookBorrows.put(key, callback.apply(oldValue));
			return true;
		});
	}

	public void deleteUserBookBorrow(@NotNull User user, @NotNull Book book) throws TransactionException {
		transact(tx -> tx.userBookBorrows.remove(new Object[]{user, book}) != null);
	}

	public void deleteUserBookBorrow(@NotNull User user) throws TransactionException {
		transact(tx -> {
			if (!tx.users.containsKey(user)) return false;
			tx.userBookBorrows.prefixSubMap(new Object[]{user}).clear();
			return true;
		});
	}

	@Override
	public void close() {
		db.close();
	}

	public record TransactData(@NotNull HTreeMap<User, User.Data> users,
	                           @NotNull HTreeMap<Book, Book.Data> books,
	                           @NotNull HTreeMap<User, String[]> userNotifications,
	                           @NotNull BTreeMap<Object[], BookRequest.Data> userBookRequests,
	                           @NotNull BTreeMap<Object[], Borrow> userBookBorrows
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
