package library.persistence;

import library.models.BookRequest;
import library.models.User;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record RepositoryUserBookRequestOps(Repository repository) {
	public void create(User user, BookRequest bookRequest, BookRequest.Data data) throws TransactionException {
		repository.transact(tx -> tx.userBookRequests().put(new Object[]{user, bookRequest}, data) == null, () -> "Already created: %s, %s".formatted(user, bookRequest));
	}

	public Map<Tuple2<User, BookRequest>, BookRequest.Data> read() {
		return read(_ -> true);
	}

	public Map<Tuple2<User, BookRequest>, BookRequest.Data> read(Predicate<? super Map.Entry<Tuple2<User, BookRequest>, BookRequest.Data>> filter) {
		return repository.userBookRequests.entrySet().stream().map(entry -> Map.entry(new Tuple2<>((User) entry.getKey()[0], (BookRequest) entry.getKey()[1]), entry.getValue())).filter(filter).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public Optional<BookRequest.Data> read(User user, BookRequest bookRequest) {
		return Optional.ofNullable(repository.userBookRequests.get(new Object[]{user, bookRequest}));
	}

	public BookRequest.Data readOrThrow(User user, BookRequest bookRequest) {
		return read(user, bookRequest)
				.orElseThrow(() -> new NoSuchElementException(
						"Not found: %s, %s".formatted(user, bookRequest)));
	}

	public Map<BookRequest, BookRequest.Data> read(User user) {
		return repository.userBookRequests.prefixSubMap(new Object[]{user}).entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> (BookRequest) entry.getKey()[1], Map.Entry::getValue));
	}

	public void update(User user, BookRequest bookRequest, Function<BookRequest.Data, BookRequest.Data> callback) throws TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, bookRequest};
			final var oldValue = tx.userBookRequests().get(key);
			return oldValue != null && oldValue.equals(tx.userBookRequests().put(key, callback.apply(oldValue)));
		}, () -> "Not found or updated concurrently: %s, %s".formatted(user, bookRequest));
	}

	public void update(User user,
	                   BookRequest bookRequest,
	                   BookRequest.Data data,
	                   BookRequest.@Nullable Data expected) throws TransactionException {
		repository.transact(
				tx -> {
					final var key = new Object[]{user, bookRequest};
					return expected == null
							? tx.userBookRequests().put(key, data) != null
							: expected.equals(tx.userBookRequests().put(key, data));
				},
				() -> "Not found or updated concurrently: %s, %s".formatted(user, bookRequest)
		);
	}

	public void delete(User user,
	                   BookRequest bookRequest,
	                   @Nullable BookRequest.Data expected) throws TransactionException {
		repository.transact(
				tx -> expected == null ? tx.userBookRequests().remove(new Object[]{user, bookRequest}) != null : tx.userBookRequests().remove(new Object[]{user, bookRequest}, expected),
				() -> "Already deleted: %s, %s".formatted(user, bookRequest)
		);
	}

	void delete(User user, BookRequest bookRequest) throws TransactionException {
		delete(user, bookRequest, null);
	}

	public void delete(User user) throws TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.userBookRequests().prefixSubMap(new Object[]{user}).clear();
			return true;
		}, () -> "User not found: %s".formatted(user));
	}
}
