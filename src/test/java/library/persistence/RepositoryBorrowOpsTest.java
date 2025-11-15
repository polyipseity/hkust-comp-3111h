package library.persistence;

import library.models.Author;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.utils.ByteArray;
import library.utils.Dates;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.time.Duration;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryBorrowOpsTest {
	private Repository repository;
	private RepositoryBorrowOps ops;

	@SuppressWarnings("SameReturnValue")
	private static boolean populate(@NotNull Repository.TransactData data) {
		final var reader = new User("reader");
		final var author = new User("author");
		data.users().put(reader, new User.Data(User.Role.STUDENT_STAFF, true, "reader", "reader"));
		data.users().put(author, new User.Data(User.Role.AUTHOR, true, "author", "author"));

		final var book = new Book("book", new Author.ByRef(author));
		final var book2 = new Book("book", new Author.ByName("author"));
		data.books().put(book, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, null, 42));
		data.books().put(book2, new Book.Data("summary", "content", Book.ApprovalStatus.REJECTED, null, 42));

		data.borrows().put(new Object[]{reader, book}, new Borrow(Dates.nowZoned(), Duration.ofNanos(42), new ByteArray(new byte[42])));

		return true;
	}

	@BeforeEach
	void setUp() throws TransactionException {
		repository = new Repository(DBMaker::memoryDirectDB);
		ops = new RepositoryBorrowOps(repository);

		repository.transact(RepositoryBorrowOpsTest::populate);
	}

	@AfterEach
	void tearDown() {
		try (final var _ = repository) {
			ops = null;
			repository = null;
		}
	}

	@Test
	void readExistingBorrow() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var opt = assertDoesNotThrow(() -> ops.read(user, book));
		assertTrue(opt.isPresent());
		assertNotNull(opt.get());
	}

	@Test
	void readNonExistingBorrow() {
		final var user = new User("reader");
		final var book = new Book("nonexistent", new Author.ByRef(new User("author")));
		final var opt = assertDoesNotThrow(() -> ops.read(user, book));
		assertFalse(opt.isPresent());
	}

	@Test
	void readAllBorrowsForUser() {
		final var user = new User("reader");
		final var map = assertDoesNotThrow(() -> ops.read(user));
		// reader has one borrow in populateRepository()
		assertEquals(1, map.size());
		assertTrue(map.containsKey(new Book("book", new Author.ByRef(new User("author")))));
	}

	@Test
	void readAllBorrowsForBook() {
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var map = assertDoesNotThrow(() -> ops.read(book));
		// book has one borrow (by reader)
		assertEquals(1, map.size());
		assertTrue(map.containsKey(new User("reader")));
	}

	@Test
	void createBorrowSucceeds() {
		final var user = new User("author");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var borrow = new Borrow(
				ZonedDateTime.now(),
				Duration.ofHours(1),
				new ByteArray(new byte[10])
		);

		assertDoesNotThrow(() -> ops.create(user, book, borrow));

		// verify persistence
		final var opt = ops.read(user, book);
		assertTrue(opt.isPresent());
		assertEquals(borrow, opt.get());
	}

	@Test
	void createDuplicateBorrowFails() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByName("author")); // `ByName` instead of `ByRef` is intentional
		final var borrow = new Borrow(
				ZonedDateTime.now(),
				Duration.ofHours(1),
				new ByteArray(new byte[10])
		);

		assertDoesNotThrow(() -> ops.create(user, book, borrow));
		assertThrows(TransactionException.class, () -> ops.create(user, book, borrow));
	}

	@Test
	void updateBorrowSucceeds() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));

		// change duration by 1 hour
		assertDoesNotThrow(() -> ops.update(user, book, old -> old.withDuration(Duration.ofDays(42))));

		final var opt = ops.read(user, book);
		assertTrue(opt.isPresent());
		assertEquals(Duration.ofDays(42), opt.get().duration());
	}

	@Test
	void updateNonExistingBorrowFails() {
		final var user = new User("reader");
		final var book = new Book("missing", new Author.ByRef(new User("author")));

		assertThrows(TransactionException.class,
				() -> ops.update(user, book, old -> old));
	}

	@Test
	void deleteBorrowSucceeds() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));

		assertDoesNotThrow(() -> ops.delete(user, book));
		assertFalse(ops.read(user, book).isPresent());
	}

	@Test
	void deleteNonExistingBorrow() {
		final var user = new User("reader");
		final var book = new Book("missing", new Author.ByRef(new User("author")));

		// should throw, nothing to remove
		assertThrows(TransactionException.class, () -> ops.delete(user, book));
	}

	@Test
	void deleteAllBorrowsForUser() {
		final var user = new User("reader");

		assertDoesNotThrow(() -> ops.delete(user));
		assertTrue(ops.read(user).isEmpty());
	}

	@Test
	void deleteAllBorrowsForNonExistingUser() {
		final var user = new User("missing");

		// should throw, nothing to remove
		assertThrows(TransactionException.class, () -> ops.delete(user));
	}

	@Test
	void repository() {
		assertSame(repository, ops.repository());
	}
}
