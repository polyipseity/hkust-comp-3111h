package library.models;

import java.util.Map;

public record User(
		String username
) {
	public enum Role {
		STUDENT_STAFF,
		LIBRARIAN,
		AUTHOR
	}

	public record Data(
			String password,
			boolean active,
			Role role,
			String fullName,
			String[] notifications,
			Map<BookRequest, BookRequest.Data> bookRequests
	) {
	}
}
