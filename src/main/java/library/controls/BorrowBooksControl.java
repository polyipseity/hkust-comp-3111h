package library.controls;

import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import library.utils.TimeUtil;
import lombok.Getter;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the control class for borrowing, reading, and returning books.
 * Provides the functionality to borrow books for a specific duration, read
 * borrowed books, check if a book is borrowed, and return borrowed books.
 */
public record BorrowBooksControl(Repository repository) {
    /**
     * Borrow book borrow result.
     *
     * @param user    the user
     * @param book    the book
     * @param minutes the minutes
     * @param seconds the seconds
     * @return the borrow result
     * @throws TransactionException the transaction exception
     */
    public BorrowResult borrowBook(User user, Book book, int minutes, int seconds) throws TransactionException {
		int durationUpperBound = 14 * 24 * 60 * 60;
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

		// Check if book has been borrowed by user before
		if (repository.borrowOps.read(user, book).isPresent())
			return new BorrowResult.BookAlreadyBorrowed();

		// Execute borrow after everything else is successful
		Borrow borrowData = new Borrow(
				TimeUtil.nowZoned(),
				Duration.ofSeconds(durationSeconds),
				generatePdfPath(user, book)
		);
		repository.borrowOps.create(user, book, borrowData);
		repository.bookOps.update(book, current -> current.withTimesBorrowed(current.timesBorrowed() + 1));
		return new BorrowResult.Success(borrowData);
	}

    /**
     * Gets borrowable books.
     *
     * @param user the user
     * @return the borrowable books
     */
    public Map<Book, Book.Data> getBorrowableBooks(User user) {
		final var publishedBooks = repository.bookOps.read(entry -> entry.getValue().published());
		final var borrowedBooks = repository.borrowOps.read(user);
		return publishedBooks.entrySet().stream()
				.filter(book -> !borrowedBooks.containsKey(book.getKey()))
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private String generatePdfPath(User user, Book book) {
		String filteredBookTitle = book.title().replaceAll("[-+.^:,]", "");
		return "%s__%s.pdf".formatted(user.username(), filteredBookTitle);
	}

    /**
     * Read book read result.
     *
     * @param user the user
     * @param book the book
     * @return the read result
     */
    public ReadResult readBook(User user, Book book) {
		Optional<Borrow> borrowData = repository.borrowOps.read(user, book);
		Optional<Book.Data> bookData = repository.bookOps.read(book);
		if (borrowData.isEmpty()) return new ReadResult.BorrowDataNotFound();
		if (bookData.isEmpty()) return new ReadResult.BookDataNotFound();

		// Try to access a pre-existing PDF file; generate one if none exists
		String pdfPath = borrowData.get().pdfPath();
		File f = new File(pdfPath);
		if (!f.exists()) {
			boolean pdfGenerationResult = generatePdf(bookData.get().content(), pdfPath);
			return pdfGenerationResult ?
					new ReadResult.NewPdfGenerated(pdfPath) :
					new ReadResult.PdfGenerationError();
		} else return new ReadResult.Success(pdfPath);
	}

	private boolean generatePdf(String content, String pdfPath) {
		try {
			Document outputDoc = new Document(PageSize.A4, 50, 50, 50, 50);
			FileOutputStream os = new FileOutputStream(pdfPath);
			PdfWriter.getInstance(outputDoc, os);
			outputDoc.open();
			for (String line : content.split("\\r?\\n")) {
				Paragraph p = new Paragraph(line);
				p.setAlignment(Element.ALIGN_JUSTIFIED);
				outputDoc.add(p);
			}
			outputDoc.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

    /**
     * Checks if a book is being borrowed by a user.
     *
     * @param user the user
     * @param book the book
     * @return True if the book is being borrowed by the user, false otherwise.
     */
    public boolean checkBorrowed(User user, Book book) {
		return repository.borrowOps.read(user, book).isPresent();
	}

    /**
     * Returns a book being borrowed by a user.
     *
     * @param user The user whose book will be returned.
     * @param book The book to be returned.
     * @return the return result
     * @throws TransactionException the transaction exception
     */
    public ReturnResult returnBook(User user, Book book) throws TransactionException {
		if (!checkBorrowed(user, book)) return new ReturnResult.BookNotBorrowed();
		else {
			Borrow borrowData = repository.borrowOps.readOrThrow(user, book);
			repository.borrowOps.delete(user, book, borrowData);
			return new ReturnResult.Success();
		}
	}

    /**
     * The interface Borrow result.
     */
    public sealed interface BorrowResult {
        /**
         * The type Success.
         */
        record Success(Borrow borrow) implements BorrowResult {
		}

        /**
         * The type Invalid duration.
         */
        record InvalidDuration(String message) implements BorrowResult, HasMessage {
			@Override
			public String getMessage() {
				return message;
			}
		}

        /**
         * The type Book data not found.
         */
        record BookDataNotFound() implements BorrowResult, HasMessage {
			@Override
			public String getMessage() {
				return "The data for the selected book cannot be found";
			}
		}

        /**
         * The type Book already borrowed.
         */
        record BookAlreadyBorrowed() implements BorrowResult, HasMessage {
			@Override
			public String getMessage() {
				return "The user has already borrowed the book";
			}
		}
	}

    /**
     * The interface Read result.
     */
    public sealed interface ReadResult {
        /**
         * The type Success.
         */
        record Success(@Getter String path) implements ReadResult {
		}

        /**
         * The type New pdf generated.
         */
        record NewPdfGenerated(@Getter String path) implements ReadResult {
		}

        /**
         * The type Pdf generation error.
         */
        record PdfGenerationError() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "Error while generating PDF";
			}
		}

        /**
         * The type Book data not found.
         */
        record BookDataNotFound() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "The data for the book is not found";
			}
		}

        /**
         * The type Borrow data not found.
         */
        record BorrowDataNotFound() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "The borrow data for the book is not found";
			}
		}

        /**
         * The type Book expired.
         */
        record BookExpired() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "The borrow period for the book has expired";
			}
		}
	}

    /**
     * The interface Return result.
     */
    public sealed interface ReturnResult {
        /**
         * The type Success.
         */
        record Success() implements ReturnResult {
		}

        /**
         * The type Book not borrowed.
         */
        record BookNotBorrowed() implements ReturnResult, HasMessage {
			@Override
			public String getMessage() {
				return "The book was not borrowed, so it cannot be returned";
			}
		}
	}
}
