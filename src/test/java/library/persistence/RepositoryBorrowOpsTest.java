package library.persistence;

import library.models.Author;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.utils.TimeUtil;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryBorrowOpsTest {
	private Repository repository;
	private RepositoryBorrowOps ops;

	@SuppressWarnings("SameReturnValue")
	private static boolean populate(@NotNull Repository.TransactData data) {
		final var reader = new User("reader");
		final var reader2 = new User("reader2");
		final var author = new User("author");
		data.users().put(reader, new User.Data(User.Role.STUDENT_STAFF, true, "reader", "reader"));
		data.users().put(reader2, new User.Data(User.Role.STUDENT_STAFF, true, "reader2", "reader2"));
		data.users().put(author, new User.Data(User.Role.AUTHOR, true, "author", "author"));

		final var book = new Book("book", new Author.ByRef(author));
		final var book2 = new Book("book", new Author.ByName("author"));
		data.books().put(book, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
		data.books().put(book2, new Book.Data("summary", "content", Book.ApprovalStatus.REJECTED, null, null, 42));

		data.borrows().put(new Object[]{reader, book}, new Borrow(TimeUtil.nowZoned(), Duration.ofNanos(42), "test.pdf"));

		return true;
	}

	@BeforeEach
	void setUp() throws IOException, TransactionException {
		final var file = Files.createTempFile(null, null);
		Files.deleteIfExists(file);
		final var file2 = file.toFile();
		file2.deleteOnExit();
		// Requires persistence across rollbacks
		repository = new Repository(DBMaker.fileDB(file2));
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
	void read_allBorrows_unfiltered() throws TransactionException {
		final var reader2 = new User("reader2");
		final var book2 = new Book("book2", new Author.ByName("author"));
		repository.bookOps.create(book2,
				new Book.Data("s", "c",
						Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 10));

		ops.create(reader2, book2, new Borrow(TimeUtil.nowZoned(),
				Duration.ofDays(7), "test.pdf"));

		// now there should be two borrows in the repo
		final var all = assertDoesNotThrow(() -> ops.read());
		assertEquals(2, all.size(), "Both user‑book pairs must be returned");

		// check that the map is immutable
		assertThrows(
				UnsupportedOperationException.class,
				() -> all.put(new Tuple2<>(reader2, book2),
						new Borrow(TimeUtil.nowZoned(),
								Duration.ofDays(3), "test.pdf")),
				"Returned map must be unmodifiable");
	}

	@Test
	void read_filteredByDuration() throws TransactionException {
		// the fixture created in populate() has a duration of 42 nanoseconds.
		// create another borrow with a longer duration so that we can filter
		final var reader = new User("reader");
		final var bookLong = new Book("longBook", new Author.ByName("author"));
		repository.bookOps.create(bookLong,
				new Book.Data("s", "c",
						Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 5));

		ops.create(reader, bookLong, new Borrow(TimeUtil.nowZoned(),
				Duration.ofHours(1), "test.pdf"));

		// keep only borrows that last longer than 42 nanoseconds
		final var filtered = assertDoesNotThrow(() ->
				ops.read(entry -> entry.getValue().duration().toNanos() > 42));

		// we should have exactly the long borrow in the result
		assertEquals(1, filtered.size());
		assertTrue(filtered.containsKey(new Tuple2<>(reader, bookLong)));
		assertFalse(filtered.containsKey(new Tuple2<>(reader,
				new Book("book", new Author.ByRef(new User("author"))))));
	}

	@Test
	void read_existingBorrow() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var opt = assertDoesNotThrow(() -> ops.read(user, book));
		assertTrue(opt.isPresent());
		assertNotNull(opt.get());
		assertDoesNotThrow(() -> ops.readOrThrow(user, book));
	}

	@Test
	void read_nonExistingBorrow() {
		final var user = new User("reader");
		final var book = new Book("nonexistent", new Author.ByRef(new User("author")));
		final var opt = assertDoesNotThrow(() -> ops.read(user, book));
		assertFalse(opt.isPresent());
		assertThrows(NoSuchElementException.class, () -> ops.readOrThrow(user, book));
	}

	@Test
	void read_byUser_allBorrows() {
		final var user = new User("reader");
		final var map = assertDoesNotThrow(() -> ops.read(user));
		// reader has one borrow in populateRepository()
		assertEquals(1, map.size());
		assertTrue(map.containsKey(new Book("book", new Author.ByRef(new User("author")))));
	}

	@Test
	void read_byBook_AllBorrows() {
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var map = assertDoesNotThrow(() -> ops.read(book));
		// book has one borrow (by reader)
		assertEquals(1, map.size());
		assertTrue(map.containsKey(new User("reader")));
	}

	@Test
	void create_borrowSucceeds() {
		final var user = new User("author");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var borrow = new Borrow(
				TimeUtil.nowZoned(),
				Duration.ofHours(1),
				"test.pdf"
		);

		assertDoesNotThrow(() -> ops.create(user, book, borrow));

		// verify persistence
		final var opt = ops.read(user, book);
		assertTrue(opt.isPresent());
		assertEquals(borrow, opt.get());
	}

	@Test
	void create_duplicateBorrowFails() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByName("author")); // `ByName` instead of `ByRef` is intentional
		final var borrow = new Borrow(
				TimeUtil.nowZoned(),
				Duration.ofHours(1),
				"test.pdf"
		);

		assertDoesNotThrow(() -> ops.create(user, book, borrow));
		assertThrows(TransactionException.class, () -> ops.create(user, book, borrow));
	}

	@Test
	void update_borrowSucceeds() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));

		// change duration by 1 hour
		assertDoesNotThrow(() -> ops.update(user, book, old -> old.withDuration(Duration.ofDays(42))));

		final var opt = ops.read(user, book);
		assertTrue(opt.isPresent());
		assertEquals(Duration.ofDays(42), opt.get().duration());
	}

	@Test
	void update_nonExistingBorrowFails() {
		final var user = new User("reader2");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var data = new Borrow(TimeUtil.nowZoned().plusDays(1), Duration.ofDays(42), "");

		assertThrows(TransactionException.class,
				() -> ops.update(user, book, old -> old));
		assertThrows(TransactionException.class,
				() -> ops.update(user, book, data, null));
	}

	@Test
	void update_borrowWithData_expectedMatches() throws TransactionException {
		final var user = new User("reader2");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var data = new Borrow(TimeUtil.nowZoned().plusDays(1), Duration.ofDays(42), "");
		ops.create(user, book, data);

		assertDoesNotThrow(() ->
				ops.update(user, book, data.withDuration(data.duration().plusDays(1)), data));

		final var opt = ops.read(user, book);
		assertTrue(opt.isPresent());
		assertEquals(data.duration().plusDays(1), opt.get().duration());
	}

	@Test
	void update_borrowWithData_expectedMismatchThrows() throws TransactionException {
		final var user = new User("reader2");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var data = new Borrow(TimeUtil.nowZoned().plusDays(1), Duration.ofDays(42), "");
		ops.create(user, book, data);

		assertThrows(TransactionException.class,
				() -> ops.update(user, book, data.withDuration(data.duration().plusDays(1)), data.withBorrowDate(data.borrowDate().plusDays(1))));
	}

	@Test
	void update_borrowWithData_expectedNullWorks() throws TransactionException {
		final var user = new User("reader2");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var data = new Borrow(TimeUtil.nowZoned().plusDays(1), Duration.ofDays(42), "");
		ops.create(user, book, data);

		assertDoesNotThrow(() ->
				ops.update(user, book, data.withDuration(data.duration().plusDays(1)), null));

		final var opt = ops.read(user, book);
		assertTrue(opt.isPresent());
		assertEquals(data.duration().plusDays(1), opt.get().duration());
	}

	@Test
	void delete_singleExisting() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));

		assertDoesNotThrow(() -> ops.delete(user, book));
		assertFalse(ops.read(user, book).isPresent());
	}

	@Test
	void delete_singleExisting_expected() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var data = ops.readOrThrow(user, book);

		assertDoesNotThrow(() -> ops.delete(user, book, data));
		assertFalse(ops.read(user, book).isPresent());
	}

	@Test
	void delete_singleExisting_unexpected() {
		final var user = new User("reader");
		final var book = new Book("book", new Author.ByRef(new User("author")));
		final var data = ops.readOrThrow(user, book);

		assertThrows(TransactionException.class, () -> ops.delete(user, book, data.withDuration(data.duration().plusDays(1))));
		assertTrue(ops.read(user, book).isPresent());
	}

	@Test
	void delete_singleNonExisting() {
		final var user = new User("reader");
		final var book = new Book("missing", new Author.ByRef(new User("author")));

		// should throw, nothing to remove
		assertThrows(TransactionException.class, () -> ops.delete(user, book));
	}

	@Test
	void delete_allBorrowsForUserSucceeds() {
		final var user = new User("reader");

		assertDoesNotThrow(() -> ops.delete(user));
		assertTrue(ops.read(user).isEmpty());
	}

	@Test
	void delete_allBorrowsForNonExistingUserFails() {
		final var user = new User("missing");

		// should throw, nothing to remove
		assertThrows(TransactionException.class, () -> ops.delete(user));
	}

	@Test
	void repository() {
		assertSame(repository, ops.repository());
	}
}
