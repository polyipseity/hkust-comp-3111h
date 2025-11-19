package library.persistence;

import library.models.BookRequest;
import library.models.User;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryUserBookRequestOps(Repository repository) {
	public void create(@NotNull User user, @NotNull BookRequest bookRequest, @NotNull BookRequest.Data data) throws TransactionException {
		repository.transact(tx -> tx.userBookRequests().put(new Object[]{user, bookRequest}, data) == null, () -> "Already created: %s, %s".formatted(user, bookRequest));
	}

	@NotNull
	public Map<Tuple2<User, BookRequest>, BookRequest.Data> read() {
		return read(_ -> true);
	}

	@NotNull
	public Map<Tuple2<User, BookRequest>, BookRequest.Data> read(@NotNull Predicate<? super Map.@NotNull Entry<Tuple2<User, BookRequest>, BookRequest.Data>> filter) {
		return repository.userBookRequests.entrySet().stream().map(entry -> Map.entry(new Tuple2<>((User) entry.getKey()[0], (BookRequest) entry.getKey()[1]), entry.getValue())).filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@NotNull
	public Optional<BookRequest.Data> read(@NotNull User user, @NotNull BookRequest bookRequest) {
		return Optional.ofNullable(repository.userBookRequests.get(new Object[]{user, bookRequest}));
	}

	@NotNull
	public BookRequest.Data readOrThrow(@NotNull User user, @NotNull BookRequest bookRequest) {
		return read(user, bookRequest)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s, %s".formatted(user, bookRequest)));
	}

	@NotNull
	public Map<BookRequest, BookRequest.Data> read(@NotNull User user) {
		return repository.userBookRequests.prefixSubMap(new Object[]{user}).entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> (BookRequest) entry.getKey()[1], Map.Entry::getValue));
	}

	public void update(@NotNull User user, @NotNull BookRequest bookRequest, @NotNull Function<BookRequest.@NotNull Data, BookRequest.@NotNull Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, bookRequest};
			final var oldValue = tx.userBookRequests().get(key);
			return oldValue != null && oldValue.equals(tx.userBookRequests().put(key, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s, %s".formatted(user, bookRequest));
	}

	public void delete(@NotNull User user,
	                   @NotNull BookRequest bookRequest,
	                   @Nullable BookRequest.Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.userBookRequests().remove(new Object[]{user, bookRequest}) != null : tx.userBookRequests().remove(new Object[]{user, bookRequest}, expected),
				() -> "Already deleted: %s, %s".formatted(user, bookRequest)
		);
	}

	void delete(@NotNull User user, @NotNull BookRequest bookRequest) throws TransactionException {
		delete(user, bookRequest, null);
	}

	public void delete(@NotNull User user) throws TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.userBookRequests().prefixSubMap(new Object[]{user}).clear();
			return true;
		}, () -> "User not found: %s".formatted(user));
	}
}
