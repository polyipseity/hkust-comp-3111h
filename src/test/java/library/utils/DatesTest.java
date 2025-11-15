package library.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

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
}
