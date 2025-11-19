package library.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Tuple6Test {
	@Test
	@DisplayName("Tuple6 stores all six elements correctly")
	void testConstructorAndAccessors() {
		var tuple = new Tuple6<>("a", 1, true, 3.14f, 'c', List.of(1, 2, 3));

		assertEquals("a", tuple._1());
		assertEquals(1, tuple._2());
		assertTrue(tuple._3());
		assertEquals(3.14f, tuple._4());
		assertEquals('c', tuple._5());
		assertEquals(List.of(1, 2, 3), tuple._6());
	}
}
