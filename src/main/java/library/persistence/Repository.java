package library.persistence;

import library.models.*;
import library.utils.Dates;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.jetbrains.annotations.NotNull;
import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;
import org.mapdb.serializer.SerializerArrayTuple;

import java.io.Closeable;
import java.util.function.Supplier;

public final class Repository implements Closeable {
	@NotNull
	public final RepositoryUserOps userOps = new RepositoryUserOps(this);
	@NotNull
	public final RepositoryBookOps bookOps = new RepositoryBookOps(this);
	@NotNull
	public final RepositoryUserNotificationOps userNotificationOps = new RepositoryUserNotificationOps(this);
	@NotNull
	public final RepositoryBookRequestOps bookRequestOps = new RepositoryBookRequestOps(this);
	@NotNull
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

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
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
		this.users = db.hashMap("users", userS, userDataS).modificationListener(((key, oldValue, newValue, _) -> {
			if (oldValue != null && newValue == null) {
				final var key2 = new Object[]{key};
				for (final var bookKey : this2.books.getKeys()) {
					switch (bookKey) {
						case Book(_, Author.ByRef(final var val), _) when key.equals(val) -> this2.books.remove(bookKey);
						default -> {
						}
					}
				}
				this2.userNotifications.remove(key);
				this2.userBookRequests.prefixSubMap(key2).clear();
				this2.borrows.prefixSubMap(key2).clear();
			}
		})).createOrOpen();
		this.books = db.hashMap("books", bookS, bookDataS).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null) {
				switch (newValue) {
					case Book.Data(_, _, _, _,final Book original, _) -> {
						switch (key.author()) {
							case Author.ByRef(final var val) when !users.containsKey(val) ->
									throw new IllegalStateException("User not found");
							case Author.ByRef _, Author.ByName _ -> {
							}
						}
						if (original != null) {
							switch (this2.books.get(original)) {
								case null -> throw new IllegalStateException("Original book not found");
								case Book.Data(_, _, _, _,final Book originalOrModified, _) when key.equals(originalOrModified) ->
										throw new IllegalStateException("New book linked to itself");
								case Book.Data(_, _, _, _,final Book originalOrModified, _) when originalOrModified != null ->
										throw new IllegalStateException("Original book already linked");
								case Book.Data data -> this2.books.put(original, data.withOriginalOrModified(key));
							}
						}
					}
					case null -> {
					}
				}
			}
			if (newValue == null) {
				switch (oldValue) {
					case Book.Data(_, _, _, _,final Book other, _) -> {
						if (other != null) {
							switch (this2.books.get(other)) {
								case null -> throw new IllegalStateException("Other book not found");
								case Book.Data(_, _, _, _, final Book originalOrModified, _) when !key.equals(originalOrModified) ->
										throw new IllegalStateException("Other book wrongly linked");
								case Book.Data data -> this2.books.put(other, data.withOriginalOrModified(null));
							}
						}
						for (final var userBookBorrowKey : this2.borrows.getKeys()) {
							if (key.equals(userBookBorrowKey[1])) {
								this2.borrows.remove(userBookBorrowKey);
							}
						}
					}
					case null -> {
					}
				}
			}
		}).createOrOpen();

		this.userNotifications = db.hashMap("userNotifications", userS, new SerializerArray<>(Serializer.STRING, String.class)).valueLoader(key -> users.containsKey(key) ? new String[0] : null).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null && newValue != null) {
				if (!users.containsKey(key)) {
					throw new IllegalStateException("User not found");
				}
			}
		}).createOrOpen();
		this.userBookRequests = db.treeMap("userBookRequests", new SerializerArrayTuple(userS, bookRequestS), bookRequestDataS).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null && newValue != null) {
				@SuppressWarnings("SuspiciousMethodCalls") final var containsUser = users.containsKey(key[0]);
				if (!containsUser) {
					throw new IllegalStateException("User not found");
				}
			}
		}).createOrOpen();
		this.borrows = db.treeMap("borrows", new SerializerArrayTuple(userS, bookS), borrowS).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null && newValue != null) {
				@SuppressWarnings("SuspiciousMethodCalls") final var containsUser = users.containsKey(key[0]);
				if (!containsUser) {
					throw new IllegalStateException("User not found");
				}
				@SuppressWarnings("SuspiciousMethodCalls") final var containsBook = books.containsKey(key[1]);
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

	public record TransactData(@NotNull HTreeMap<User, User.Data> users, @NotNull HTreeMap<Book, Book.Data> books,
	                           @NotNull HTreeMap<User, String[]> userNotifications,
	                           @NotNull BTreeMap<Object[], BookRequest.Data> userBookRequests,
	                           @NotNull BTreeMap<Object[], Borrow> borrows) {
	}
}
