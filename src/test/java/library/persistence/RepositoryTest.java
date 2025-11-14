package library.persistence;

import library.models.Author;
import library.models.Book;
import library.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for the CRUD helpers in {@link Repository}.
 */
class RepositoryTest {
	private Repository service;

	@BeforeEach
	void setUp() {
		service = new Repository(DBMaker::memoryDirectDB);
	}

	@AfterEach
	void tearDown() {
		service.close();
	}

	@Test
	void userCreateReadUpdateDelete() {
		var user = new User("alice");
		var data = new User.Data(User.Role.STUDENT_STAFF, true, "alice_pwd", "Alice Smith");

		// ---- create ---------------------------------------------------------
		assertDoesNotThrow(() -> service.userOps.create(user, data), "userOps.create should not throw when the key is new");

		// Trying to insert the same user again must fail
		assertThrows(TransactionException.class, () -> service.userOps.create(user, data), "userOps.create should throw for duplicate keys");

		// ---- read -----------------------------------------------------------
		Optional<User.Data> opt = service.userOps.read(user);
		assertTrue(opt.isPresent(), "userOps.read should find the inserted key");
		assertEquals(data, opt.get(), "Returned data must match what was stored after considering constraints");

		// ---- update ---------------------------------------------------------
		Function<User.Data, User.Data> deactivateAccount = d -> d.withActive(false);
		assertDoesNotThrow(() -> service.userOps.update(user, deactivateAccount));
		assertThrows(TransactionException.class, () -> service.userOps.update(user, _ -> {
			throw new RuntimeException();
		}));

		var afterUpdate = service.userOps.read(user).orElseThrow();
		assertFalse(afterUpdate.active(), "The callback should have deactivated account");

		// ---- delete ---------------------------------------------------------
		assertDoesNotThrow(() -> service.userOps.delete(user), "deleteUser must not throw when the key existed");
		assertThrows(TransactionException.class, () -> service.userOps.delete(user), "deleteUser must throw for a non‑existent key");
		assertThrows(TransactionException.class, () -> service.userOps.update(user, deactivateAccount));
	}

	@Test
	void bookCreateReadUpdateDelete() {
		var book = new Book("Clean Code", new Author.ByName("Robert C. Martin"));
		var data = new Book.Data("Good book!", "Good book content...", Book.ApprovalStatus.APPROVED, null, 5);
		var book2 = new Book("Dirty Code", new Author.ByRef(new User("alice")), true);
		var data2 = new Book.Data("Bad book!", "Bad book content...", Book.ApprovalStatus.PENDING, book, 0);

		// ---- create ---------------------------------------------------------
		assertDoesNotThrow(() -> service.bookOps.create(book, data), "bookOps.create should not throw when the key is new");
		// Trying to insert the same book again must fail
		assertThrows(TransactionException.class, () -> service.bookOps.create(book, data), "bookOps.create should throw for duplicate keys");
		assertDoesNotThrow(() -> service.bookOps.create(book2, data2), "bookOps.create should not throw when the key is new");

		// ---- read -----------------------------------------------------------
		Optional<Book.Data> opt = service.bookOps.read(book);
		assertTrue(opt.isPresent(), "bookOps.read should find the inserted key");
		assertEquals(data.withOriginalOrModified(book2), opt.get(), "Returned data must match what was stored after considering constraints");
		Optional<Book.Data> opt2 = service.bookOps.read(book2);
		assertTrue(opt2.isPresent(), "bookOps.read should find the inserted key");
		assertEquals(data2, opt2.get(), "Returned data must match what was stored after considering constraints");

		// ---- update ---------------------------------------------------------
		Function<Book.Data, Book.Data> addTimesBorrowed = d -> d.withTimesBorrowed(d.timesBorrowed() + 3);
		assertDoesNotThrow(() -> service.bookOps.update(book, addTimesBorrowed));
		assertThrows(TransactionException.class, () -> service.bookOps.update(book, _ -> {
			throw new RuntimeException();
		}));

		var afterUpdate = service.bookOps.read(book).orElseThrow();
		assertEquals(8, afterUpdate.timesBorrowed(), "The callback should have increased times borrowed to 3");

		// ---- delete ---------------------------------------------------------
		assertDoesNotThrow(() -> service.bookOps.delete(book), "deleteBook must not throw when the key existed");
		assertThrows(TransactionException.class, () -> service.bookOps.delete(book), "deleteBook must throw for a non‑existent key");
		assertThrows(TransactionException.class, () -> service.bookOps.update(book, addTimesBorrowed));
	}
}
