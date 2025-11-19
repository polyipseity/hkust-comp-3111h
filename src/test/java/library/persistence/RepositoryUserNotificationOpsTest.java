package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryUserNotificationOpsTest {
	private Repository repository;
	private RepositoryUserNotificationOps ops;

	@SuppressWarnings("SameReturnValue")
	private static boolean populate(@NotNull Repository.Data data) {
		// initialise users with some notifications and no notifications
		final var reader = new User("reader");
		final var reader2 = new User("reader2");
		data.users().put(reader, new User.Data(User.Role.STUDENT_STAFF,
				true,
				"pwd",
				"full name"));
		data.users().put(reader2, new User.Data(User.Role.STUDENT_STAFF,
				true,
				"pwd",
				"full name"));
		// start with two notifications and no notifications
		data.userNotifications().put(reader, new String[]{"n1", "n2"});
		data.userNotifications().put(reader2, new String[]{});
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
		ops = new RepositoryUserNotificationOps(repository);

		repository.transact(RepositoryUserNotificationOpsTest::populate);
	}

	@Test
	void read_allNotifications_unfiltered() {
		// we already have a few users in the fixture; fetch everything
		final var all = assertDoesNotThrow(() -> ops.read());
		assertEquals(2, all.size(), "All two users should be present");

		// verify that each entry is immutable (the value array is returned as‑is,
		// but we can still check that the map itself cannot be modified)
		assertThrows(
				UnsupportedOperationException.class,
				() -> all.put(new User("tmp"), new String[]{"x"}),
				"Returned map must be unmodifiable");
	}

	@Test
	void read_filteredByNonEmptyNotifications() {
		// keep only users that are authors
		final var filtered = assertDoesNotThrow(() ->
				ops.read(entry -> entry.getValue().length > 0));

		// we know the fixture contains exactly one author (the “reader” user)
		assertEquals(1, filtered.size(), "Only one reader should be returned");
		assertTrue(filtered.containsKey(new User("reader")));
	}

	@Test
	void read_existingUser() {
		final var reader = new User("reader");
		final var opt = assertDoesNotThrow(() -> ops.read(reader));
		assertTrue(opt.isPresent(), "Expected notifications to be present");
		assertArrayEquals(new String[]{"n1", "n2"}, opt.get());
		assertDoesNotThrow(() -> ops.readOrThrow(reader));
	}

	@Test
	void read_existingUserWithoutUserNotifications() throws TransactionException {
		final var reader = new User("new reader");
		final var readerData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "full name");
		repository.userOps.create(reader, readerData);

		final var opt = assertDoesNotThrow(() -> ops.read(reader));
		assertTrue(opt.isPresent(), "Expected notifications to be present");
		assertArrayEquals(new String[]{}, opt.get());
		assertDoesNotThrow(() -> ops.readOrThrow(reader));
	}

	@Test
	void read_missingUser() {
		final var missing = new User("missing");
		final var opt = assertDoesNotThrow(() -> ops.read(missing));
		assertFalse(opt.isPresent(), "Expected no notifications for unknown user");
		assertFalse(repository.userNotifications.containsKey(missing));
		assertThrows(NoSuchElementException.class, () -> ops.readOrThrow(missing));
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
	void update_missing() {
		final var reader = new User("missing");

		// add a new notification via the callback
		assertThrows(TransactionException.class, () -> ops.update(reader, old -> {
			assertArrayEquals(new String[]{"n1", "n2"}, old);
			return new String[]{"n1", "n2", "new"};
		}));
	}

	@Test
	void updateAsList_functional() {
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
	void updateAsList_imperative() {
		final var reader = new User("reader");

		// remove the first notification using a List callback
		assertDoesNotThrow(() -> ops.updateAsList(reader, list -> {
			assertEquals(2, list.size());
			list.removeFirst();                // removes "n1"
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
