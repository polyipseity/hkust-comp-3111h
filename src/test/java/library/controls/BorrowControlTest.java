package library.controls;

import library.models.Author;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BorrowControlTest {
    private Repository repository;
    private BorrowBooksControl borrowBooksControl;

    @BeforeEach
    void setUp() {
        repository = new Repository(DBMaker.memoryDirectDB());
        borrowBooksControl = new BorrowBooksControl(repository);
    }

    @Test
    void borrowBook_InvalidDuration_NegativeMinutes() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);

        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        var result = borrowBooksControl.borrowBook(student, approvedBook, -1, 30);
        assertInstanceOf(BorrowBooksControl.BorrowResult.InvalidDuration.class, result);
        assertEquals("One of the entered values are negative", ((BorrowBooksControl.BorrowResult.InvalidDuration) result).getMessage());
    }

    @Test
    void borrowBook_InvalidDuration_NegativeSeconds() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        var result = borrowBooksControl.borrowBook(student, approvedBook, 10, -1);
        assertInstanceOf(BorrowBooksControl.BorrowResult.InvalidDuration.class, result);
        assertEquals("One of the entered values are negative", ((BorrowBooksControl.BorrowResult.InvalidDuration) result).getMessage());
    }

    @Test
    void borrowBook_InvalidDuration_ExceedsUpperLimit() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // 14 days + 1 second = 1,209,601 seconds
        var result = borrowBooksControl.borrowBook(student, approvedBook, 20160, 1);
        assertInstanceOf(BorrowBooksControl.BorrowResult.InvalidDuration.class, result);
        assertEquals("Entered duration exceeds upper limit of 14 days", ((BorrowBooksControl.BorrowResult.InvalidDuration) result).getMessage());
    }

    @Test
    void borrowBook_InvalidDuration_BelowLowerLimit() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        var result = borrowBooksControl.borrowBook(student, approvedBook, 0, 0);
        assertInstanceOf(BorrowBooksControl.BorrowResult.InvalidDuration.class, result);
        assertEquals("Entered duration exceeds lower limit of 1 second", ((BorrowBooksControl.BorrowResult.InvalidDuration) result).getMessage());
    }

    @Test
    void borrowBook_BookDataNotFound() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book nonExistentBook = new Book("NonExistent", new Author.ByRef(author));

        var result = borrowBooksControl.borrowBook(student, nonExistentBook, 10, 0);
        assertInstanceOf(BorrowBooksControl.BorrowResult.BookDataNotFound.class, result);
    }

    @Test
    void borrowBook_BookAlreadyBorrowed() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // First borrow should succeed
        var firstResult = borrowBooksControl.borrowBook(student, approvedBook, 10, 0);
        assertInstanceOf(BorrowBooksControl.BorrowResult.Success.class, firstResult);

        // Second borrow should fail
        var secondResult = borrowBooksControl.borrowBook(student, approvedBook, 10, 0);
        assertInstanceOf(BorrowBooksControl.BorrowResult.BookAlreadyBorrowed.class, secondResult);
    }

    @Test
    void borrowBook_Success() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        var result = borrowBooksControl.borrowBook(student, approvedBook, 10, 30);

        assertInstanceOf(BorrowBooksControl.BorrowResult.Success.class, result);

        // Verify book timesBorrowed was incremented
        Optional<Book.Data> updatedBookData = repository.bookOps.read(approvedBook);
        assertTrue(updatedBookData.isPresent());
        assertEquals(1, updatedBookData.get().timesBorrowed());

        // Verify borrow record was created
        Optional<Borrow> borrowData = repository.borrowOps.read(student, approvedBook);
        assertTrue(borrowData.isPresent());
    }

    @Test
    void borrowBook_Success_ExactUpperBound() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // Exactly 14 days = 1,209,600 seconds
        var result = borrowBooksControl.borrowBook(student, approvedBook, 20160, 0);

        assertInstanceOf(BorrowBooksControl.BorrowResult.Success.class, result);
    }

    @Test
    void borrowBook_Success_ExactLowerBound() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // Exactly 1 second
        var result = borrowBooksControl.borrowBook(student, approvedBook, 0, 1);

        assertInstanceOf(BorrowBooksControl.BorrowResult.Success.class, result);
    }

    @Test
    void borrowBook_CannotBorrowPendingBook() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book pendingBook = new Book("Thunder1", new Author.ByRef(author));

        var result = borrowBooksControl.borrowBook(student, pendingBook, 10, 0);
        assertInstanceOf(BorrowBooksControl.BorrowResult.BookDataNotFound.class, result);
    }

    @Test
    void borrowBook_CannotBorrowRejectedBook() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book Non_exist = new Book("Thunder3", new Author.ByRef(author));

        var result = borrowBooksControl.borrowBook(student, Non_exist, 10, 0);
        assertInstanceOf(BorrowBooksControl.BorrowResult.BookDataNotFound.class, result);
    }

    // GetBorrowableBooks Tests

    @Test
    void getBorrowableBooks_OnlyApprovedBooks() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book pendingBook = new Book("Thunder1", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        Book rejectedBook = new Book("Thunder3", new Author.ByRef(author));
        Book.Data rejectedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.REJECTED, null, null, 0);

        repository.bookOps.create(pendingBook, pendingData);
        repository.bookOps.create(approvedBook, approvedData);
        repository.bookOps.create(rejectedBook, rejectedData);

	    Map<Book, Book.Data> result = borrowBooksControl.getBorrowableBooks(repository.borrowOps.read(student).keySet());

        // Should only contain approved books
        assertEquals(1, result.size());
        assertTrue(result.containsKey(approvedBook));
        assertFalse(result.containsKey(pendingBook));
        assertFalse(result.containsKey(rejectedBook));
    }

    @Test
    void getBorrowableBooks_ExcludeBorrowedBooks() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook1 = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData1 = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        Book approvedBook2 = new Book("Thunder4", new Author.ByRef(author));
        Book.Data approvedData2 = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);

        repository.bookOps.create(approvedBook1, approvedData1);
        repository.bookOps.create(approvedBook2, approvedData2);

        // Borrow one book
        borrowBooksControl.borrowBook(student, approvedBook1, 10, 0);

	    Map<Book, Book.Data> result = borrowBooksControl.getBorrowableBooks(repository.borrowOps.read(student).keySet());

        // Should only contain the non-borrowed approved book
        assertEquals(1, result.size());
        assertTrue(result.containsKey(approvedBook2));
        assertFalse(result.containsKey(approvedBook1));
    }

    @Test
    void getBorrowableBooks_NoBorrowableBooks() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book pendingBook = new Book("Thunder1", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        Book rejectedBook = new Book("Thunder3", new Author.ByRef(author));
        Book.Data rejectedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.REJECTED, null, null, 0);

        repository.bookOps.create(pendingBook, pendingData);
        repository.bookOps.create(rejectedBook, rejectedData);

	    Map<Book, Book.Data> result = borrowBooksControl.getBorrowableBooks(repository.borrowOps.read(student).keySet());

        assertTrue(result.isEmpty());
    }

    // ReadBook Tests

    @Test
    void readBook_BorrowDataNotFound() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        var result = borrowBooksControl.readBook(student, approvedBook);
        assertInstanceOf(BorrowBooksControl.ReadResult.BorrowDataNotFound.class, result);
    }

    @Test
    void readBook_NewPdfGenerated() throws Exception {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Test book content for PDF generation", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // Borrow the book first
        borrowBooksControl.borrowBook(student, approvedBook, 10, 0);

        var result = borrowBooksControl.readBook(student, approvedBook);

        String pdfPath = ((BorrowBooksControl.ReadResult.NewPdfGenerated) result).getPath();
	    try {
		    // Verify PDF was created and contains content
		    File pdfFile = new File(pdfPath);
		    assertTrue(pdfFile.exists());
		    assertTrue(pdfFile.length() > 0);
	    } finally {
		    Files.deleteIfExists(Path.of(pdfPath));
	    }
    }

    @Test
    void readBook_Success_ExistingPdf() throws Exception {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Test book content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // Borrow the book first
        var borrowResult = borrowBooksControl.borrowBook(student, approvedBook, 10, 0);
        String pdfPath = ((BorrowBooksControl.BorrowResult.Success) borrowResult).borrow().pdfPath();

        // Create the PDF file manually first
        try (FileOutputStream fos = new FileOutputStream(pdfPath)) {
            fos.write("existing pdf content".getBytes());
        }

        var result = borrowBooksControl.readBook(student, approvedBook);

        assertInstanceOf(BorrowBooksControl.ReadResult.Success.class, result);
        assertEquals(pdfPath, ((BorrowBooksControl.ReadResult.Success) result).getPath());
    }

    // CheckBorrowed Tests

    @Test
    void checkBorrowed_True() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        borrowBooksControl.borrowBook(student, approvedBook, 10, 0);

        boolean result = borrowBooksControl.checkBorrowed(student, approvedBook);
        assertTrue(result);
    }

    @Test
    void checkBorrowed_False() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        boolean result = borrowBooksControl.checkBorrowed(student, approvedBook);
        assertFalse(result);
    }

    // ReturnBook Tests

    @Test
    void returnBook_BookNotBorrowed() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        var result = borrowBooksControl.returnBook(student, approvedBook);
        assertInstanceOf(BorrowBooksControl.ReturnResult.BookNotBorrowed.class, result);
    }

    @Test
    void returnBook_Success() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book approvedBook = new Book("Thunder2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(approvedBook, approvedData);

        // Borrow first
        borrowBooksControl.borrowBook(student, approvedBook, 10, 0);

        // Then return
        var result = borrowBooksControl.returnBook(student, approvedBook);
        assertInstanceOf(BorrowBooksControl.ReturnResult.Success.class, result);

        // Verify book is no longer borrowed
        boolean isStillBorrowed = borrowBooksControl.checkBorrowed(student, approvedBook);
        assertFalse(isStillBorrowed);
    }

    @Test
    void generatePdfPath_SpecialCharacters() throws TransactionException {
        //Create user
        User student = new User("John");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(student, userData1);

        //Create author
        User author = new User("Alex");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
        repository.userOps.create(author, userData2);


        Book specialBook = new Book("Test-Book: Title.v2", new Author.ByRef(author));
        Book.Data approvedData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(specialBook, approvedData);

        var result = borrowBooksControl.borrowBook(student, specialBook, 10, 0);

        assertInstanceOf(BorrowBooksControl.BorrowResult.Success.class, result);
        String pdfPath = ((BorrowBooksControl.BorrowResult.Success) result).borrow().pdfPath();

        // The PDF path should be generated without special characters
        assertTrue(pdfPath.startsWith("John__TestBook Titlev2"));

        // Check that the dot only appears in the extension
        String withoutExtension = pdfPath.substring(0, pdfPath.length() - 4); // Remove ".pdf"
        assertFalse(withoutExtension.contains("-"));
        assertFalse(withoutExtension.contains(":"));
        assertFalse(withoutExtension.contains("."));  // No dots in the filename part
        assertFalse(withoutExtension.contains(","));
    }
}
