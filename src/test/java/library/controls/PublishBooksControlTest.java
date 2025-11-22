package library.controls;

import library.models.Author;
import library.models.Book;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.*;

class PublishBooksControlTest {

    private Repository repository;
    private PublishBooksControl publishBooksControl;

    @BeforeEach
    void setUp() throws TransactionException {
        repository = new Repository(DBMaker.memoryDirectDB());
        publishBooksControl = new PublishBooksControl(repository);
    }

    @Test
    void addBook_Success_NewBook() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data bookData = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);

        PublishBooksControl.AddBookResult result = publishBooksControl.addBook(book, bookData);

        assertTrue(result instanceof PublishBooksControl.AddBookResult.Success);
        assertNull(((PublishBooksControl.AddBookResult.Success) result).conflictBookData());

        // Verify book was actually added
        Book.Data storedData = repository.bookOps.read(book).orElse(null);
        assertNotNull(storedData);
        assertEquals("Summary", storedData.summary());
        assertEquals("Content", storedData.content());
        assertEquals(Book.ApprovalStatus.PENDING, storedData.approvalStatus());
    }

    @Test
    void addBook_AlreadyExists_WithoutOverwrite() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Original Summary", "Original Content", Book.ApprovalStatus.PENDING, null, null, 0);
        Book.Data newData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.APPROVED, null, null, 0);

        // Add book first time
        PublishBooksControl.AddBookResult firstResult = publishBooksControl.addBook(book, originalData);
        assertTrue(firstResult instanceof PublishBooksControl.AddBookResult.Success);

        // Try to add same book again without overwrite
        PublishBooksControl.AddBookResult secondResult = publishBooksControl.addBook(book, newData);

        assertTrue(secondResult instanceof PublishBooksControl.AddBookResult.AlreadyExists);
        Book.Data conflictData = ((PublishBooksControl.AddBookResult.AlreadyExists) secondResult).conflictBookData();
        assertEquals("Original Summary", conflictData.summary());
        assertEquals("Original Content", conflictData.content());
        assertEquals(Book.ApprovalStatus.PENDING, conflictData.approvalStatus());
    }

    @Test
    void addBook_Overwrite_ExistingBook() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Original Summary", "Original Content", Book.ApprovalStatus.PENDING, null, null, 0);
        Book.Data newData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.APPROVED, null, null, 5);

        // Add book first time
        PublishBooksControl.AddBookResult firstResult = publishBooksControl.addBook(book, originalData);
        assertTrue(firstResult instanceof PublishBooksControl.AddBookResult.Success);

        // Add same book with overwrite
        PublishBooksControl.AddBookResult secondResult = publishBooksControl.addBook(book, newData, true);

        assertTrue(secondResult instanceof PublishBooksControl.AddBookResult.Success);
        Book.Data conflictData = ((PublishBooksControl.AddBookResult.Success) secondResult).conflictBookData();
        assertNotNull(conflictData);
        assertEquals("Original Summary", conflictData.summary());
        assertEquals("Original Content", conflictData.content());

        // Verify data was overwritten
        Book.Data storedData = repository.bookOps.read(book).orElse(null);
        assertNotNull(storedData);
        assertEquals("New Summary", storedData.summary());
        assertEquals("New Content", storedData.content());
        assertEquals(Book.ApprovalStatus.APPROVED, storedData.approvalStatus());
        assertEquals(5, storedData.timesBorrowed());
    }

    @Test
    void addBook_Success_WithConflictData() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data originalData = new Book.Data("Original Summary", "Original Content", Book.ApprovalStatus.PENDING, null, null, 0);
        Book.Data newData = new Book.Data("New Summary", "New Content", Book.ApprovalStatus.APPROVED, null, null, 0);

        // Add book first time
        publishBooksControl.addBook(book, originalData);

        // Overwrite and check conflict data
        PublishBooksControl.AddBookResult result = publishBooksControl.addBook(book, newData, true);

        assertTrue(result instanceof PublishBooksControl.AddBookResult.Success);
        Book.Data conflictData = ((PublishBooksControl.AddBookResult.Success) result).conflictBookData();
        assertNotNull(conflictData);
        assertEquals("Original Summary", conflictData.summary());
        assertEquals("Original Content", conflictData.content());
        assertEquals(Book.ApprovalStatus.PENDING, conflictData.approvalStatus());
    }

    @Test
    void addBook_MultipleBooks_Success() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book1 = new Book("Book 1", new Author.ByRef(author));
        Book.Data data1 = new Book.Data("Summary 1", "Content 1", Book.ApprovalStatus.PENDING, null, null, 0);

        Book book2 = new Book("Book 2", new Author.ByRef(author));
        Book.Data data2 = new Book.Data("Summary 2", "Content 2", Book.ApprovalStatus.APPROVED, null, null, 0);

        // Add multiple books
        PublishBooksControl.AddBookResult result1 = publishBooksControl.addBook(book1, data1);
        PublishBooksControl.AddBookResult result2 = publishBooksControl.addBook(book2, data2);

        assertTrue(result1 instanceof PublishBooksControl.AddBookResult.Success);
        assertTrue(result2 instanceof PublishBooksControl.AddBookResult.Success);

        // Verify both books were added
        assertTrue(repository.bookOps.read(book1).isPresent());
        assertTrue(repository.bookOps.read(book2).isPresent());
    }

    @Test
    void addBook_DifferentAuthors_SameTitle() throws TransactionException {
        // Create authors
        User author1 = new User("author1");
        User.Data authorData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author One");
        repository.userOps.create(author1, authorData1);

        User author2 = new User("author2");
        User.Data authorData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Two");
        repository.userOps.create(author2, authorData2);

        // Same title, different authors
        Book book1 = new Book("Same Title", new Author.ByRef(author1));
        Book.Data data1 = new Book.Data("Summary 1", "Content 1", Book.ApprovalStatus.PENDING, null, null, 0);

        Book book2 = new Book("Same Title", new Author.ByRef(author2));
        Book.Data data2 = new Book.Data("Summary 2", "Content 2", Book.ApprovalStatus.APPROVED, null, null, 0);

        // Both should succeed since they're different books (different authors)
        PublishBooksControl.AddBookResult result1 = publishBooksControl.addBook(book1, data1);
        PublishBooksControl.AddBookResult result2 = publishBooksControl.addBook(book2, data2);

        assertTrue(result1 instanceof PublishBooksControl.AddBookResult.Success);
        assertTrue(result2 instanceof PublishBooksControl.AddBookResult.Success);

        // Verify both books were added
        assertTrue(repository.bookOps.read(book1).isPresent());
        assertTrue(repository.bookOps.read(book2).isPresent());
    }

    @Test
    void addBook_AlreadyExists_HasMessage() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data data = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);

        // Add book first time
        publishBooksControl.addBook(book, data);

        // Try to add same book again
        PublishBooksControl.AddBookResult result = publishBooksControl.addBook(book, data);

        assertTrue(result instanceof PublishBooksControl.AddBookResult.AlreadyExists);
        assertTrue(result instanceof library.utils.HasMessage);

        String message = ((library.utils.HasMessage) result).getMessage();
        assertEquals("Book already exists", message);
    }

    @Test
    void addBook_WithDifferentApprovalStatuses() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));

        // Test with different approval statuses
        Book.ApprovalStatus[] statuses = {
                Book.ApprovalStatus.PENDING,
                Book.ApprovalStatus.APPROVED,
                Book.ApprovalStatus.REJECTED
        };

        for (Book.ApprovalStatus status : statuses) {
            Book.Data data = new Book.Data("Summary", "Content", status, null, null, 0);
            PublishBooksControl.AddBookResult result = publishBooksControl.addBook(book, data, true);

            assertTrue(result instanceof PublishBooksControl.AddBookResult.Success);

            // Verify status was set correctly
            Book.Data storedData = repository.bookOps.read(book).orElse(null);
            assertNotNull(storedData);
            assertEquals(status, storedData.approvalStatus());
        }
    }

    @Test
    void addBook_WithTimesBorrowed() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("Test Book", new Author.ByRef(author));
        Book.Data data = new Book.Data("Summary", "Content", Book.ApprovalStatus.APPROVED, null, null, 10);

        PublishBooksControl.AddBookResult result = publishBooksControl.addBook(book, data);

        assertTrue(result instanceof PublishBooksControl.AddBookResult.Success);

        // Verify timesBorrowed was set correctly
        Book.Data storedData = repository.bookOps.read(book).orElse(null);
        assertNotNull(storedData);
        assertEquals(10, storedData.timesBorrowed());
    }

    @Test
    void addBook_Overwrite_NoPreviousData() throws TransactionException {
        // Create author
        User author = new User("author1");
        User.Data authorData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Author Name");
        repository.userOps.create(author, authorData);

        Book book = new Book("New Book", new Author.ByRef(author));
        Book.Data data = new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0);

        // Add with overwrite when no previous data exists
        PublishBooksControl.AddBookResult result = publishBooksControl.addBook(book, data, true);

        assertTrue(result instanceof PublishBooksControl.AddBookResult.Success);
        assertNull(((PublishBooksControl.AddBookResult.Success) result).conflictBookData());

        // Verify book was added
        assertTrue(repository.bookOps.read(book).isPresent());
    }

    @Test
    void addBook_ResultHierarchy() {
        // Test that all result types implement the proper interfaces
        PublishBooksControl.AddBookResult success = new PublishBooksControl.AddBookResult.Success(null);
        PublishBooksControl.AddBookResult alreadyExists = new PublishBooksControl.AddBookResult.AlreadyExists(
                new Book.Data("Summary", "Content", Book.ApprovalStatus.PENDING, null, null, 0)
        );

        assertTrue(success instanceof PublishBooksControl.AddBookResult);
        assertTrue(alreadyExists instanceof PublishBooksControl.AddBookResult);
        assertTrue(alreadyExists instanceof library.utils.HasMessage);
    }
}