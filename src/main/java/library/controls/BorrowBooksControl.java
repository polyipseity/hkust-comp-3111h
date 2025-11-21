package library.controls;

import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import lombok.Getter;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record BorrowBooksControl(Repository repository) {
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

        // Check if book has been borrowed by user before
        if (repository.borrowOps.read(user, book).isPresent())
            return new BorrowResult.BookAlreadyBorrowed();

        // Execute borrow after everything else is successful
        Borrow borrowData = new Borrow(
                ZonedDateTime.now(),
                Duration.ofSeconds(durationSeconds),
                generatePdfPath(user, book)
        );
        repository.borrowOps.create(user, book, borrowData);
        return new BorrowResult.Success();
    }

	public Map<Book, Book.Data> getBorrowableBooks(User user) {
		final var publishedBooks = repository.bookOps.read(entry -> entry.getValue().published());
		final var borrowedBooks = repository.borrowOps.read(user);
		return publishedBooks.entrySet().stream()
				.filter(book -> !borrowedBooks.containsKey(book.getKey()))
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
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
            public String getMessage() {
                return message;
            }
        }

        record BookDataNotFound() implements BorrowResult, HasMessage {
            @Override
            public String getMessage() {
                return "The data for the selected book cannot be found";
            }
        }

        record BookAlreadyBorrowed() implements BorrowResult, HasMessage {
            @Override
            public String getMessage() {
                return "The user has already borrowed the book";
            }
        }
    }

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

	public sealed interface ReadResult {
		record Success(@Getter String path) implements ReadResult {
		}

		record NewPdfGenerated(@Getter String path) implements ReadResult {
		}

		record PdfGenerationError() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "Error while generating PDF";
			}
		}

		record BookDataNotFound() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "The data for the book is not found";
			}
		}

		record BorrowDataNotFound() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "The borrow data for the book is not found";
			}
		}

		record BookExpired() implements ReadResult, HasMessage {
			@Override
			public String getMessage() {
				return "The borrow period for the book has expired";
			}
		}
	}

	/**
	 * Checks if a book is being borrowed by a user.
	 * @return True if the book is being borrowed by the user, false otherwise.
	 */
	public boolean checkBorrowed(User user, Book book) {
		return repository.borrowOps.read(user, book).isPresent();
	}

	/**
	 * Returns a book being borrowed by a user.
	 * @param user The user whose book will be returned.
	 * @param book The book to be returned.
	 */
	public ReturnResult returnBook(User user, Book book) throws TransactionException {
		if (!checkBorrowed(user, book)) return new ReturnResult.BookNotBorrowed();
		else {
			Borrow borrowData = repository.borrowOps.readOrThrow(user, book);
			repository.borrowOps.delete(user, book, borrowData);
			return new ReturnResult.Success();
		}
	}

	public sealed interface ReturnResult {
		record Success() implements ReturnResult {
		}

		record BookNotBorrowed() implements ReturnResult, HasMessage {
			@Override
			public String getMessage() { return "The book was not borrowed, so it cannot be returned"; }
		}
	}

	public Map<Book, Duration> getBorrowDurations(User user) {
		return repository.borrowOps.read(user).entrySet().stream().collect(
				Collectors.toMap(Map.Entry::getKey, e -> e.getValue().durationLeft()));
	}

	public void returnExpiredBooks(User user) {
		getBorrowDurations(user).forEach((book, duration) -> {
			if (duration.isZero()) {
				try {
					returnBook(user, book);
				} catch (TransactionException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
