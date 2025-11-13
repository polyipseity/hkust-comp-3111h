package library.controls;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface UserValidator {
	@NotNull UserValidator DEFAULT = (username, password, fullName) -> {
		if (username.isEmpty()) {
			return new Result.BadUsername("Empty username");
		}
		if (username.chars().anyMatch(ch -> ch < 32 || ch >= 127)) { // non-ASCII or control characters
			return new Result.BadUsername("Bad characters in username");
		}
		if (password.length() < 8) {
			return new Result.BadPassword("Password too short");
		}
		if (password.chars().anyMatch(ch -> ch < 32 || ch >= 127)) {// non-ASCII or control characters
			return new Result.BadPassword("Bad characters in password");
		}
		if (fullName.isEmpty()) {
			return new Result.BadFullName("Empty full name");
		}
		if (fullName.chars().anyMatch(ch -> ch < 32)) { // control characters
			return new Result.BadFullName("Bad characters in full name");
		}
		return new Result.Success();
	};

	@NotNull Result apply(@NotNull String username, @NotNull String password, @NotNull String fullName);

	sealed interface Result {
		record Success() implements Result {
		}

		record BadUsername(@NotNull String message) implements Result {
		}

		record BadPassword(@NotNull String message) implements Result {
		}

		record BadFullName(@NotNull String message) implements Result {
		}
	}
}
