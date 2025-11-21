package library.persistence;

import library.models.Author;
import library.models.User;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryUserOps(Repository repository) {
	public void create(User user, User.Data data) throws TransactionException {
		repository.transact(tx -> tx.users().put(user, data) == null, () -> "Already created: %s".formatted(user));
	}

	public Map<User, User.Data> read() {
		return Collections.unmodifiableMap(repository.users);
	}

	public Map<User, User.Data> read(Predicate<? super Map.Entry<User, User.Data>> filter) {
		return repository.users.entrySet().stream().filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public Optional<User.Data> read(User user) {
		return Optional.ofNullable(repository.users.get(user));
	}

	public User.Data readOrThrow(User user) {
		return read(user)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s".formatted(user)));
	}

	public String readFullName(Author author) {
		return switch (author) {
			case Author.ByName(final var val) -> val;
			case Author.ByRef(final var val) ->
					read(val).map(User.Data::fullName).orElseGet(() -> "ERROR: %s".formatted(val.username()));
		};
	}

	public void update(User user, Function<User.Data, User.Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var oldValue = tx.users().get(user);
			return oldValue != null && oldValue.equals(tx.users().put(user, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s".formatted(user));
	}

	public void update(User user,
	                   User.Data data,
	                   User.@Nullable Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null
						? tx.users().put(user, data) != null
						: expected.equals(tx.users().put(user, data)),
				() -> "Not found or updated concurrently: %s".formatted(user)
		);
	}

	public void delete(User user,
	                   @Nullable User.Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.users().remove(user) != null : tx.users().remove(user, expected),
				() -> "Already deleted: %s".formatted(user)
		);
	}

	void delete(User user) throws TransactionException {
		delete(user, null);
	}
}
