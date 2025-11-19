package library.controls;

import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.jetbrains.annotations.NotNull;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

public record ManageBorrowControl(Repository repository) {
    @NotNull
    public BorrowResult borrowBook(User user, Book book, int minutes, int seconds) throws TransactionException {
        int durationUpperBound = 1 + 14 * 24 * 60 * 60;
        int durationLowerBound = 1;
        int durationSeconds = minutes * 60 + seconds;

        // Validate user provided duration
        if (minutes < 0 || seconds < 0)
            return new BorrowResult.InvalidDuration("One of the entered values are negative");
        if (durationSeconds > durationUpperBound)
            return new BorrowResult.InvalidDuration("Entered duration exceeds upper limit of 14 days");
        if (durationSeconds < durationLowerBound)
            return new BorrowResult.InvalidDuration("Entered duration exceeds lower limit of 1 second");

        // Try and obtain book's data
        Optional<Book.Data> selectedBookData = repository.bookOps.read(book);
        if (selectedBookData.isEmpty()) return new BorrowResult.BookDataNotFound();

        // Execute borrow after everything else is successful
        Borrow borrowData = new Borrow(
                ZonedDateTime.now(),
                Duration.ofSeconds(durationSeconds),
                generatePdfPath(user, book)
        );
        repository.borrowOps.create(user, book, borrowData);
        return new BorrowResult.Success();
    }

    public Map<Book, Book.Data> availableBooks(User user) {
        return repository.bookOps.read(entry -> {
            Book book = entry.getKey();
            Book.Data bookData = entry.getValue();
            return repository.borrowOps.read(user, book).isEmpty();
        });
    }

    private String generatePdfPath(User user, Book book) {
        String filteredBookTitle = book.title().replaceAll("[-+.^:,]", "");
        return user.username() + "__" + filteredBookTitle + ".pdf";
    }

    public sealed interface BorrowResult {
        record Success() implements BorrowResult {
        }

        record InvalidDuration(String message) implements BorrowResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return message;
            }
        }

        record BookDataNotFound() implements BorrowResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return "The data for the selected book cannot be found";
            }
        }
    }
}
