package library.persistence;

import library.models.*;
import library.utils.ByteArray;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

	public static boolean populateRepository(@NotNull Repository.TransactData data) {
		final var reader = new User("reader");
		final var author = new User("author");
		final var librarian = new User("librarian");
		data.users().put(reader, new User.Data(User.Role.STUDENT_STAFF, true, "reader", "reader"));
		data.users().put(author, new User.Data(User.Role.AUTHOR, true, "author", "author"));
		data.users().put(librarian, new User.Data(User.Role.LIBRARIAN, true, "librarian", "librarian"));

		final var book = new Book("book", new Author.ByRef(author));
		final var book2 = new Book("book", new Author.ByName("author"));
		final var oldBook = new Book("book2", new Author.ByRef(author));
		final var newBook = new Book("book2", new Author.ByRef(author), true);
		data.books().put(book, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, null, 42));
		data.books().put(book2, new Book.Data("summary", "content", Book.ApprovalStatus.REJECTED, null, 42));
		data.books().put(oldBook, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, null, 42)); // `originalOrModified`: newBook
		data.books().put(newBook, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, oldBook, 42));

		data.userNotifications().put(reader, new String[]{"notification", "notification2"});
		data.userNotifications().put(author, new String[]{"notification"});
		data.userNotifications().put(librarian, new String[]{});

		data.userBookRequests().put(new Object[]{reader, new BookRequest("title", "author")}, new BookRequest.Data(new Date()));
		data.userBookRequests().put(new Object[]{author, new BookRequest("title", "author")}, new BookRequest.Data(new Date()));
		data.userBookRequests().put(new Object[]{librarian, new BookRequest("title", "author")}, new BookRequest.Data(new Date()));

		data.borrows().put(new Object[]{reader, book}, new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42])));
		data.borrows().put(new Object[]{author, book2}, new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42])));
		data.borrows().put(new Object[]{librarian, oldBook}, new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42])));

		return true;
	}

	@BeforeEach
	void setUp() {

	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void transact() {
		try (final var repository = new Repository(DBMaker::memoryDirectDB)) {
			assertDoesNotThrow(() -> repository.transact(_ -> true));

			assertThrows(TransactionException.class, () -> repository.transact(_ -> false));

			assertThrows(TransactionException.class, () -> repository.transact(_ -> {
				throw new RuntimeException();
			}));

			assertThrows(Error.class, () -> repository.transact(_ -> {
				throw new Error();
			}));

			assertDoesNotThrow(() -> repository.transact(RepositoryTest::populateRepository));

			repository.close();
			assertThrows(IllegalAccessError.class, () -> repository.transact(RepositoryTest::populateRepository));
		}
	}

	@Test
	void transactConstraints() {
		try (final var repository = new Repository(DBMaker::memoryDirectDB)) {
			assertDoesNotThrow(() -> repository.transact(RepositoryTest::populateRepository));

			// book `originalOrModified` linkage constraints
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.books().put(new Book("book with missing author", new Author.ByRef(new User("missing author"))), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, 42));
				return true;
			}));
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.books().put(new Book("book with missing linkage", new Author.ByName("author")), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, new Book("missing book", new Author.ByName("author")), 42));
				return true;
			}));
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.books().put(new Book("book with self-linkage", new Author.ByName("author")), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, new Book("book with self-linkage", new Author.ByName("author")), 42));
				return true;
			}));
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.books().put(new Book("book with duplicate linkage", new Author.ByName("author")), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, new Book("book2", new Author.ByRef(new User("author"))), 42));
				return true;
			}));

			final var bookToLink = new Book("book", new Author.ByName("author"));
			final var bookToLinkData = Objects.requireNonNull(repository.books.get(bookToLink));
			final var newBookToLink = new Book("book with linkage", new Author.ByName("author"));
			assertDoesNotThrow(() -> repository.transact(tx -> tx.books().put(newBookToLink, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, new Book("book", new Author.ByName("author")), 42)) == null));
			assertEquals(bookToLinkData.withOriginalOrModified(newBookToLink), repository.books.get(bookToLink));
			assertDoesNotThrow(() -> repository.transact(tx -> tx.books().remove(newBookToLink) != null));
			assertEquals(bookToLinkData, repository.books.get(bookToLink));

			// missing user
			final var missingUser = new User("missing user");
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.books().put(new Book("title", new Author.ByRef(missingUser)), new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, null, 42));
				return true;
			}));
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.userNotifications().put(missingUser, new String[]{"notification"});
				return true;
			}));
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.userBookRequests().put(new Object[]{missingUser, new BookRequest("title", "author")}, new BookRequest.Data(new Date()));
				return true;
			}));
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.borrows().put(new Object[]{missingUser, bookToLink}, new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42])));
				return true;
			}));

			// missing book
			final var user = new User("reader");
			final var missingBook = new Book("missing book", new Author.ByName("author"), true);
			assertThrows(TransactionException.class, () -> repository.transact(tx -> {
				tx.borrows().put(new Object[]{user, missingBook}, new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42])));
				return true;
			}));

			// remove user
			final var newUser = new User("new user");
			final var newUserData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");

			assertDoesNotThrow(() -> repository.transact(tx -> tx.users().put(newUser, newUserData) == null));
			assertEquals(newUserData, repository.users.get(newUser));

			final var newBookToRemove = new Book("title", new Author.ByRef(newUser));
			final var newBookToRemoveData = new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, null, 42);
			assertDoesNotThrow(() -> repository.transact(tx -> tx.books().put(newBookToRemove, newBookToRemoveData) == null));
			assertEquals(newBookToRemoveData, repository.books.get(newBookToRemove));

			final var newUserNotificationsToRemove = new String[]{"notification"};
			assertDoesNotThrow(() -> repository.transact(tx -> tx.userNotifications().put(newUser, newUserNotificationsToRemove) == null));
			assertArrayEquals(newUserNotificationsToRemove, repository.userNotifications.get(newUser));

			final var newBookRequestToRemove = new BookRequest("title", "author");
			final var newBookRequestToRemoveData = new BookRequest.Data(new Date());
			assertDoesNotThrow(() -> repository.transact(tx -> tx.userBookRequests().put(new Object[]{newUser, newBookRequestToRemove}, newBookRequestToRemoveData) == null));
			assertEquals(newBookRequestToRemoveData, repository.userBookRequests.get(new Object[]{newUser, newBookRequestToRemove}));

			final var borrowToRemove = new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42]));
			assertDoesNotThrow(() -> repository.transact(tx -> tx.borrows().put(new Object[]{newUser, bookToLink}, borrowToRemove) == null));
			assertEquals(borrowToRemove, repository.borrows.get(new Object[]{newUser, bookToLink}));

			assertDoesNotThrow(() -> repository.transact(tx -> tx.users().remove(newUser) != null));
			assertNull(repository.users.get(newUser));
			assertNull(repository.books.get(newBookToRemove));
			assertNull(repository.userNotifications.get(newUser));
			assertNull(repository.userBookRequests.get(new Object[]{newUser, newBookRequestToRemove}));
			assertNull(repository.borrows.get(new Object[]{newUser, bookToLink}));

			// remove book
			final var newBook = new Book("new book", new Author.ByRef(user));
			final var newBookData = new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, null, 42);

			assertDoesNotThrow(() -> repository.transact(tx -> tx.books().put(newBook, newBookData) == null));
			assertEquals(newBookData, repository.books.get(newBook));

			final var newBorrowToRemove2 = new Borrow(new Date(), Duration.ofNanos(42), new ByteArray(new byte[42]));
			assertDoesNotThrow(() -> repository.transact(tx -> tx.borrows().put(new Object[]{user, newBook}, newBorrowToRemove2) == null));
			assertEquals(newBorrowToRemove2, repository.borrows.get(new Object[]{user, newBook}));

			assertDoesNotThrow(() -> repository.transact(tx -> tx.books().remove(newBook) != null));
			assertNull(repository.books.get(newBook));
			assertNull(repository.borrows.get(new Object[]{user, newBook}));
		}
	}

	@Test
	void close() {
		try (final var repository = new Repository(DBMaker::memoryDirectDB)) {
			assertDoesNotThrow(() -> repository.transact(RepositoryTest::populateRepository));

			repository.close();
			assertThrows(IllegalAccessError.class, () -> repository.transact(RepositoryTest::populateRepository));
		}
	}
}
