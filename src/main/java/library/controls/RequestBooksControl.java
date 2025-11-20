package library.controls;

import library.models.BookRequest;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

public record RequestBooksControl(Repository repository) {
	public static final @NotNull String NOTIFICATION_APPROVE = "Your book request for '%s' has been approved!";
	public static final @NotNull String NOTIFICATION_REJECT = "Your book request for '%s' has been rejected!";

    @NotNull
    public RequestResult requestBook(User user, String title, String author) throws TransactionException {
        // Check if either title or author field is an empty string
        if (title.isEmpty())
            return new RequestResult.InvalidRequest(RequestResult.InvalidType.INVALID_TITLE);
        if (author.isEmpty())
            return new RequestResult.InvalidRequest(RequestResult.InvalidType.INVALID_AUTHOR);

        BookRequest bookRequest = new BookRequest(title, author);
        BookRequest.Data bookRequestData = new BookRequest.Data(ZonedDateTime.now());

        // Check if the user has made the same book request in the past
        if (repository.userBookRequestOps.read(user, bookRequest).isPresent())
            return new RequestResult.RequestRepeated();
        else {
            repository.userBookRequestOps.create(user, bookRequest, bookRequestData);
            return new RequestResult.Success();
        }
    }

    public sealed interface RequestResult {
        enum InvalidType {INVALID_TITLE, INVALID_AUTHOR}

        record Success() implements RequestResult {
        }

        record InvalidRequest(InvalidType type) implements RequestResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return switch (type) {
                    case INVALID_TITLE -> "Invalid title";
                    case INVALID_AUTHOR -> "Invalid author";
                };
            }
        }

        record RequestRepeated() implements RequestResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return "Request has been made before";
            }
        }
    }

	public @NotNull ApproveResult approveRequest(@NotNull User user, @NotNull BookRequest bookRequest) throws TransactionException {
		repository.transact(_ -> {
			repository.userBookRequestOps.delete(user, bookRequest, null);
			repository.userNotificationOps.updateAsList(user, list -> {
				list.add(NOTIFICATION_APPROVE.formatted(bookRequest.title()));
			});
			return true;
		}, () -> "Failed to approve book request: %s, %s".formatted(user, bookRequest));
		return new ApproveResult.Success();
	}

	public @NotNull RejectResult rejectRequest(@NotNull User user, @NotNull BookRequest bookRequest) throws TransactionException {
		repository.transact(_ -> {
			repository.userBookRequestOps.delete(user, bookRequest, null);
			repository.userNotificationOps.updateAsList(user, list -> {
				list.add(NOTIFICATION_REJECT.formatted(bookRequest.title()));
			});
			return true;
		}, () -> "Failed to reject book request: %s, %s".formatted(user, bookRequest));
		return new RejectResult.Success();
	}

	/**
	 * Result type for the approval operation.
	 */
	public sealed interface ApproveResult permits ApproveResult.Success {
		record Success() implements ApproveResult {
		}
	}

	/**
	 * Result type for the rejection operation.
	 */
	public sealed interface RejectResult permits RejectResult.Success {
		record Success() implements RejectResult {
		}
	}
}
