package library.persistence;

import library.models.*;
import library.utils.ByteArray;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {
	private Repository repository;

	@SuppressWarnings("SameReturnValue")
	private static boolean populate(@NotNull Repository.TransactData data) {
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
		data.books().put(book, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
		data.books().put(book2, new Book.Data("summary", "content", Book.ApprovalStatus.REJECTED, null, null, 42));
		data.books().put(oldBook, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42)); // `originalOrModified`: newBook
		data.books().put(newBook, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, oldBook, 42));

		data.userNotifications().put(reader, new String[]{"notification", "notification2"});
		data.userNotifications().put(author, new String[]{"notification"});
		data.userNotifications().put(librarian, new String[]{});

		data.userBookRequests().put(new Object[]{reader, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));
		data.userBookRequests().put(new Object[]{author, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));
		data.userBookRequests().put(new Object[]{librarian, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));

		data.borrows().put(new Object[]{reader, book}, new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf"));
		data.borrows().put(new Object[]{author, book2}, new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf"));
		data.borrows().put(new Object[]{librarian, oldBook}, new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf"));

		return true;
	}

	@BeforeEach
	void setUp() {
		repository = new Repository(DBMaker::memoryDirectDB);
	}

	@AfterEach
	void tearDown() {
		try (final var _ = repository) {
			repository = null;
		}
	}

	@Test
	void transact() {
		assertDoesNotThrow(() -> repository.transact(_ -> true));

		assertThrows(TransactionException.class, () -> repository.transact(_ -> false));

		assertThrows(TransactionException.class, () -> repository.transact(_ -> {
			throw new RuntimeException();
		}));

		assertThrows(Error.class, () -> repository.transact(_ -> {
			throw new Error();
		}));

		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		repository.close();
		assertThrows(IllegalAccessError.class, () -> repository.transact(RepositoryTest::populate));
	}

	@Test
	void transact_bookLinkConstraint() {
		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		// book `originalOrModified` link constraints
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.books().put(new Book("book with missing author", new Author.ByRef(new User("missing author"))), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, null, 42));
			return true;
		}));
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.books().put(new Book("book with missing link", new Author.ByName("author")), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, new Book("missing book", new Author.ByName("author")), 42));
			return true;
		}));
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.books().put(new Book("book with self-link", new Author.ByName("author")), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, new Book("book with self-link", new Author.ByName("author")), 42));
			return true;
		}));
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.books().put(new Book("book with duplicate link", new Author.ByName("author")), new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, new Book("book2", new Author.ByRef(new User("author"))), 42));
			return true;
		}));

		final var existingBook = new Book("book", new Author.ByName("author"));
		final var existingBookData = Objects.requireNonNull(repository.books.get(existingBook));
		final var newBookToLink = new Book("book with link", new Author.ByName("author"));
		assertDoesNotThrow(() -> repository.transact(tx -> tx.books().put(newBookToLink, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, new Book("book", new Author.ByName("author")), 42)) == null));
		assertEquals(existingBookData.withOriginalOrModified(newBookToLink), repository.books.get(existingBook));
		assertDoesNotThrow(() -> repository.transact(tx -> tx.books().remove(newBookToLink) != null));
		assertEquals(existingBookData, repository.books.get(existingBook));

	}

	@Test
	void transact_missingUserConstraint() {
		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		// missing user
		final var existingBook = new Book("book", new Author.ByName("author"));
		final var missingUser = new User("missing user");
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.books().put(new Book("title", new Author.ByRef(missingUser)), new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
			return true;
		}));
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.userNotifications().put(missingUser, new String[]{"notification"});
			return true;
		}));
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.userBookRequests().put(new Object[]{missingUser, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));
			return true;
		}));
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.borrows().put(new Object[]{missingUser, existingBook}, new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf"));
			return true;
		}));

	}

	@Test
	void transact_missingBookConstraint() {
		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		// missing book
		final var existingUser = new User("reader");
		final var missingBook = new Book("missing book", new Author.ByName("author"), true);
		assertThrows(TransactionException.class, () -> repository.transact(tx -> {
			tx.borrows().put(new Object[]{existingUser, missingBook}, new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf"));
			return true;
		}));
	}

	@Test
	void transact_removeUserConstraint() {
		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		// remove user
		final var existingBook = new Book("book", new Author.ByName("author"));
		final var newUser = new User("new user");
		final var newUserData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");

		assertDoesNotThrow(() -> repository.transact(tx -> tx.users().put(newUser, newUserData) == null));
		assertEquals(newUserData, repository.users.get(newUser));

		final var newBookToRemove = new Book("title", new Author.ByRef(newUser));
		final var newBookToRemoveData = new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42);
		assertDoesNotThrow(() -> repository.transact(tx -> tx.books().put(newBookToRemove, newBookToRemoveData) == null));
		assertEquals(newBookToRemoveData, repository.books.get(newBookToRemove));

		final var newUserNotificationsToRemove = new String[]{"notification"};
		assertDoesNotThrow(() -> repository.transact(tx -> tx.userNotifications().put(newUser, newUserNotificationsToRemove) == null));
		assertArrayEquals(newUserNotificationsToRemove, repository.userNotifications.get(newUser));

		final var newBookRequestToRemove = new BookRequest("title", "author");
		final var newBookRequestToRemoveData = new BookRequest.Data(TimeUtil.nowZoned());
		assertDoesNotThrow(() -> repository.transact(tx -> tx.userBookRequests().put(new Object[]{newUser, newBookRequestToRemove}, newBookRequestToRemoveData) == null));
		assertEquals(newBookRequestToRemoveData, repository.userBookRequests.get(new Object[]{newUser, newBookRequestToRemove}));

		final var borrowToRemove = new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf");
		assertDoesNotThrow(() -> repository.transact(tx -> tx.borrows().put(new Object[]{newUser, existingBook}, borrowToRemove) == null));
		assertEquals(borrowToRemove, repository.borrows.get(new Object[]{newUser, existingBook}));

		assertDoesNotThrow(() -> repository.transact(tx -> tx.users().remove(newUser) != null));
		assertNull(repository.users.get(newUser));
		assertNull(repository.books.get(newBookToRemove));
		assertNull(repository.userNotifications.get(newUser));
		assertNull(repository.userBookRequests.get(new Object[]{newUser, newBookRequestToRemove}));
		assertNull(repository.borrows.get(new Object[]{newUser, existingBook}));
	}

	@Test
	void transact_removeBookConstraint() {
		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		// remove book
		final var existingUser = new User("reader");
		final var newBook = new Book("new book", new Author.ByRef(existingUser));
		final var newBookData = new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42);

		assertDoesNotThrow(() -> repository.transact(tx -> tx.books().put(newBook, newBookData) == null));
		assertEquals(newBookData, repository.books.get(newBook));

		final var newBorrowToRemove = new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf");
		assertDoesNotThrow(() -> repository.transact(tx -> tx.borrows().put(new Object[]{existingUser, newBook}, newBorrowToRemove) == null));
		assertEquals(newBorrowToRemove, repository.borrows.get(new Object[]{existingUser, newBook}));

		assertDoesNotThrow(() -> repository.transact(tx -> tx.books().remove(newBook) != null));
		assertNull(repository.books.get(newBook));
		assertNull(repository.borrows.get(new Object[]{existingUser, newBook}));
	}

	@Test
	void close() {
		assertDoesNotThrow(() -> repository.transact(RepositoryTest::populate));

		repository.close();
		assertThrows(IllegalAccessError.class, () -> repository.transact(RepositoryTest::populate));
	}
}
