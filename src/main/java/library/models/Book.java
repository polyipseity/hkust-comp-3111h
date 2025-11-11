package library.models;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

public record Book(
		String title,
		Author author
) {
	public enum ApprovalStatus {
		PENDING,
		APPROVED,
		REJECTED
	}

	public record Borrow(
			Date borrowDate,
			Duration duration,
			byte[] pdfFile
	) {
	}

	public record Data(
			String summary,
			ApprovalStatus approvalStatus,
			Book originalOrModified,
			Map<User, Borrow> borrows,
			long timesBorrowed
	) {
	}
}
