package library.controls;

import library.models.User;
import library.persistence.Repository;
import library.utils.HasMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public record ManageProfileControl(Repository repository) {
	@NotNull
	public LoginResult login(@NotNull User.Role role, @NotNull String username, @NotNull String password) {
		final var user = new User(username);
		return switch (repository.readUser(user).orElse(null)) {
			case null -> new LoginResult.InvalidUsername();
			case User.Data data when data.role() != role -> new LoginResult.WrongRole(data.role());
			case User.Data data when !data.password().equals(password) -> new LoginResult.InvalidPassword();
			case User.Data data when !data.active() -> new LoginResult.DeactivatedAccount();
			case User.Data data -> new LoginResult.Success(user, data);
		};
	}

	@NotNull
	public RegisterResult register(@NotNull UserValidator validator, @NotNull User.Role role, @NotNull String username, @NotNull String password, @NotNull String fullName) throws Repository.TransactionException {
		return switch (validator.apply(username, password, fullName)) {
			case UserValidator.Result.Success _ -> {
				final var user = new User(username);
				yield switch (repository.readUser(user).orElse(null)) {
					case User.Data _ -> new RegisterResult.UsernameExists();
					case null -> {
						final var data = new User.Data(role, true, password, fullName, Collections.emptyList(), Collections.emptyMap());
						repository.createUser(user, data);
						yield new RegisterResult.Success(user, data);
					}
				};
			}
			case HasMessage cause -> new RegisterResult.InvalidDetails(cause);
		};
	}

	public sealed interface LoginResult {
		record Success(User user, User.Data data) implements LoginResult {
		}

		record InvalidUsername() implements LoginResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Invalid username";
			}
		}

		record WrongRole(User.Role actualRole) implements LoginResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Wrong role: %s".formatted(actualRole.name);
			}
		}

		record InvalidPassword() implements LoginResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Invalid password";
			}
		}

		record DeactivatedAccount() implements LoginResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Deactivated account";
			}
		}
	}

	public sealed interface RegisterResult {
		record Success(@NotNull User user, @NotNull User.Data data) implements RegisterResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Registration successful";
			}
		}

		record InvalidDetails(@NotNull HasMessage cause) implements RegisterResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Invalid details: %s".formatted(cause.getMessage());
			}
		}

		record UsernameExists() implements RegisterResult, HasMessage {
			@Override
			public @NotNull String getMessage() {
				return "Username exists";
			}
		}
	}
}
