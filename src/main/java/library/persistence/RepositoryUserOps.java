package library.persistence;

import library.models.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryUserOps(Repository repository) {
	public void create(@NotNull User user, @NotNull User.Data data) throws TransactionException {
		repository.transact(tx -> tx.users().put(user, data) == null, () -> "Already created: %s".formatted(user));
	}

	@NotNull
	public Map<User, User.Data> read() {
		return Collections.unmodifiableMap(repository.users);
	}

	@NotNull
	public Map<User, User.Data> read(@NotNull Predicate<? super Map.@NotNull Entry<User, User.Data>> filter) {
		return repository.users.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@NotNull
	public Optional<User.Data> read(@NotNull User user) {
		return Optional.ofNullable(repository.users.get(user));
	}

	@NotNull
	public User.Data readOrThrow(@NotNull User user) {
		return read(user)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s".formatted(user)));
	}

	public void update(@NotNull User user, @NotNull Function<User.@NotNull Data, User.@NotNull Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.users().get(user);
			return oldValue != null && oldValue.equals(tx.users().put(user, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(user));
	}

	public void update(@NotNull User user,
	                   User.@NotNull Data data,
	                   User.@Nullable Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null
						? tx.users().put(user, data) != null
						: expected.equals(tx.users().put(user, data)),
				() -> "Not found or updated concurrently: %s".formatted(user)
		);
	}

	public void delete(@NotNull User user,
	                   @Nullable User.Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.users().remove(user) != null : tx.users().remove(user, expected),
				() -> "Already deleted: %s".formatted(user)
		);
	}

	void delete(@NotNull User user) throws TransactionException {
		delete(user, null);
	}
}
