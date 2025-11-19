package library.persistence;

import library.models.BookRequest;
import library.models.User;
import library.utils.TimeUtil;
import library.utils.Tuple2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.util.NoSuchElementException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryUserBookRequestOpsTest {
	private Repository repository;
	private RepositoryUserBookRequestOps ops;

	@BeforeEach
	void setUp() {
		// Fresh in‑memory DB for every test
		repository = new Repository(DBMaker.memoryDirectDB());
		ops = new RepositoryUserBookRequestOps(repository);
	}

	@AfterEach
	void tearDown() {
		try (final var _ = repository) {
			ops = null;
			repository = null;
		}
	}

	@Test
	void create_successful() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		final var data = new BookRequest.Data(TimeUtil.nowZoned());

		assertDoesNotThrow(() -> ops.create(user, req, data));
		assertEquals(data, repository.userBookRequests.get(new Object[]{user, req}));
	}

	@Test
	void create_duplicateThrows() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		assertDoesNotThrow(() -> ops.create(user, req, new BookRequest.Data(TimeUtil.nowZoned())));

		assertThrows(TransactionException.class,
				() -> ops.create(user, req, new BookRequest.Data(TimeUtil.nowZoned())));
	}

	@Test
	void read_allEntries_unfiltered() throws TransactionException {
		// set up two different users with a single request each
		final var uA = new User("alice");
		repository.userOps.create(uA, new User.Data(User.Role.STUDENT_STAFF, true,
				"pw", "Alice"));

		final var rA = new BookRequest("a1", "a2");
		ops.create(uA, rA, new BookRequest.Data(TimeUtil.nowZoned()));

		final var uB = new User("bob");
		repository.userOps.create(uB, new User.Data(User.Role.STUDENT_STAFF, true,
				"pw", "Bob"));

		final var rB = new BookRequest("b1", "b2");
		ops.create(uB, rB, new BookRequest.Data(TimeUtil.nowZoned()));

		// fetch all and verify that both are present
		final var all = assertDoesNotThrow(() -> ops.read());
		assertEquals(2, all.size(), "Both user‑request pairs should be returned");

		// sanity check that the keys really contain the expected tuple
		assertTrue(all.containsKey(new Tuple2<>(uA, rA)));
		assertTrue(all.containsKey(new Tuple2<>(uB, rB)));

		// and that the map is unmodifiable
		assertThrows(
				UnsupportedOperationException.class,
				() -> all.put(new Tuple2<>(new User("c"), new BookRequest("x", "y")),
						new BookRequest.Data(TimeUtil.nowZoned())),
				"Returned map must be immutable");
	}

	@Test
	void read_filteredByTitle() throws TransactionException {
		final var u = new User("filterUser");
		repository.userOps.create(u, new User.Data(User.Role.STUDENT_STAFF, true,
				"pw", "Filter"));

		// two requests with different titles
		final var r1 = new BookRequest("unique-title", "authorX");
		ops.create(u, r1, new BookRequest.Data(TimeUtil.nowZoned()));

		final var r2 = new BookRequest("common", "authorY");
		ops.create(u, r2, new BookRequest.Data(TimeUtil.nowZoned()));

		// keep only the one whose title contains “unique”
		final var filtered = assertDoesNotThrow(() ->
				ops.read(entry -> "unique-title".equals(entry.getKey()._2().title())));

		assertEquals(1, filtered.size());
		assertTrue(filtered.containsKey(new Tuple2<>(u, r1)));
		assertFalse(filtered.containsKey(new Tuple2<>(u, r2)));
	}

	@Test
	void read_byUser_whenNoRequests() throws TransactionException {
		final var lonely = new User("lonely");
		repository.userOps.create(lonely,
				new User.Data(User.Role.STUDENT_STAFF, true, "pw", "Lonely"));

		// no requests yet – should return an empty map
		final var result = assertDoesNotThrow(() -> ops.read(lonely));
		assertTrue(result.isEmpty(), "A user with no requests must get an empty map");
	}


	@Test
	void read_byUser_returnsUnmodifiableMap() throws TransactionException {
		final var u = new User("immutable");
		repository.userOps.create(u,
				new User.Data(User.Role.STUDENT_STAFF, true, "pw", "Immutable"));

		final var req = new BookRequest("i1", "a1");
		ops.create(u, req, new BookRequest.Data(TimeUtil.nowZoned()));

		final var map = assertDoesNotThrow(() -> ops.read(u));
		assertFalse(map.isEmpty());

		// attempt to modify – should fail
		assertThrows(
				UnsupportedOperationException.class,
				() -> map.put(req, new BookRequest.Data(TimeUtil.nowZoned())),
				"Map returned by read(User) must be immutable");
	}

	@Test
	void read_singleExisting() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		// Persist the request first
		final var req = new BookRequest("title", "author");
		final var data = new BookRequest.Data(TimeUtil.nowZoned());
		ops.create(user, req, data);

		final var result = assertDoesNotThrow(() -> ops.read(user, req));
		assertTrue(result.isPresent());
		assertEquals(data, result.get());
		assertDoesNotThrow(() -> ops.readOrThrow(user, req));
	}

	@Test
	void read_singleNonExisting() {
		final var user = new User("u1");
		final var req = new BookRequest("t", "a");
		assertFalse(assertDoesNotThrow(() -> ops.read(user, req)).isPresent());
		assertThrows(NoSuchElementException.class, () -> ops.readOrThrow(user, req));
	}

	@Test
	void read_allForUser() throws TransactionException {
		final var user = new User("reader");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var r1 = new BookRequest("title1", "author1");
		final var r2 = new BookRequest("title2", "author2");
		ops.create(user, r1, new BookRequest.Data(TimeUtil.nowZoned()));
		ops.create(user, r2, new BookRequest.Data(TimeUtil.nowZoned()));

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
	void update_existing() throws TransactionException {
		final var user = new User("u1");
		final var userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(user, userData);

		final var req = new BookRequest("title", "author");
		final var oldData = new BookRequest.Data(TimeUtil.nowZoned());

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
		ops.create(user, req, new BookRequest.Data(TimeUtil.nowZoned()));

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
		ops.create(user, r1, new BookRequest.Data(TimeUtil.nowZoned()));
		ops.create(user, r2, new BookRequest.Data(TimeUtil.nowZoned()));

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
