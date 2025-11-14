package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public record RepositoryUserOps(Repository repository) {
	public void create(@NotNull User user, @NotNull User.Data data) throws TransactionException {
		repository.transact(tx -> tx.users().put(user, data) == null);
	}

	@NotNull
	public Optional<User.Data> read(@NotNull User user) {
		return Optional.ofNullable(repository.users.get(user));
	}

	public void update(@NotNull User user, @NotNull Function<User.@NotNull Data, User.@NotNull Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.users().get(user);
			if (oldValue == null) return false;
			tx.users().put(user, callback.apply(oldValue));
			return true;
		});
	}

	public void delete(@NotNull User user) throws TransactionException {
		repository.transact(tx -> tx.users().remove(user) != null);
	}
}
