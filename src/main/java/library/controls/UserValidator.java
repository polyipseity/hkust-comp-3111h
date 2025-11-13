package library.controls;

import library.utils.HasMessage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface UserValidator {
	@NotNull UserValidator DEFAULT = (username, password, fullName) -> {
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
				return new Result.BadUsername("Bad characters in password");
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

	@NotNull Result apply(@NotNull String username, @NotNull String password, @NotNull String fullName);

	sealed interface Result {
		record Success() implements Result {
		}

		record BadUsername(@Getter @NotNull String message) implements Result, HasMessage {
		}

		record BadPassword(@Getter @NotNull String message) implements Result, HasMessage {
		}

		record BadFullName(@Getter @NotNull String message) implements Result, HasMessage {
		}
	}
}
