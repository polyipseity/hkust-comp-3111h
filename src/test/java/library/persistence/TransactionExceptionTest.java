package library.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionExceptionTest {
	@Test
	void constructor_default() {
		final var ex = new TransactionException();
		assertEquals("Database transaction exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void constructor_withMessage() {
		final var msg = "Specific error";
		final var ex = new TransactionException(msg);
		assertEquals("Database transaction exception: %s".formatted(msg), ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void constructor_withMessageAndCause() {
		final var cause = new IllegalStateException("inner");
		final var ex = new TransactionException("Outer", cause);
		assertEquals("Database transaction exception: Outer", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void constructor_withCauseOnly() {
		final var cause = new IllegalStateException("inner");
		final var ex = new TransactionException(cause);
		// When no message is supplied, the default message from Exception is used.
		assertEquals("Database transaction exception: %s".formatted(cause), ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void getMessage_withNullDetail() {
		final var ex = new TransactionException((String) null);
		assertEquals("Database transaction exception", ex.getMessage());
	}

	@Test
	void getMessage_withEmptyDetail() {
		final var ex = new TransactionException("");
		assertEquals("Database transaction exception", ex.getMessage());
	}

	@Test
	void getMessage_withNonEmptyDetail() {
		final var detail = "Connection lost";
		TransactionException ex = new TransactionException(detail);
		assertEquals("Database transaction exception: %s".formatted(detail), ex.getMessage());
	}
}
