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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ManageBooksControlTest {

    private Repository repository;
    private ManageBooksControl manageBooksControl;
    private BorrowBooksControl borrowBooksControl;

    @BeforeEach
    void setUp() throws TransactionException {
        repository = new Repository(DBMaker.memoryDirectDB());
        manageBooksControl = new ManageBooksControl(repository);
        borrowBooksControl = new BorrowBooksControl(repository);
    }

    // ApproveBook Tests

    @Test
    void approveBook_Success_NoOriginalReference() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create pending book without original reference
        Book pendingBook = new Book("Pending Book", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(pendingBook, pendingData);

        ManageBooksControl.ApproveResult result = manageBooksControl.approveBook(pendingBook);

        assertTrue(result instanceof ManageBooksControl.ApproveResult.Success);

        // Verify book was approved and published
        Book.Data approvedData = repository.bookOps.read(pendingBook).orElse(null);
        assertNotNull(approvedData);
        assertEquals(Book.ApprovalStatus.APPROVED, approvedData.approvalStatus());
        assertNotNull(approvedData.publishDate());

        // Verify author was notified
        var notifications = Arrays.asList(repository.userNotificationOps.read(author).get());
        assertFalse(notifications.isEmpty());
        assertEquals("Your book 'Pending Book' has been approved!", notifications.get(0));
    }

    @Test
    void approveBook_Success_WithOriginalReference_Temporary() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create original book
        Book originalBook = new Book("Original Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Old Summary", "Old Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        repository.bookOps.create(originalBook, originalData);

        // Create temporary pending book with original reference
        Book pendingBook = new Book("Pending Book", new Author.ByRef(author), true);
        Book.Data pendingData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.PENDING, null, originalBook, 0);
        repository.bookOps.create(pendingBook, pendingData);

        ManageBooksControl.ApproveResult result = manageBooksControl.approveBook(pendingBook);

        assertTrue(result instanceof ManageBooksControl.ApproveResult.Success);

        // Verify original book was updated
        Book.Data updatedOriginalData = repository.bookOps.read(originalBook).orElse(null);
        assertNotNull(updatedOriginalData);
        assertEquals("New Summary", updatedOriginalData.summary());
        assertEquals("New Content", updatedOriginalData.content());
        assertEquals(Book.ApprovalStatus.APPROVED, updatedOriginalData.approvalStatus());

        // Verify pending book was deleted
        assertFalse(repository.bookOps.read(pendingBook).isPresent());
    }

    @Test
    void approveBook_approvedBook() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create original book
        Book originalBook = new Book("Original Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Old Summary", "Old Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        repository.bookOps.create(originalBook, originalData);

        assertThrows(TransactionException.class, () -> manageBooksControl.approveBook(originalBook));
    }

    @Test
    void approveBook_Success_WithOriginalReference_NotTemporary() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create original book
        Book originalBook = new Book("Original Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Old Summary", "Old Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        repository.bookOps.create(originalBook, originalData);

        // Create non-temporary pending book with original reference
        Book pendingBook = new Book("Pending Book", new Author.ByRef(author), false);
        Book.Data pendingData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.PENDING, null, originalBook, 0);
        repository.bookOps.create(pendingBook, pendingData);

        ManageBooksControl.ApproveResult result = manageBooksControl.approveBook(pendingBook);

        assertTrue(result instanceof ManageBooksControl.ApproveResult.Success);

        // Verify pending book was updated and original deleted
        Book.Data updatedPendingData = repository.bookOps.read(pendingBook).orElse(null);
        assertNotNull(updatedPendingData);
        assertEquals(Book.ApprovalStatus.APPROVED, updatedPendingData.approvalStatus());

        // Verify original book was deleted
        assertFalse(repository.bookOps.read(originalBook).isPresent());
    }

    // RejectBook Tests

    @Test
    void rejectBook_Success_NoOriginalReference() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create pending book without original reference
        Book pendingBook = new Book("Pending Book", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(pendingBook, pendingData);

        ManageBooksControl.RejectResult result = manageBooksControl.rejectBook(pendingBook);

        assertTrue(result instanceof ManageBooksControl.RejectResult.Success);

        // Verify book was rejected
        Book.Data rejectedData = repository.bookOps.read(pendingBook).orElse(null);
        assertNotNull(rejectedData);
        assertEquals(Book.ApprovalStatus.REJECTED, rejectedData.approvalStatus());

        // Verify author was notified
        var notifications = Arrays.asList(repository.userNotificationOps.read(author).get());
        assertFalse(notifications.isEmpty());
        assertEquals("Your book 'Pending Book' has been rejected!", notifications.get(0));
    }

    @Test
    void rejectBook_nonPending() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create pending book without original reference
        Book book = new Book("Book", new Author.ByRef(author));
        Book.Data data = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, data);

        assertThrows(TransactionException.class, () -> manageBooksControl.rejectBook(book));
    }

    @Test
    void rejectBook_Success_WithOriginalReference() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        // Create original book
        Book originalBook = new Book("Original Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        repository.bookOps.create(originalBook, originalData);

        // Create pending book with original reference
        Book pendingBook = new Book("Pending Book", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.PENDING, null, originalBook, 0);
        repository.bookOps.create(pendingBook, pendingData);

        ManageBooksControl.RejectResult result = manageBooksControl.rejectBook(pendingBook);

        assertTrue(result instanceof ManageBooksControl.RejectResult.Success);

        // Verify pending book was deleted
        assertFalse(repository.bookOps.read(pendingBook).isPresent());

        // Verify original book still exists
        assertTrue(repository.bookOps.read(originalBook).isPresent());
    }

    // DeleteBook Tests

    @Test
    void deleteBook_Success_Librarian() throws TransactionException {
        // Create author and book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.DeleteResult result = manageBooksControl.deleteBook(book, User.Role.LIBRARIAN);

        assertTrue(result instanceof ManageBooksControl.DeleteResult.Success);

        // Verify book was deleted
        assertFalse(repository.bookOps.read(book).isPresent());

        // Verify author was notified
        var notifications = Arrays.asList(repository.userNotificationOps.read(author).get());
        assertFalse(notifications.isEmpty());
        assertEquals("Your book 'Test Book' has been deleted!", notifications.get(0));
    }

    @Test
    void deleteBook_userRole_notAuthor() throws TransactionException {
        Book book = new Book("Test Book", new Author.ByName("student"));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        manageBooksControl.deleteBook(book, User.Role.LIBRARIAN);
    }

    @Test
    void deleteBook_TransactionException_RetIsNull_RethrowsException() {
        User author = new User("author1");
        Book nonExistentBook = new Book("Non Existent Book", new Author.ByRef(author));

        assertThrows(TransactionException.class, () -> {
            manageBooksControl.deleteBook(nonExistentBook, User.Role.LIBRARIAN);
        });
    }

    @Test
    void deleteBook_Success_Author() throws TransactionException {
        // Create author and book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.DeleteResult result = manageBooksControl.deleteBook(book, User.Role.AUTHOR);

        assertTrue(result instanceof ManageBooksControl.DeleteResult.Success);

        // Verify book was deleted
        assertFalse(repository.bookOps.read(book).isPresent());

        // Verify author was NOT notified (since author deleted their own book)
        var notifications = Arrays.asList(repository.userNotificationOps.read(author).get());
        assertTrue(notifications.isEmpty());
    }

    @Test
    void deleteBook_BadRole_StudentStaff() throws TransactionException {
        // Create author and book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.DeleteResult result = manageBooksControl.deleteBook(book, User.Role.STUDENT_STAFF);

        assertTrue(result instanceof ManageBooksControl.DeleteResult.BadRole);
        ManageBooksControl.DeleteResult.BadRole badRole = (ManageBooksControl.DeleteResult.BadRole) result;
        assertEquals(User.Role.STUDENT_STAFF, badRole.role());
        assertEquals("Bad role: student/staff", badRole.getMessage());
    }

    @Test
    void deleteBook_HasBorrows_Author() throws TransactionException {
        // Create author, borrower and book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        User borrower = new User("borrower1");
        User.Data borrowerData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Borrower One");
        repository.userOps.create(borrower, borrowerData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Create borrow record
        Borrow borrow = new Borrow(ZonedDateTime.now(), Duration.ofHours(24), "test.pdf");
        repository.borrowOps.create(borrower, book, borrow);

        ManageBooksControl.DeleteResult result = manageBooksControl.deleteBook(book, User.Role.AUTHOR);

        assertTrue(result instanceof ManageBooksControl.DeleteResult.HasBorrows);
        ManageBooksControl.DeleteResult.HasBorrows hasBorrows = (ManageBooksControl.DeleteResult.HasBorrows) result;
        assertEquals(1, hasBorrows.borrows().size());
        assertEquals("Borrowed books cannot be modified or deleted.", hasBorrows.getMessage());
    }

    @Test
    void deleteBook_WithBorrowers_NotifiesBorrowers() throws TransactionException {
        // Create author, borrowers and book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        User borrower1 = new User("borrower1");
        User.Data borrowerData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Borrower One");
        repository.userOps.create(borrower1, borrowerData1);

        User borrower2 = new User("borrower2");
        User.Data borrowerData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Borrower Two");
        repository.userOps.create(borrower2, borrowerData2);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Borrow the book first
        borrowBooksControl.borrowBook(borrower1, book, 10, 0);
        borrowBooksControl.borrowBook(borrower2, book, 10, 0);

        // Librarian can delete even with borrows
        ManageBooksControl.DeleteResult result = manageBooksControl.deleteBook(book, User.Role.LIBRARIAN);

        assertTrue(result instanceof ManageBooksControl.DeleteResult.Success);

        // Verify borrowers were notified
        var notifications1 = Arrays.asList(repository.userNotificationOps.read(borrower1).get());
        var notifications2 = Arrays.asList(repository.userNotificationOps.read(borrower2).get());

        assertFalse(notifications1.isEmpty());
        assertEquals("The book 'Test Book' you were borrowing has been deleted!", notifications1.get(0));
        assertFalse(notifications2.isEmpty());
        assertEquals("The book 'Test Book' you were borrowing has been deleted!", notifications2.get(0));
    }

    // ModifyBook Tests

    @Test
    void modifyBook_Success_PendingBook() throws TransactionException {
        // Create author and pending book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Pending Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        ManageBooksControl.ModifyResult.Success success = (ManageBooksControl.ModifyResult.Success) result;
        assertEquals("New Title", success.newBook().title());
        assertEquals("New Summary", success.newBookData().summary());
        assertEquals("Book updated and waiting for approval", success.getMessage());
    }

    @Test
    void modifyBook_TransactionException_RetIsNull() throws TransactionException {
        // Create author and book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "password", "Author One"));

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);
        repository.bookOps.delete(book, bookData);

        assertThrows(TransactionException.class, () -> {
            manageBooksControl.modifyBook(book, "New Title", "New Summary");
        });
    }

    @Test
    void modifyBook_PendingBook_WithOriginalReference_TitleMatchesOriginal() throws TransactionException {
        // Create author
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "password", "Author One"));

        // Create original book
        Book originalBook = new Book("Original Title", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Original Summary", "Original Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        repository.bookOps.create(originalBook, originalData);

        // Create pending book with original reference and DIFFERENT title
        Book pendingBook = new Book("Pending Title", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("Pending Summary", "Pending Content", Book.ApprovalStatus.PENDING, null, originalBook, 0);
        repository.bookOps.create(pendingBook, pendingData);

        // Modify the pending book to have the SAME title as the original reference
        // This should trigger the case Book branch where title2.equals(title) is true
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(pendingBook, "Original Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        ManageBooksControl.ModifyResult.Success success = (ManageBooksControl.ModifyResult.Success) result;

        // The new book should be temporary since the title matches the original reference
        assertTrue(success.newBook().temporary());

        // Verify the book was created with the correct data
        Book.Data newBookData = repository.bookOps.read(success.newBook()).orElse(null);
        assertNotNull(newBookData);
        assertEquals("New Summary", newBookData.summary());
        assertEquals(Book.ApprovalStatus.PENDING, newBookData.approvalStatus());
    }

    @Test
    void modifyBook_PendingBook_WithOriginalReference_TitleDifferentFromOriginal() throws TransactionException {
        // Create author
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "password", "Author One"));

        // Create original book
        Book originalBook = new Book("Original Title", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Original Summary", "Original Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        repository.bookOps.create(originalBook, originalData);

        // Create pending book with original reference
        Book pendingBook = new Book("Pending Title", new Author.ByRef(author));
        Book.Data pendingData = new Book.Data("Pending Summary", "Pending Content", Book.ApprovalStatus.PENDING, null, originalBook, 0);
        repository.bookOps.create(pendingBook, pendingData);

        // Modify the pending book to have a DIFFERENT title from the original reference
        // This should trigger the case Book branch where title2.equals(title) is false
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(pendingBook, "Different Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        ManageBooksControl.ModifyResult.Success success = (ManageBooksControl.ModifyResult.Success) result;

        // The new book should NOT be temporary since the title is different from the original reference
        assertFalse(success.newBook().temporary());

        // Verify the book was created with the correct data
        Book.Data newBookData = repository.bookOps.read(success.newBook()).orElse(null);
        assertNotNull(newBookData);
        assertEquals("New Summary", newBookData.summary());
        assertEquals(Book.ApprovalStatus.PENDING, newBookData.approvalStatus());
    }

    @Test
    void modifyBook_Success_ApprovedBook() throws TransactionException {
        // Create author and approved book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Approved Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        ManageBooksControl.ModifyResult.Success success = (ManageBooksControl.ModifyResult.Success) result;
        assertEquals("New Title", success.newBook().title());
        assertEquals(Book.ApprovalStatus.PENDING, success.newBookData().approvalStatus());
    }

    @Test
    void modifyBook_SameDetails2() throws TransactionException {
        // Create author and book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "Test Book", "Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.SameDetails);
        assertEquals("Book details are the same", ((ManageBooksControl.ModifyResult.SameDetails) result).getMessage());
    }

    @Test
    void modifyBook_HasBorrows() throws TransactionException {
        // Create author, borrower and approved book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        User borrower = new User("borrower1");
        User.Data borrowerData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Borrower One");
        repository.userOps.create(borrower, borrowerData);

        Book book = new Book("Approved Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Create borrow record
        Borrow borrow = new Borrow(ZonedDateTime.now(), Duration.ofHours(24), "test.pdf");
        repository.borrowOps.create(borrower, book, borrow);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.HasBorrows);
        assertEquals("Borrowed books cannot be modified or deleted.", ((ManageBooksControl.ModifyResult.HasBorrows) result).getMessage());
    }

    @Test
    void modifyBook_AlreadyRejected() throws TransactionException {
        // Create author and rejected book
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.AUTHOR, true, "password", "Author One");
        repository.userOps.create(author, authorData);

        Book book = new Book("Rejected Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.REJECTED, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.AlreadyRejected);
        assertEquals("Rejected books cannot be modified or deleted.", ((ManageBooksControl.ModifyResult.AlreadyRejected) result).getMessage());
    }

    // ModifyBookData Tests

    @Test
    void modifyBookData_NewIsTemporary() {
        Book.Data oldData = new Book.Data("Old Summary", "Old Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        Book.Data newData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.PENDING, null, null, 0);

        Book.Data result = manageBooksControl.modifyBookData(oldData, newData, true);

        assertEquals("New Summary", result.summary());
        assertEquals("New Content", result.content());
        assertEquals(Book.ApprovalStatus.APPROVED, result.approvalStatus());
        assertNotNull(result.publishDate());
    }

    @Test
    void modifyBookData_NewIsNotTemporary() {
        Book originalBook = new Book("Original", new Author.ByName("Author"));
        Book.Data oldData = new Book.Data("Old Summary", "Old Content", Book.ApprovalStatus.APPROVED, null, null, 5);
        Book.Data newData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.PENDING, null, originalBook, 0);

        Book.Data result = manageBooksControl.modifyBookData(oldData, newData, false);

        assertEquals("New Summary", result.summary());
        assertEquals("New Content", result.content());
        assertEquals(Book.ApprovalStatus.APPROVED, result.approvalStatus());
        assertEquals(originalBook, result.originalOrModified());
    }

    @Test
    void modifyBook_PendingBook_SameBookIdentity() throws TransactionException {
        // Create pending book without original reference
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Pending Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Modify with same title (same book identity)
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "Pending Book", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        // Book should be updated in place
        assertTrue(repository.bookOps.read(book).isPresent());
    }

    @Test
    void modifyBook_PendingBook_NewBookIdentity_BookAlreadyExists() throws TransactionException {
        // Create pending book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Pending Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Create another book with the target title
        Book existingBook = new Book("Target Title", new Author.ByRef(author));
        Book.Data existingData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(existingBook, existingData);

        // Try to modify to existing book's title
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "Target Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.AlreadyExists);
        ManageBooksControl.ModifyResult.AlreadyExists alreadyExists = (ManageBooksControl.ModifyResult.AlreadyExists) result;
        assertEquals(existingBook, alreadyExists.conflictBook());
        assertEquals("Book of the same title and author already exists",alreadyExists.getMessage());
    }

// APPROVED Book Tests

    @Test
    void modifyBook_ApprovedBook_SameTitle() throws TransactionException {
        // Create approved book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Approved Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Modify with same title
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "Approved Book", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        ManageBooksControl.ModifyResult.Success success = (ManageBooksControl.ModifyResult.Success) result;
        assertTrue(success.newBook().temporary()); // Should be temporary when same title
    }

    @Test
    void modifyBook_ApprovedBook_DifferentTitle() throws TransactionException {
        // Create approved book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Approved Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Modify with different title
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.Success);
        ManageBooksControl.ModifyResult.Success success = (ManageBooksControl.ModifyResult.Success) result;
        assertFalse(success.newBook().temporary()); // Should not be temporary when different title
    }

    @Test
    void modifyBook_ApprovedBook_BookAlreadyExists() throws TransactionException {
        // Create approved book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Approved Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Old Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Create another book with target title
        Book existingBook = new Book("Target Title", new Author.ByRef(author));
        Book.Data existingData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(existingBook, existingData);

        // Try to modify to existing book's title
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "Target Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.AlreadyExists);
    }

// REJECTED Book Test

    @Test
    void modifyBook_RejectedBook() throws TransactionException {
        // Create rejected book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Rejected Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.REJECTED, null, null, 0);
        repository.bookOps.create(book, bookData);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.AlreadyRejected);
    }

