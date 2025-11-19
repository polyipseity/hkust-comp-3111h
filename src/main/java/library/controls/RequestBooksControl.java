package library.controls;

import library.models.BookRequest;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

public record RequestBooksControl(Repository repository) {
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
}
