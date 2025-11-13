package library.controls;

import library.models.User;
import library.persistence.Repository;
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
			case UserValidator.Result cause -> new RegisterResult.InvalidDetails(cause);
		};
	}

	public sealed interface LoginResult {
		record Success(User user, User.Data data) implements LoginResult {
		}

		record InvalidUsername() implements LoginResult {
		}

		record WrongRole(User.Role actualRole) implements LoginResult {
		}

		record InvalidPassword() implements LoginResult {
		}

		record DeactivatedAccount() implements LoginResult {
		}
	}

	public sealed interface RegisterResult {
		record Success(@NotNull User user, @NotNull User.Data data) implements RegisterResult {
		}

		record InvalidDetails(@NotNull UserValidator.Result cause) implements RegisterResult {
		}

		record UsernameExists() implements RegisterResult {
		}
	}
}
