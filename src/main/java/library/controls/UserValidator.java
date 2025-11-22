package library.controls;

import library.utils.HasMessage;
import lombok.Getter;

@FunctionalInterface
public interface UserValidator {
	UserValidator DEFAULT = (username, password, fullName) -> {
		switch (username) {
			case "" -> {
				return new Result.BadUsername("Empty username");
			}
			case String val when val.chars().anyMatch(ch -> ch < 32 || ch >= 127) -> {
				// non-ASCII or control characters
				return new Result.BadUsername("Bad characters in username");
			}
			default -> {
			}
		}
		switch (password) {
			case String val when val.length() < 8 -> {
				return new Result.BadPassword("Password too short");
			}
			case String val when val.chars().anyMatch(ch -> ch < 32 || ch >= 127) -> {
				// non-ASCII or control characters
				return new Result.BadPassword("Bad characters in password");
			}
			default -> {
			}
		}
		switch (fullName) {
			case "" -> {
				return new Result.BadFullName("Empty full name");
			}
			case String val when val.chars().anyMatch(ch -> ch < 32) -> {
				// control characters
				return new Result.BadFullName("Bad characters in full name");
			}
			default -> {
			}
		}
		return new Result.Success();
	};

	Result apply(String username, String password, String fullName);

	sealed interface Result permits Result.BadFullName, Result.BadPassword, Result.BadUsername, Result.Success {
		record Success() implements Result {
		}

		record BadUsername(@Getter String message) implements Result, HasMessage {
		}

		record BadPassword(@Getter String message) implements Result, HasMessage {
		}

		record BadFullName(@Getter String message) implements Result, HasMessage {
		}
	}
}
