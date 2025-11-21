package library.controls;

import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public record ManageUsersControl(Repository repository) {

	/**
	 * Activates an already inactive user.
	 *
	 * <p>Updates the user's status to active in the database and restores their access privileges.
	 * The user can now perform all actions within the system.</p>
	 *
	 * <p>Any associated notifications or permissions are updated accordingly.</p>
	 *
	 * @param inactiveUser the inactive user to activate; must be in INACTIVE state
	 * @return an ActivateResult indicating success or failure
	 * @throws TransactionException if any database operation fails during the transaction
	 */
	public ActivateResult activateUser(User inactiveUser) throws TransactionException {
		final var data = new AtomicReference<User.@Nullable Data>();
		repository.userOps.update(inactiveUser, userData -> {
			if (userData.active()) {
				throw new IllegalArgumentException("User is active: %s".formatted(inactiveUser));
			}
			final var ret = userData.withActive(true);
			data.set(ret);
			return ret;
		});
		return new ActivateResult.Success(Objects.requireNonNull(data.get()));
	}

	/**
	 * Deactivates an already active user.
	 *
	 * <p>Updates the user's status to inactive in the database and removes their access privileges.
	 * The user remains in the system but cannot perform any actions.</p>
	 *
	 * <p>Any associated notifications or permissions are updated accordingly.</p>
	 *
	 * @param activeUser the active user to deactivate; must be in ACTIVE state
	 * @return a DeactivateResult indicating success or failure
	 * @throws TransactionException if any database operation fails during the transaction
	 */
	public DeactivateResult deactivateUser(User activeUser) throws TransactionException {
		final var data = new AtomicReference<User.@Nullable Data>();
		repository.userOps.update(activeUser, userData -> {
			if (!userData.active()) {
				throw new IllegalArgumentException("User is not active: %s".formatted(activeUser));
			}
			final var ret = userData.withActive(false);
			data.set(ret);
			return ret;
		});
		return new DeactivateResult.Success(Objects.requireNonNull(data.get()));
	}

	/**
	 * Result type for the deactivate operation.
	 */
	public sealed interface DeactivateResult permits DeactivateResult.Success {
		record Success(User.Data data) implements DeactivateResult {
		}
	}

	/**
	 * Result type for the activate operation.
	 */
	public sealed interface ActivateResult permits ActivateResult.Success {
		record Success(User.Data data) implements ActivateResult {
		}
	}
}
