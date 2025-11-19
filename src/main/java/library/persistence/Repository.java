package library.persistence;

import library.models.*;
import library.utils.ThrowingFunction;
import library.utils.Tuple6;
import org.jetbrains.annotations.NotNull;
import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;
import org.mapdb.serializer.SerializerArrayTuple;

import java.io.Closeable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class Repository implements Closeable {
	@NotNull
	public final RepositoryUserOps userOps = new RepositoryUserOps(this);
	@NotNull
	public final RepositoryBookOps bookOps = new RepositoryBookOps(this);
	@NotNull
	public final RepositoryUserNotificationOps userNotificationOps = new RepositoryUserNotificationOps(this);
	@NotNull
	public final RepositoryUserBookRequestOps userBookRequestOps = new RepositoryUserBookRequestOps(this);
	@NotNull
	public final RepositoryBorrowOps borrowOps = new RepositoryBorrowOps(this);
	@NotNull
	private final DBMaker.Maker dbMaker;
	@NotNull
	private final ReentrantLock transactLock = new ReentrantLock();
	@NotNull
	DB db;
	@NotNull
	HTreeMap<User, User.Data> users;
	@NotNull
	HTreeMap<Book, Book.Data> books;
	@NotNull
	HTreeMap<User, String[]> userNotifications;
	@NotNull
	BTreeMap<Object[], BookRequest.Data> userBookRequests; // key: (User, BookRequest)
	@NotNull
	BTreeMap<Object[], Borrow> borrows; // key: (User, Book)

	public Repository(@NotNull DBMaker.Maker dbMaker) {
		this.dbMaker = dbMaker.transactionEnable();
		final var stores = open();
		this.db = stores._1();
		this.users = stores._2();
		this.books = stores._3();
		this.userNotifications = stores._4();
		this.userBookRequests = stores._5();
		this.borrows = stores._6();
	}

	private void reopen() {
		db.close();
		final var stores = open();
		this.db = stores._1();
		this.users = stores._2();
		this.books = stores._3();
		this.userNotifications = stores._4();
		this.userBookRequests = stores._5();
		this.borrows = stores._6();
	}

	@NotNull
	private Tuple6<DB, HTreeMap<User, User.Data>, HTreeMap<Book, Book.Data>, HTreeMap<User, String[]>, BTreeMap<Object[], BookRequest.Data>, BTreeMap<Object[], Borrow>> open() {
		final var userS = new User.S();
		final var userDataS = new User.Data.S();
		final var authorS = new Author.S(userS);

		final var bookRequestS = new BookRequest.S();
		final var bookRequestDataS = new BookRequest.Data.S();

		final var bookS = new Book.S(authorS);
		final var bookDataS = new Book.Data.S(bookS);
		final var borrowS = new Borrow.S();

		final var db = dbMaker.make();
		final var this2 = this;
		final var users = db.hashMap("users", userS, userDataS).modificationListener(((key, oldValue, newValue, _) -> {
			if (oldValue != null && newValue == null) {
				final var key2 = new Object[]{key};
				for (final var bookKey : books.getKeys()) {
					if (bookKey instanceof Book(_, Author.ByRef(final var val), _) && key.equals(val)) {
						books.remove(bookKey);
					}
				}
				userNotifications.remove(key);
				userBookRequests.prefixSubMap(key2).clear();
				borrows.prefixSubMap(key2).clear();
			}
		})).createOrOpen();
		final var books = db.hashMap("books", bookS, bookDataS).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null) {
				switch (newValue) {
					case Book.Data(_, _, _, _, final Book original, _) -> {
						switch (key.author()) {
							case Author.ByRef(final var val) when !users.containsKey(val) ->
									throw new IllegalStateException("User not found");
							case Author.ByRef _, Author.ByName _ -> {
							}
						}
						if (original != null) {
							switch (this2.books.get(original)) {
								case null -> throw new IllegalStateException("Original book not found");
								case Book.Data(_, _, _, _, final Book originalOrModified, _) when key.equals(originalOrModified) ->
										throw new IllegalStateException("New book linked to itself");
								case Book.Data(_, _, _, _, final Book originalOrModified, _) when originalOrModified != null ->
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
					case Book.Data(_, _, _, _, final Book other, _) -> {
						if (other != null) {
							switch (this2.books.get(other)) {
								case null -> throw new IllegalStateException("Other book not found");
								case Book.Data(_, _, _, _, final Book originalOrModified, _) when !key.equals(originalOrModified) ->
										throw new IllegalStateException("Other book wrongly linked");
								case Book.Data data -> this2.books.put(other, data.withOriginalOrModified(null));
							}
						}
						for (final var userBookBorrowKey : borrows.getKeys()) {
							if (key.equals(userBookBorrowKey[1])) {
								borrows.remove(userBookBorrowKey);
							}
						}
					}
					case null -> {
					}
				}
			}
		}).createOrOpen();

		final var userNotifications = db.hashMap("userNotifications", userS, new SerializerArray<>(Serializer.STRING, String.class)).valueLoader(key -> users.containsKey(key) ? new String[0] : null).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null && newValue != null && !users.containsKey(key)) {
				throw new IllegalStateException("User not found");
			}
		}).createOrOpen();
		final var userBookRequests = db.treeMap("userBookRequests", new SerializerArrayTuple(userS, bookRequestS), bookRequestDataS).modificationListener((key, oldValue, newValue, _) -> {
			if (oldValue == null && newValue != null) {
				@SuppressWarnings("SuspiciousMethodCalls") final var containsUser = users.containsKey(key[0]);
				if (!containsUser) {
					throw new IllegalStateException("User not found");
				}
			}
		}).createOrOpen();
		final var borrows = db.treeMap("borrows", new SerializerArrayTuple(userS, bookS), borrowS).modificationListener((key, oldValue, newValue, _) -> {
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

		return new Tuple6<>(db, users, books, userNotifications, userBookRequests, borrows);
	}

	public void transact(@NotNull ThrowingFunction<@NotNull Data, @NotNull Boolean> action, @NotNull Supplier<String> rollbackMessageSupplier) throws TransactionException {
		transactLock.lock();
		try {
			if (!action.apply(new Data())) {
				throw new DummyException();
			}
			if (transactLock.getHoldCount() == 1) {
				db.commit();
			}
		} catch (DummyException exception) {
			db.rollback();
			reopen();
			throw new TransactionException(rollbackMessageSupplier.get());
		} catch (Exception exception) {
			db.rollback();
			reopen();
			throw new TransactionException(exception);
		} finally {
			transactLock.unlock();
		}
	}

	void transact(@NotNull ThrowingFunction<@NotNull Data, @NotNull Boolean> action) throws TransactionException {
		transact(action, () -> "Transaction rolled back");
	}

	@Override
	public void close() {
		db.close();
	}

	private final static class DummyException extends Exception {
	}

	public final class Data {
		public @NotNull HTreeMap<User, User.Data> users() {
			return users;
		}

		public @NotNull HTreeMap<Book, Book.Data> books() {
			return books;
		}

		public @NotNull HTreeMap<User, String[]> userNotifications() {
			return userNotifications;
		}

		public @NotNull BTreeMap<Object[], BookRequest.Data> userBookRequests() {
			return userBookRequests;
		}

		public @NotNull BTreeMap<Object[], Borrow> borrows() {
			return borrows;
		}
	}
}
