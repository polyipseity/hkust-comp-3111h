package library.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Tuple2Test {
	@Test
	@DisplayName("Constructor stores values correctly")
	void constructorAndAccessors() {
		final var tuple = new Tuple2<>("foo", 42);

		assertEquals("foo", tuple._1(), "First component mismatch");
		assertEquals(42, tuple._2(), "Second component mismatch");
	}
}
