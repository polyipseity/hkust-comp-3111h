package library.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionExceptionTest {

	@BeforeEach
	void setUp() {
	}

	@AfterEach
	void tearDown() {
	}

	@Nested
	class ConstructorTests {

		@Test
		void defaultConstructor() {
			final var ex = new TransactionException();
			assertEquals("Database transaction exception", ex.getMessage());
			assertNull(ex.getCause());
		}

		@Test
		void constructorWithMessage() {
			final var msg = "Specific error";
			final var ex = new TransactionException(msg);
			assertEquals("Database transaction exception: %s".formatted(msg), ex.getMessage());
			assertNull(ex.getCause());
		}

		@Test
		void constructorWithMessageAndCause() {
			final var cause = new IllegalStateException("inner");
			final var ex = new TransactionException("Outer", cause);
			assertEquals("Database transaction exception: Outer", ex.getMessage());
			assertSame(cause, ex.getCause());
		}

		@Test
		void constructorWithCauseOnly() {
			final var cause = new IllegalStateException("inner");
			final var ex = new TransactionException(cause);
			// When no message is supplied, the default message from Exception is used.
			assertEquals("Database transaction exception: %s".formatted(cause), ex.getMessage());
			assertSame(cause, ex.getCause());
		}
	}

	@Nested
	class GetMessageTests {
		@Test
		void getMessageWithNullDetail() {
			final var ex = new TransactionException((String) null);
			assertEquals("Database transaction exception", ex.getMessage());
		}

		@Test
		void getMessageWithEmptyDetail() {
			final var ex = new TransactionException("");
			assertEquals("Database transaction exception", ex.getMessage());
		}

		@Test
		void getMessageWithNonEmptyDetail() {
			final var detail = "Connection lost";
			TransactionException ex = new TransactionException(detail);
			assertEquals("Database transaction exception: %s".formatted(detail), ex.getMessage());
		}
	}
}
