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
		assertDoesNotThrow(() -> service.createUser(user, data), "createUser should not throw when the key is new");

		// Trying to insert the same user again must fail
		assertThrows(Repository.TransactionException.class, () -> service.createUser(user, data), "createUser should throw for duplicate keys");

		// ---- read -----------------------------------------------------------
		Optional<User.Data> opt = service.readUser(user);
		assertTrue(opt.isPresent(), "readUser should find the inserted key");
		assertEquals(data, opt.get(), "Returned data must match what was stored after considering constraints");

		// ---- update ---------------------------------------------------------
		Function<User.Data, User.Data> deactivateAccount = d -> d.withActive(false);
		assertDoesNotThrow(() -> service.updateUser(user, deactivateAccount));
		assertThrows(Repository.TransactionException.class, () -> service.updateUser(user, _ -> {
			throw new RuntimeException();
		}));

		var afterUpdate = service.readUser(user).orElseThrow();
		assertFalse(afterUpdate.active(), "The callback should have deactivated account");

		// ---- delete ---------------------------------------------------------
		assertDoesNotThrow(() -> service.deleteUser(user), "deleteUser must not throw when the key existed");
		assertThrows(Repository.TransactionException.class, () -> service.deleteUser(user), "deleteUser must throw for a non‑existent key");
		assertThrows(Repository.TransactionException.class, () -> service.updateUser(user, deactivateAccount));
	}

	@Test
	void bookCreateReadUpdateDelete() {
		var book = new Book("Clean Code", new Author.ByName("Robert C. Martin"));
		var data = new Book.Data("Good book!", "...", Book.ApprovalStatus.APPROVED, null, 5);
		var book2 = new Book("Dirty Code", new Author.ByRef(new User("alice")), true);
		var data2 = new Book.Data("Bad book!", "...", Book.ApprovalStatus.PENDING, book, 0);

		// ---- create ---------------------------------------------------------
		assertDoesNotThrow(() -> service.createBook(book, data), "createBook should not throw when the key is new");
		// Trying to insert the same book again must fail
		assertThrows(Repository.TransactionException.class, () -> service.createBook(book, data), "createBook should throw for duplicate keys");
		assertDoesNotThrow(() -> service.createBook(book2, data2), "createBook should not throw when the key is new");

		// ---- read -----------------------------------------------------------
		Optional<Book.Data> opt = service.readBook(book);
		assertTrue(opt.isPresent(), "readBook should find the inserted key");
		assertEquals(data.withOriginalOrModified(book2), opt.get(), "Returned data must match what was stored after considering constraints");
		Optional<Book.Data> opt2 = service.readBook(book2);
		assertTrue(opt2.isPresent(), "readBook should find the inserted key");
		assertEquals(data2, opt2.get(), "Returned data must match what was stored after considering constraints");

		// ---- update ---------------------------------------------------------
		Function<Book.Data, Book.Data> addTimesBorrowed = d -> d.withTimesBorrowed(d.timesBorrowed() + 3);
		assertDoesNotThrow(() -> service.updateBook(book, addTimesBorrowed));
		assertThrows(Repository.TransactionException.class, () -> service.updateBook(book, _ -> {
			throw new RuntimeException();
		}));

		var afterUpdate = service.readBook(book).orElseThrow();
		assertEquals(8, afterUpdate.timesBorrowed(), "The callback should have increased times borrowed to 3");

		// ---- delete ---------------------------------------------------------
		assertDoesNotThrow(() -> service.deleteBook(book), "deleteBook must not throw when the key existed");
		assertThrows(Repository.TransactionException.class, () -> service.deleteBook(book), "deleteBook must throw for a non‑existent key");
		assertThrows(Repository.TransactionException.class, () -> service.updateBook(book, addTimesBorrowed));
	}
}
