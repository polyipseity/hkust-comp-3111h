package library.persistence;

import library.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryUserOpsTest {
	private Repository repository;
	private RepositoryUserOps ops;

	@BeforeEach
	void setUp() throws IOException {
		final var file = Files.createTempFile(null, null);
		Files.deleteIfExists(file);
		final var file2 = file.toFile();
		file2.deleteOnExit();
		// Requires persistence across rollbacks
		repository = new Repository(DBMaker.fileDB(file2));
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
	void read_allReturnsUnmodifiableMap() {
		final var user = new User("john");
		final var user2 = new User("charlie");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "John Doe");
		final var data2 = new User.Data(User.Role.AUTHOR, true, "pwd", "Charlie");

		// initially absent
		assertNull(repository.users.get(user));
		assertNull(repository.users.get(user2));
		assertDoesNotThrow(() -> ops.create(user, data));
		assertDoesNotThrow(() -> ops.create(user2, data2));
		// after create it is present in the underlying map
		assertEquals(data, repository.users.get(user));
		assertEquals(data2, repository.users.get(user2));

		final var map = assertDoesNotThrow(() -> ops.read());          // no filter
		// verify that the map is indeed unmodifiable
		assertThrows(UnsupportedOperationException.class,
				() -> map.put(new User("charlie"),
						new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "Charlie")));
	}

	@Test
	void read_filterMatchesAll() {
		final var user = new User("john");
		final var user2 = new User("charlie");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "John Doe");
		final var data2 = new User.Data(User.Role.AUTHOR, true, "pwd", "Charlie");

		// initially absent
		assertNull(repository.users.get(user));
		assertNull(repository.users.get(user2));
		assertDoesNotThrow(() -> ops.create(user, data));
		assertDoesNotThrow(() -> ops.create(user2, data2));
		// after create it is present in the underlying map
		assertEquals(data, repository.users.get(user));
		assertEquals(data2, repository.users.get(user2));

		final var map = assertDoesNotThrow(() -> ops.read(_ -> true));
		assertEquals(2, map.size());
	}

	@Test
	void read_filterMatchesNone() {
		final var user = new User("john");
		final var user2 = new User("charlie");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "John Doe");
		final var data2 = new User.Data(User.Role.AUTHOR, true, "pwd", "Charlie");

		// initially absent
		assertNull(repository.users.get(user));
		assertNull(repository.users.get(user2));
		assertDoesNotThrow(() -> ops.create(user, data));
		assertDoesNotThrow(() -> ops.create(user2, data2));
		// after create it is present in the underlying map
		assertEquals(data, repository.users.get(user));
		assertEquals(data2, repository.users.get(user2));

		final var map = assertDoesNotThrow(() -> ops.read(e -> e.getKey().username().equals("nonexistent")));
		assertTrue(map.isEmpty());
	}

	@Test
	void read_filterMatchesSome() {
		final var user = new User("john");
		final var user2 = new User("charlie");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "John Doe");
		final var data2 = new User.Data(User.Role.AUTHOR, true, "pwd", "Charlie");

		// initially absent
		assertNull(repository.users.get(user));
		assertNull(repository.users.get(user2));
		assertDoesNotThrow(() -> ops.create(user, data));
		assertDoesNotThrow(() -> ops.create(user2, data2));
		// after create it is present in the underlying map
		assertEquals(data, repository.users.get(user));
		assertEquals(data2, repository.users.get(user2));

		final var map = assertDoesNotThrow(() -> ops.read(e -> e.getValue().role() == User.Role.AUTHOR));
		assertEquals(1, map.size());
		assertTrue(map.containsKey(new User("charlie")));
	}


	@Test
	void read_existing() throws TransactionException {
		final var user = new User("alice");
		final var data = new User.Data(User.Role.AUTHOR, true, "pass", "Alice");

		ops.create(user, data);

		final var opt = assertDoesNotThrow(() -> ops.read(user));
		assertTrue(opt.isPresent());
		assertEquals(data, opt.get());
		assertDoesNotThrow(() -> ops.readOrThrow(user));
	}

	@Test
	void read_missing() {
		final var unknown = new User("unknown");
		assertFalse(ops.read(unknown).isPresent());
		assertThrows(NoSuchElementException.class, () -> ops.readOrThrow(unknown));
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
	void delete_existing() throws TransactionException {
		final var user = new User("eve");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "Eve");

		ops.create(user, data);
		assertEquals(data, repository.users.get(user));

		assertDoesNotThrow(() -> ops.delete(user));

		// after deletion, it is no longer present
		assertNull(repository.users.get(user));
	}

	@Test
	void delete_existing_expected() throws TransactionException {
		final var user = new User("eve");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "Eve");

		ops.create(user, data);
		assertEquals(data, repository.users.get(user));

		assertDoesNotThrow(() -> ops.delete(user, data));

		// after deletion, it is no longer present
		assertNull(repository.users.get(user));
	}

	@Test
	void delete_existing_unexpected() throws TransactionException {
		final var user = new User("eve");
		final var data = new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "Eve");

		ops.create(user, data);
		assertEquals(data, repository.users.get(user));

		assertThrows(TransactionException.class, () -> ops.delete(user, data.withActive(!data.active())));
		assertNotNull(repository.users.get(user));
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
