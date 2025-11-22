package library.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTest {
	@Test
	@DisplayName("nowZoned() should not return null")
	void nowZoned_notNull() {
		final var zdt = TimeUtil.nowZoned();
		assertNotNull(zdt, "nowZoned() returned null");
	}

	@Test
	@DisplayName("nowZoned() should use UTC zone")
	void nowZoned_utcZone() {
		final var zdt = TimeUtil.nowZoned();
		assertEquals(ZoneOffset.UTC, zdt.getZone(), "nowZoned() did not return UTC zone");
	}

	@Test
	@DisplayName("Two successive calls to nowZoned() should be in chronological order")
	void nowZoned_increasingTimes() throws InterruptedException {
		final var first = TimeUtil.nowZoned();
		// Small pause to guarantee a measurable difference
		Thread.sleep(1);
		final var second = TimeUtil.nowZoned();

		assertTrue(second.isAfter(first),
				"Second call did not return a later time than the first");
	}

	@Test
	@DisplayName("Should convert UTC time to system default zone")
	void toSystemDefaultZonedLocal() {
		// Arrange: a fixed instant in UTC
		final var utcInstant =
				ZonedDateTime.of(2023, 5, 15, 12, 0, 0, 0, ZoneOffset.UTC);

		// Act
		final var result = TimeUtil.toStringZonedLocal(utcInstant);

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
	void toString_ZonedLocal_alreadyInSystemDefault() {
		// Arrange: a ZonedDateTime that is already in the system default zone
		final var sys = ZoneId.systemDefault();
		final var localInstant =
				ZonedDateTime.of(2024, 1, 1, 8, 30, 0, 0, sys);

		// Act
		final var result = TimeUtil.toStringZonedLocal(localInstant);

		// Assert: the string is identical to the original formatted value
		final var expected = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(localInstant);
		assertEquals(expected, result,
				() -> """
						String representation should be unchanged for local zone.
						Expected: %s
						Actual:   %s""".formatted(expected, result));
	}

	@Test
	void toStringDuration_zeroDuration_returnsZero() {
		final var dur = Duration.ZERO;
		assertEquals("0d 00:00:00", TimeUtil.toStringDuration(dur));
	}

	@Test
	void toStringDuration_simpleDuration_formatsCorrectly() {
		final var dur = Duration.ofHours(3).plusMinutes(15).plusSeconds(42);
		// 3 hours = 0 days, 3 hours, 15 minutes, 42 seconds
		assertEquals("0d 03:15:42", TimeUtil.toStringDuration(dur));
	}

	@Test
	void toStringDuration_longDuration_includesDays() {
		final var dur = Duration.ofDays(2).plusHours(5)
				.plusMinutes(9).plusSeconds(7);
		// 2 days, 5 hours, 9 minutes, 7 seconds
		assertEquals("2d 05:09:07", TimeUtil.toStringDuration(dur));
	}

	@Test
	void toStringDuration_leadingZeros_arePreserved() {
		final var dur = Duration.ofHours(1).plusMinutes(2).plusSeconds(3);
		// hours <10, minutes <10, seconds <10 → leading zeros required
		assertEquals("0d 01:02:03", TimeUtil.toStringDuration(dur));
	}
}
