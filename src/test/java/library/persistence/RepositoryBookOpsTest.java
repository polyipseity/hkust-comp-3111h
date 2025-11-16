package library.persistence;

import library.models.Author;
import library.models.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryBookOpsTest {
	private Repository repository;
	private RepositoryBookOps ops;

	@BeforeEach
	void setUp() {
		// Create a fresh in‑memory DB for every test
		repository = new Repository(DBMaker::memoryDirectDB);
		ops = new RepositoryBookOps(repository);
	}

	@AfterEach
	void tearDown() {
		try (final var _ = repository) {
			ops = null;
			repository = null;
		}
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
	void read_existing() throws TransactionException {
		final var book = new Book("readTitle", new Author.ByName("author"));
		final var data = new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 1);

		ops.create(book, data);
		final var result = assertDoesNotThrow(() -> ops.read(book));
		assertTrue(result.isPresent());
		assertEquals(data, result.get());
	}

	@Test
	void read_nonExisting() {
		final var book = new Book("noSuch", new Author.ByName("author"));

		final var result = assertDoesNotThrow(() -> ops.read(book));
		assertFalse(result.isPresent());
	}

	@Test
	void read_byAuthor() throws TransactionException {
		final var authorA = new Author.ByName("authorA");
		final var authorB = new Author.ByName("authorB");

		// books for authorA
		ops.create(new Book("a1", authorA), new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 10));
		ops.create(new Book("a2", authorA), new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 20));

		// one book for authorB
		ops.create(new Book("b1", authorB), new Book.Data("s", "c",
				Book.ApprovalStatus.PENDING, null, null, 30));

		final var result = assertDoesNotThrow(() -> ops.read(authorA));

		assertEquals(2, result.size(), "Only two books should be returned");
		result.keySet().forEach(b -> assertEquals(authorA, b.author(),
				"Returned book must belong to the queried author"));
	}

	@Test
	void read_byAuthor_withNoBooks() {
		var author = new Author.ByName("lonely");
		final var result = assertDoesNotThrow(() -> ops.read(author));

		assertTrue(result.isEmpty(), "No books should be returned for an unknown author");
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

		assertThrows(TransactionException.class,
				() -> ops.update(book, old -> old));
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
