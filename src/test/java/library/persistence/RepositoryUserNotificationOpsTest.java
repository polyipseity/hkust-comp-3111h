package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryUserNotificationOpsTest {
	private Repository repository;
	private RepositoryUserNotificationOps ops;

	@SuppressWarnings("SameReturnValue")
	private static boolean populate(@NotNull Repository.TransactData data) {
		// initialise a user with some notifications
		final var reader = new User("reader");
		data.users().put(reader, new User.Data(User.Role.STUDENT_STAFF,
				true,
				"pwd",
				"full name"));
		// start with two notifications
		data.userNotifications().put(reader, new String[]{"n1", "n2"});
		return true;
	}

	@BeforeEach
	void setUp() throws TransactionException {
		// use an in‑memory MapDB instance – the same as used in RepositoryTest
		repository = new Repository(DBMaker::memoryDirectDB);
		ops = new RepositoryUserNotificationOps(repository);

		repository.transact(RepositoryUserNotificationOpsTest::populate);
	}

	@Test
	void read_existingUser() {
		final var reader = new User("reader");
		final var opt = ops.read(reader);
		assertTrue(opt.isPresent(), "Expected notifications to be present");
		assertArrayEquals(new String[]{"n1", "n2"}, opt.get());
	}

	@Test
	void read_missingUser() {
		final var missing = new User("missing");
		final var opt = ops.read(missing);
		assertFalse(opt.isPresent(), "Expected no notifications for unknown user");
	}

	@Test
	void update() {
		final var reader = new User("reader");

		// add a new notification via the callback
		assertDoesNotThrow(() -> ops.update(reader, old -> {
			assertArrayEquals(new String[]{"n1", "n2"}, old);
			return new String[]{"n1", "n2", "new"};
		}));

		// read back the value from the repository to verify the change
		final var stored = ops.read(reader);
		assertTrue(stored.isPresent());
		assertArrayEquals(new String[]{"n1", "n2", "new"}, stored.get());
	}

	@Test
	void updateAsList() {
		final var reader = new User("reader");

		// remove the first notification using a List callback
		assertDoesNotThrow(() -> ops.updateAsList(reader, list -> {
			assertEquals(2, list.size());
			list.removeFirst();                // removes "n1"
			return list;
		}));

		// read back and check that only the second element remains
		final var stored = ops.read(reader);
		assertTrue(stored.isPresent());
		assertArrayEquals(new String[]{"n2"}, stored.get());
	}

	@Test
	void repository() {
		assertSame(repository, ops.repository());
	}
}
