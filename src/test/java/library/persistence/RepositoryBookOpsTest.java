package library.persistence;

import library.models.Author;
import library.models.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryBookOpsTest {
	private Repository repository;
	private RepositoryBookOps ops;

	@BeforeEach
	void setUp() throws IOException {
		final var file = Files.createTempFile(null, null);
		Files.deleteIfExists(file);
		final var file2 = file.toFile();
		file2.deleteOnExit();
		// Requires persistence across rollbacks
		repository = new Repository(DBMaker.fileDB(file2));
		ops = new RepositoryBookOps(repository);
	}

	@AfterEach
	void tearDown() {
		repository.close();
	}

	@Test
	void create_successful() {
		final var book = new Book("testTitle", new Author.ByName("author"));
		final var data = new Book.Data("summary", "content",
				Book.ApprovalStatus.PENDING, null, null, 42);

		// No exception means the book was persisted
		assertDoesNotThrow(() -> ops.create(book, data));
		assertEquals(data, repository.books.get(book));
	}

	@Test
	void create_duplicateThrows() {
		final var book = new Book("dupTitle", new Author.ByName("author"));
		final var data = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 1);

		assertDoesNotThrow(() -> ops.create(book, data));
		// second insertion must fail
		assertThrows(TransactionException.class,
				() -> ops.create(book, data));
	}

	@Test
	void read_all_returnsUnmodifiableMap() throws TransactionException {
		// prepare a couple of entries
		final var bookA = new Book("a", new Author.ByName("A"));
		final var dataA = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 1);
		ops.create(bookA, dataA);

		// fetch the map and verify that it contains the entry
		final var all = assertDoesNotThrow(() -> ops.read());
		assertEquals(1, all.size());
		assertTrue(all.containsKey(bookA));

		// try to modify – should throw UnsupportedOperationException
		assertThrows(
				UnsupportedOperationException.class,
				() -> all.put(new Book("b", new Author.ByName("B")), dataA),
				"Returned map must be unmodifiable");
	}

	@Test
	void read_filteredByApprovalStatus() throws TransactionException {
		final var bookPending = new Book("pending", new Author.ByName("X"));
		final var dataPending = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 5);
		ops.create(bookPending, dataPending);

		final var bookApproved = new Book("approved", new Author.ByName("Y"));
		final var dataApproved = new Book.Data("s", "c",
				Book.ApprovalStatus.APPROVED, null, null, 6);
		ops.create(bookApproved, dataApproved);

		// filter only PENDING books
		final var filtered =
				assertDoesNotThrow(() ->
						ops.read(entry -> entry.getValue().approvalStatus() == Book.ApprovalStatus.PENDING));

		assertEquals(1, filtered.size());
		assertTrue(filtered.containsKey(bookPending));
		assertFalse(filtered.containsKey(bookApproved));
	}

	@Test
	void read_byDifferentTitle_returnsEmpty() throws TransactionException {
		final var existingBook = new Book("existing", new Author.ByName("Z"));
		ops.create(existingBook, new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 1));

		// a book with the same author but different title
		final var otherBook = new Book("other", new Author.ByName("Z"));

		final var result = assertDoesNotThrow(() -> ops.read(otherBook));
		assertFalse(result.isPresent());
	}

	@Test
	void read_existing() throws TransactionException {
		final var book = new Book("readTitle", new Author.ByName("author"));
		final var data = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 1);

		ops.create(book, data);
		final var result = assertDoesNotThrow(() -> ops.read(book));
		assertTrue(result.isPresent());
		assertEquals(data, result.get());
		assertDoesNotThrow(() -> ops.readOrThrow(book));
	}

	@Test
	void read_nonExisting() {
		final var book = new Book("noSuch", new Author.ByName("author"));

		final var result = assertDoesNotThrow(() -> ops.read(book));
		assertFalse(result.isPresent());
		assertThrows(NoSuchElementException.class, () -> ops.readOrThrow(book));
	}

	@Test
	void update_existing() throws TransactionException {
		final var book = new Book("updTitle", new Author.ByName("author"));
		final var data1 = new Book.Data("s1", "c1",
				Book.ApprovalStatus.PENDING, null, null, 10);
		ops.create(book, data1);

		// Update only the approval status
		assertDoesNotThrow(() -> ops.update(book, old -> old.withApprovalStatus(Book.ApprovalStatus.APPROVED)));
		final var updated = Objects.requireNonNull(repository.books.get(book));
		assertEquals(Book.ApprovalStatus.APPROVED, updated.approvalStatus());
	}

	@Test
	void update_nonExistingThrows() {
		final var book = new Book("missing", new Author.ByName("author"));
		final var data1 = new Book.Data("s1", "c1",
				Book.ApprovalStatus.PENDING, null, null, 10);

		assertThrows(TransactionException.class,
				() -> ops.update(book, old -> old));
		assertThrows(TransactionException.class,
				() -> ops.update(book, data1, null));
	}

	@Test
	void update_withData_expectedMatches() throws TransactionException {
		final var book = new Book("matchTitle", new Author.ByName("author"));
		final var original = new Book.Data("s1", "c1",
				Book.ApprovalStatus.PENDING, null, null, 10);
		ops.create(book, original);

		// Update with new data where expected matches the current state
		final var updatedData = original.withApprovalStatus(Book.ApprovalStatus.APPROVED);
		assertDoesNotThrow(() -> ops.update(book, updatedData, original));

		final var stored = Objects.requireNonNull(repository.books.get(book));
		assertEquals(Book.ApprovalStatus.APPROVED, stored.approvalStatus());
	}

	@Test
	void update_withData_expectedMismatchThrows() throws TransactionException {
		final var book = new Book("mismatchTitle", new Author.ByName("author"));
		final var original = new Book.Data("s1", "c1",
				Book.ApprovalStatus.PENDING, null, null, 10);
		ops.create(book, original);

		// Attempt to update with an outdated expected value
		final var staleExpected = new Book.Data("sX", "cX",
				Book.ApprovalStatus.PENDING, null, null, 5);   // different fields

		final var newData = original.withApprovalStatus(Book.ApprovalStatus.APPROVED);
		assertThrows(TransactionException.class,
				() -> ops.update(book, newData, staleExpected));
	}

	@Test
	void update_withData_expectedNullWorks() throws TransactionException {
		final var book = new Book("nullExpectedTitle", new Author.ByName("author"));
		final var original = new Book.Data("s1", "c1",
				Book.ApprovalStatus.PENDING, null, null, 10);
		ops.create(book, original);

		// Update with expected == null (ignore concurrency check)
		final var newData = original.withApprovalStatus(Book.ApprovalStatus.APPROVED);
		assertDoesNotThrow(() -> ops.update(book, newData, null));

		final var stored = Objects.requireNonNull(repository.books.get(book));
		assertEquals(Book.ApprovalStatus.APPROVED, stored.approvalStatus());
	}

	@Test
	void delete_existing() throws TransactionException {
		final var book = new Book("delTitle", new Author.ByName("author"));
		final var data = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 5);
		ops.create(book, data);

		assertDoesNotThrow(() -> ops.delete(book));
		assertNull(repository.books.get(book));
	}

	@Test
	void delete_existing_expected() throws TransactionException {
		final var book = new Book("delTitle", new Author.ByName("author"));
		final var data = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 5);
		ops.create(book, data);

		assertDoesNotThrow(() -> ops.delete(book, data));
		assertNull(repository.books.get(book));
	}

	@Test
	void delete_existing_unexpected() throws TransactionException {
		final var book = new Book("delTitle", new Author.ByName("author"));
		final var data = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 5);
		ops.create(book, data);

		assertThrows(TransactionException.class, () -> ops.delete(book, data.withContent("%sc".formatted(data.content()))));
		assertNotNull(repository.books.get(book));
	}

	@Test
	void delete_nonExistingDoesNothing() {
		final var book = new Book("neverAdded", new Author.ByName("author"));

		// Exception should be thrown if the key is absent
		assertThrows(TransactionException.class, () -> ops.delete(book));
	}

	@Test
	void repository() {
		assertSame(repository, ops.repository());
	}
}
