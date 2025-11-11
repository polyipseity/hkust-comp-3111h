package library.models;

import java.util.Date;

public record BookRequest(
		String title,
		String author
) {
	public record Data(
			Date requestDate
	) {
	}
}
