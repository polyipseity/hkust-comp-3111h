package library.persistence;

import library.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryUserOpsTest {

	private Repository repository;
	private RepositoryUserOps ops;

	@BeforeEach
	void setUp() {
		// create an in-memory MapDB instance for each test
		repository = new Repository(DBMaker::memoryDirectDB);
		ops = new RepositoryUserOps(repository);
	}

	@AfterEach
	void tearDown() {
		try (final var _ = repository) {
			ops = null;
			repository = null;
		}
	}

	@Test
	void create() {
		final var user = new User("john");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "John Doe");

		// initially absent
		assertNull(repository.users.get(user));
		assertDoesNotThrow(() -> ops.create(user, data));

		// after create it is present in the underlying map
		assertEquals(data, repository.users.get(user));
	}

	@Test
	void create_duplicate() {
		final var user = new User("john");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "John Doe");

		assertDoesNotThrow(() -> ops.create(user, data));
		// duplicate create should throw an exception
		assertThrows(TransactionException.class, () -> ops.create(user, data));
	}

	@Test
	void read() throws TransactionException {
		final var user = new User("alice");
		final var data = new User.Data(User.Role.AUTHOR, true, "pass", "Alice");

		ops.create(user, data);

		final var opt = assertDoesNotThrow(() -> ops.read(user));
		assertTrue(opt.isPresent());
		assertEquals(data, opt.get());
	}

	@Test
	void read_missing() {
		final var unknown = new User("unknown");
		assertFalse(ops.read(unknown).isPresent());
	}

	@Test
	void update() throws TransactionException {
		final var user = new User("bob");
		final var data = new User.Data(User.Role.LIBRARIAN, true, "secret", "Bob");

		ops.create(user, data);

		// change the password and full name via a callback
		assertDoesNotThrow(() -> ops.update(user, d -> d.withPassword("newSecret").withFullName("Bobby")));

		final var updated = repository.users.get(user);
		assertNotNull(updated);
		assertEquals("newSecret", updated.password());
		assertEquals("Bobby", updated.fullName());
	}

	@Test
	void update_missing() {
		// update non‑existing user should throw TransactionException
		final var unknown = new User("unknown");
		assertThrows(TransactionException.class,
				() -> ops.update(unknown, d -> d));
	}

	@Test
	void delete() throws TransactionException {
		final var user = new User("eve");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "Eve");

		ops.create(user, data);
		assertEquals(data, repository.users.get(user));

		assertDoesNotThrow(() -> ops.delete(user));

		// after deletion, it is no longer present
		assertNull(repository.users.get(user));
	}

	@Test
	void delete_missing() {
		final var user = new User("eve");
		assertThrows(TransactionException.class, () -> ops.delete(user));
	}

	@Test
	void repository() {
		assertSame(repository, ops.repository());
	}
}
