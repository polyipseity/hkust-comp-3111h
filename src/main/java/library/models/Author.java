package library.models;

public sealed interface Author {
	record ByRef(User value) implements Author {
	}

	record ByName(String value) implements Author {
	}
}
