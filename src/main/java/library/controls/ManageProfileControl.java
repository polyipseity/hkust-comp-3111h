package library.controls;

import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;

/**
 * The type Manage profile control.
 */
public record ManageProfileControl(Repository repository) {
    /**
     * Login login result.
     *
     * @param role     the role
     * @param username the username
     * @param password the password
     * @return the login result
     */
    public LoginResult login(User.Role role, String username, String password) {
		final var user = new User(username);
		return switch (repository.userOps.read(user).orElse(null)) {
			case null -> new LoginResult.InvalidUsername();
			case User.Data data when data.role() != role -> new LoginResult.WrongRole(data.role());
			case User.Data data when !data.password().equals(password) -> new LoginResult.InvalidPassword();
			case User.Data data when !data.active() -> new LoginResult.DeactivatedAccount();
			case User.Data data -> new LoginResult.Success(user, data);
		};
	}

    /**
     * Register register result.
     *
     * @param validator the validator
     * @param role      the role
     * @param username  the username
     * @param password  the password
     * @param fullName  the full name
     * @return the register result
     * @throws TransactionException the transaction exception
     */
    public RegisterResult register(UserValidator validator, User.Role role, String username, String password, String fullName) throws TransactionException {
		return switch (validator.apply(username, password, fullName)) {
			case UserValidator.Result.Success _ -> {
				final var user = new User(username);
				yield switch (repository.userOps.read(user).orElse(null)) {
					case User.Data _ -> new RegisterResult.UsernameExists();
					case null -> {
						final var data = new User.Data(role, true, password, fullName);
						repository.userOps.create(user, data);
						yield new RegisterResult.Success(user, data);
					}
				};
			}
			case HasMessage cause -> new RegisterResult.InvalidDetails(cause);
		};
	}

    /**
     * The interface Login result.
     */
    public sealed interface LoginResult permits LoginResult.DeactivatedAccount, LoginResult.InvalidPassword, LoginResult.InvalidUsername, LoginResult.Success, LoginResult.WrongRole {
        /**
         * The type Success.
         */
        record Success(User user, User.Data data) implements LoginResult {
		}

        /**
         * The type Invalid username.
         */
        record InvalidUsername() implements LoginResult, HasMessage {
			@Override
			public String getMessage() {
				return "Invalid username";
			}
		}

        /**
         * The type Wrong role.
         */
        record WrongRole(User.Role actualRole) implements LoginResult, HasMessage {
			@Override
			public String getMessage() {
				return "Logging in as wrong role: %s".formatted(actualRole.name);
			}
		}

        /**
         * The type Invalid password.
         */
        record InvalidPassword() implements LoginResult, HasMessage {
			@Override
			public String getMessage() {
				return "Invalid password";
			}
		}

        /**
         * The type Deactivated account.
         */
        record DeactivatedAccount() implements LoginResult, HasMessage {
			@Override
			public String getMessage() {
				return "Deactivated account";
			}
		}
	}

    /**
     * The interface Register result.
     */
    public sealed interface RegisterResult permits RegisterResult.InvalidDetails, RegisterResult.Success, RegisterResult.UsernameExists {
        /**
         * The type Success.
         */
        record Success(User user, User.Data data) implements RegisterResult, HasMessage {
			@Override
			public String getMessage() {
				return "Registration successful";
			}
		}

        /**
         * The type Invalid details.
         */
        record InvalidDetails(HasMessage cause) implements RegisterResult, HasMessage {
			@Override
			public String getMessage() {
				return "Invalid details: %s".formatted(cause.getMessage());
			}
		}

        /**
         * The type Username exists.
         */
        record UsernameExists() implements RegisterResult, HasMessage {
			@Override
			public String getMessage() {
				return "Username exists";
			}
		}
	}
}
