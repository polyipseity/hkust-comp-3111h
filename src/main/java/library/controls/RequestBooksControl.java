package library.controls;

import library.models.BookRequest;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import library.utils.TimeUtil;

/**
 * The RequestBooksControl class manages book request operations,
 * including the creation, approval, and rejection of book requests by a user.
 */
public record RequestBooksControl(Repository repository) {
    /**
     * The constant NOTIFICATION_APPROVE.
     */
    public static final String NOTIFICATION_APPROVE = "Your book request for '%s' has been approved!";
    /**
     * The constant NOTIFICATION_REJECT.
     */
    public static final String NOTIFICATION_REJECT = "Your book request for '%s' has been rejected!";

    /**
     * Request book request result.
     *
     * @param user   the user
     * @param title  the title
     * @param author the author
     * @return the request result
     * @throws TransactionException the transaction exception
     */
    public RequestResult requestBook(User user, String title, String author) throws TransactionException {
		// Check if either title or author field is an empty string
		if (title.trim().isEmpty())
			return new RequestResult.InvalidRequest(RequestResult.InvalidType.INVALID_TITLE);
		if (author.trim().isEmpty())
			return new RequestResult.InvalidRequest(RequestResult.InvalidType.INVALID_AUTHOR);

		BookRequest bookRequest = new BookRequest(title, author);
		BookRequest.Data bookRequestData = new BookRequest.Data(TimeUtil.nowZoned());

		// Check if the user has made the same book request in the past
		if (repository.userBookRequestOps.read(user, bookRequest).isPresent())
			return new RequestResult.RequestRepeated();
		else {
			repository.userBookRequestOps.create(user, bookRequest, bookRequestData);
			return new RequestResult.Success();
		}
	}

    /**
     * Approve request approve result.
     *
     * @param user        the user
     * @param bookRequest the book request
     * @return the approve result
     * @throws TransactionException the transaction exception
     */
    public ApproveResult approveRequest(User user, BookRequest bookRequest) throws TransactionException {
		repository.transact(_ -> {
			repository.userBookRequestOps.delete(user, bookRequest, null);
			repository.userNotificationOps.updateAsList(user, list -> {
				list.add(NOTIFICATION_APPROVE.formatted(bookRequest.title()));
			});
			return true;
		}, () -> "Failed to approve book request: %s, %s".formatted(user, bookRequest));
		return new ApproveResult.Success();
	}

    /**
     * Reject request reject result.
     *
     * @param user        the user
     * @param bookRequest the book request
     * @return the reject result
     * @throws TransactionException the transaction exception
     */
    public RejectResult rejectRequest(User user, BookRequest bookRequest) throws TransactionException {
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
     * The interface Request result.
     */
    public sealed interface RequestResult {
        /**
         * The enum Invalid type.
         */
        enum InvalidType {
            /**
             * Invalid title invalid type.
             */
            INVALID_TITLE,
            /**
             * Invalid author invalid type.
             */
            INVALID_AUTHOR}

        /**
         * The type Success.
         */
        record Success() implements RequestResult {
		}

        /**
         * The type Invalid request.
         */
        record InvalidRequest(InvalidType type) implements RequestResult, HasMessage {
			@Override
			public String getMessage() {
				return switch (type) {
					case INVALID_TITLE -> "Invalid title";
					case INVALID_AUTHOR -> "Invalid author";
				};
			}
		}

        /**
         * The type Request repeated.
         */
        record RequestRepeated() implements RequestResult, HasMessage {
			@Override
			public String getMessage() {
				return "Request has been made before";
			}
		}
	}

    /**
     * Result type for the approval operation.
     */
    public sealed interface ApproveResult permits ApproveResult.Success {
        /**
         * The type Success.
         */
        record Success() implements ApproveResult {
		}
	}

    /**
     * Result type for the rejection operation.
     */
    public sealed interface RejectResult permits RejectResult.Success {
        /**
         * The type Success.
         */
        record Success() implements RejectResult {
		}
	}
}
