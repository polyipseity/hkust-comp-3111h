package library.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DatesTest {
	@Test
	@DisplayName("nowZoned() should not return null")
	void nowZoned_notNull() {
		final var zdt = Dates.nowZoned();
		assertNotNull(zdt, "nowZoned() returned null");
	}

	@Test
	@DisplayName("nowZoned() should use UTC zone")
	void nowZoned_utcZone() {
		final var zdt = Dates.nowZoned();
		assertEquals(ZoneId.of("UTC"), zdt.getZone(), "nowZoned() did not return UTC zone");
	}

	@Test
	@DisplayName("Two successive calls to nowZoned() should be in chronological order")
	void nowZoned_increasingTimes() throws InterruptedException {
		final var first = Dates.nowZoned();
		// Small pause to guarantee a measurable difference
		Thread.sleep(1);
		final var second = Dates.nowZoned();

		assertTrue(second.isAfter(first),
				"Second call did not return a later time than the first");
	}

	@Test
	@DisplayName("Should convert UTC time to system default zone")
	void zonedLocalToString_utcToSystemDefault() {
		// Arrange: a fixed instant in UTC
		final var utcInstant =
				ZonedDateTime.of(2023, 5, 15, 12, 0, 0, 0, ZoneId.of("UTC"));

		// Act
		final var result = Dates.zonedLocalToString(utcInstant);

		// Assert: the string contains the system default zone id
		assertTrue(result.contains(ZoneId.systemDefault().getId()),
				() -> "Result did not contain system default zone id. Result: %s".formatted(result));

		// Also check that the UTC part was removed (i.e. the hour has changed if the offset is non‑zero)
		final var converted = utcInstant.withZoneSameInstant(ZoneId.systemDefault());
		final var expected = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(converted);
		assertEquals(expected, result,
				() -> """
						Converted string does not match expected value.
						Expected: %s
						Actual:   %s""".formatted(expected, result));
	}

	@Test
	@DisplayName("Should preserve the instant when already in system default zone")
	void zonedLocalToString_alreadyInSystemDefault() {
		// Arrange: a ZonedDateTime that is already in the system default zone
		final var sys = ZoneId.systemDefault();
		final var localInstant =
				ZonedDateTime.of(2024, 1, 1, 8, 30, 0, 0, sys);

		// Act
		final var result = Dates.zonedLocalToString(localInstant);

		// Assert: the string is identical to the original formatted value
		final var expected = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(localInstant);
		assertEquals(expected, result,
				() -> "String representation should be unchanged for local zone.\nExpected: "
						+ expected + "\nActual:   " + result);
	}
}