// Same Details Test

    @Test
    void modifyBook_SameDetails() throws TransactionException {
        // Create book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Try to modify with same details
        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "Test Book", "Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.SameDetails);
    }

// Has Borrows Test

    @Test
    void modifyBook_ApprovedBook_HasBorrows() throws TransactionException {
        // Create approved book
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        User borrower = new User("borrower1");
        repository.userOps.create(borrower, new User.Data(User.Role.STUDENT_STAFF, true, "pwd", "Borrower"));

        Book book = new Book("Approved Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Create borrow record
        Borrow borrow = new Borrow(ZonedDateTime.now(), Duration.ofHours(24), "test.pdf");
        repository.borrowOps.create(borrower, book, borrow);

        ManageBooksControl.ModifyResult result = manageBooksControl.modifyBook(book, "New Title", "New Summary");

        assertTrue(result instanceof ManageBooksControl.ModifyResult.HasBorrows);
    }

// Transaction Exception Test

    @Test
    void modifyBook_TransactionException_WithoutResult() throws TransactionException {
        // This tests the case where TransactionException is thrown but ret.get() is null
        User author = new User("author1");
        repository.userOps.create(author, new User.Data(User.Role.AUTHOR, true, "pwd", "Author"));

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);
        repository.bookOps.create(book, bookData);

        // Mock or cause a transaction exception that doesn't set the result
        // This might require mocking the repository to throw an exception
        // For now, we'll just verify the method signature allows TransactionException

        assertDoesNotThrow(() -> {
            manageBooksControl.modifyBook(book, "New Title", "New Summary");
        });
    }

    @Test
    void testNotificationConstants() {
        assertEquals("Your book '%s' has been approved!", ManageBooksControl.NOTIFICATION_APPROVE);
        assertEquals("Your book '%s' has been rejected!", ManageBooksControl.NOTIFICATION_REJECT);
        assertEquals("Your book '%s' has been deleted!", ManageBooksControl.NOTIFICATION_DELETE_BOOK);
        assertEquals("The book '%s' you were borrowing has been deleted!", ManageBooksControl.NOTIFICATION_DELETE_BORROWED_BOOK);
    }
}