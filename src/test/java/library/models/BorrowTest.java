package library.models;

import library.utils.ByteArray;
import library.utils.TimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorrowTest {
	@Test
	void durationLeft_beforeDue_returnsFullDuration() {
		final var borrowDate = ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0,
				ZoneOffset.UTC);
		final var dur = Duration.ofDays(5);          // due on Jan 6
		final var borrow = new Borrow(borrowDate, dur,
				"test.pdf");

		final var reference = ZonedDateTime.of(2024, 1, 3, 12, 0, 0, 0,
				ZoneOffset.UTC);
		assertEquals(Duration.between(reference,
						borrowDate.plus(dur)),
				borrow.durationLeft(reference));
	}

	@Test
	void durationLeft_afterDue_returnsZero() {
		final var borrowDate = ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0,
				ZoneOffset.UTC);
		final var dur = Duration.ofDays(5);          // due on Jan 6
		final var borrow = new Borrow(borrowDate, dur,
				"test.pdf");

		final var reference = ZonedDateTime.of(2024, 1, 7, 12, 0, 0, 0,
				ZoneOffset.UTC);
		assertEquals(Duration.ZERO, borrow.durationLeft(reference));
	}

	@Test
	void durationLeft_atExactDue_returnsZero() {
		final var borrowDate = ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0,
				ZoneOffset.UTC);
		final var dur = Duration.ofDays(5);          // due on Jan 6
		final var borrow = new Borrow(borrowDate, dur,
				"test.pdf");

		final var reference = borrowDate.plus(dur);          // exactly the due date
		assertEquals(Duration.ZERO, borrow.durationLeft(reference));
	}

	@Test
	void durationLeft_defaultUsesCurrentUTC() {
		final var now = TimeUtil.nowZoned();              // helper that returns UTC instant
		final var borrowDate = now.minusHours(1);
		final var dur = Duration.ofHours(2);          // due 1 hour from now
		final var borrow = new Borrow(borrowDate, dur,
				"test.pdf");

		// The default method should return a duration close to the remaining period.
		final var result = borrow.durationLeft();
		assertTrue(59 <= result.toMinutes() && result.toMinutes() <= 60);
	}
}
