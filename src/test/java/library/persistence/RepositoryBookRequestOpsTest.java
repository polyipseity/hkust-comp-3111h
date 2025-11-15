package library.persistence;

import library.models.BookRequest;
import library.models.User;
import library.utils.Dates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryBookRequestOpsTest {
	private Repository repository;
	private RepositoryBookRequestOps ops;

	@BeforeEach
	void setUp() {
		// Fresh in‑memory DB for every test
		repository = new Repository(DBMaker::memoryDirectDB);
		ops = new RepositoryBookRequestOps(repository);
	}

	@AfterEach
	void tearDown() {
		try (final var _ = repository) {
			ops = null;
			repository = null;
		}
	}

	@Test
	void read_singleExisting() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		// Persist the request first
		final var req = new BookRequest("title", "author");
		final var data = new BookRequest.Data(Dates.nowZoned());
		ops.create(user, req, data);

		final var result = assertDoesNotThrow(() -> ops.read(user, req));
		assertTrue(result.isPresent());
		assertEquals(data, result.get());
	}

	@Test
	void read_singleNonExisting() {
		final var user = new User("u1");
		final var req = new BookRequest("t", "a");
		assertFalse(assertDoesNotThrow(() -> ops.read(user, req)).isPresent());
	}

	@Test
	void read_allForUser() throws TransactionException {
		final var user = new User("reader");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var r1 = new BookRequest("title1", "author1");
		final var r2 = new BookRequest("title2", "author2");
		ops.create(user, r1, new BookRequest.Data(Dates.nowZoned()));
		ops.create(user, r2, new BookRequest.Data(Dates.nowZoned()));

		final var all = assertDoesNotThrow(() -> ops.read(user));
		assertEquals(2, all.size());
		assertTrue(all.containsKey(r1));
		assertTrue(all.containsKey(r2));
	}

	@Test
	void read_allEmpty() throws TransactionException {
		final var user = new User("reader");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		assertTrue(assertDoesNotThrow(() -> ops.read(user)).isEmpty());
	}

	@Test
	void create_successful() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		final var data = new BookRequest.Data(Dates.nowZoned());

		assertDoesNotThrow(() -> ops.create(user, req, data));
		assertEquals(data, repository.userBookRequests.get(new Object[]{user, req}));
	}

	@Test
	void create_duplicateThrows() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		assertDoesNotThrow(() -> ops.create(user, req, new BookRequest.Data(Dates.nowZoned())));

		assertThrows(TransactionException.class,
				() -> ops.create(user, req, new BookRequest.Data(Dates.nowZoned())));
	}

	@Test
	void update_existing() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		final var oldData = new BookRequest.Data(Dates.nowZoned());

		ops.create(user, req, oldData);
		assertDoesNotThrow(() -> ops.update(user, req, d -> d.withRequestDate(d.requestDate().plusDays(1))));

		final var updated = Objects.requireNonNull(repository.userBookRequests.get(new Object[]{user, req}));
		assertNotEquals(oldData, updated);
		assertEquals(oldData.requestDate().plusDays(1), updated.requestDate());
	}

	@Test
	void update_nonExistingThrows() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		assertThrows(TransactionException.class,
				() -> ops.update(user, req, d -> d));
	}

	@Test
	void delete_singleExisting() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		ops.create(user, req, new BookRequest.Data(Dates.nowZoned()));

		assertDoesNotThrow(() -> ops.delete(user, req));
		assertNull(repository.userBookRequests.get(new Object[]{user, req}));
	}

	@Test
	void delete_singleNonExistingDoesNothing() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		assertThrows(TransactionException.class, () -> ops.delete(user, req));
	}

	@Test
	void delete_allForUser() throws TransactionException {
		final var user = new User("reader");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var r1 = new BookRequest("t1", "a1");
		final var r2 = new BookRequest("t2", "a2");
		ops.create(user, r1, new BookRequest.Data(Dates.nowZoned()));
		ops.create(user, r2, new BookRequest.Data(Dates.nowZoned()));

		assertDoesNotThrow(() -> ops.delete(user)); // delete all requests of the user
		assertTrue(repository.userBookRequests.prefixSubMap(new Object[]{user}).isEmpty());
	}

	@Test
	void delete_allForNonExistingUserDoesNothing() {
		final var user = new User("nonExistent");
		assertThrows(TransactionException.class, () -> ops.delete(user));
	}

	@Test
	void repository() {
		assertSame(repository, ops.repository());
	}
}
