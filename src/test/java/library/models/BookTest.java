package library.models;

import library.utils.TimeUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTest {
	@Nested
	class DataTest {
		@Test
		void published_whenApprovedAndNotModified_returnsTrue() {
			final var data = new Book.Data("sum", "cont",
					Book.ApprovalStatus.APPROVED,
					TimeUtil.nowZoned(),
					null,   // originalOrModified == null
					0L);
			assertTrue(data.published());
		}

		@Test
		void published_whenNotApproved_andNotModified_returnsFalse() {
			final var data = new Book.Data("sum", "cont",
					Book.ApprovalStatus.PENDING,
					TimeUtil.nowZoned(),
					null,
					0L);
			assertFalse(data.published());
		}

		@Test
		void published_whenApproved_butIsModification_returnsFalse() {
			final var originalBook = new Book("title", new Author.ByName("author"));          // any non‑null book instance
			final var data = new Book.Data("sum", "cont",
					Book.ApprovalStatus.APPROVED,
					TimeUtil.nowZoned(),
					originalBook,   // not null -> modification
					0L);
			assertFalse(data.published());
		}
	}
}
