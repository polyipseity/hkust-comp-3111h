package library.persistence;

import library.models.BookRequest;
import library.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record RepositoryBookRequestOps(Repository repository) {
	@NotNull
	public Optional<BookRequest.Data> read(@NotNull User user, @NotNull BookRequest bookRequest) {
		return Optional.ofNullable(repository.userBookRequests.get(new Object[]{user, bookRequest}));
	}

	@NotNull
	public Map<BookRequest, BookRequest.Data> read(@NotNull User user) {
		return repository.userBookRequests.prefixSubMap(new Object[]{user}).entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> (BookRequest) entry.getKey()[1], Map.Entry::getValue));
	}

	public void create(@NotNull User user, @NotNull BookRequest bookRequest, @NotNull BookRequest.Data data) throws Repository.TransactionException {
		repository.transact(tx -> tx.userBookRequests().put(new Object[]{user, bookRequest}, data) == null);
	}

	public void update(@NotNull User user, @NotNull BookRequest bookRequest, @NotNull Function<BookRequest.@NotNull Data, BookRequest.@NotNull Data> callback) throws Repository.TransactionException {
		repository.transact(tx -> {
			final var key = new Object[]{user, bookRequest};
			final var oldValue = tx.userBookRequests().get(key);
			if (oldValue == null) return false;
			tx.userBookRequests().put(key, callback.apply(oldValue));
			return true;
		});
	}

	public void delete(@NotNull User user, @NotNull BookRequest bookRequest) throws Repository.TransactionException {
		repository.transact(tx -> tx.userBookRequests().remove(new Object[]{user, bookRequest}) != null);
	}

	public void delete(@NotNull User user) throws Repository.TransactionException {
		repository.transact(tx -> {
			if (!tx.users().containsKey(user)) return false;
			tx.userBookRequests().prefixSubMap(new Object[]{user}).clear();
			return true;
		});
	}
}
