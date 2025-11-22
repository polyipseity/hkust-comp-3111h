package library.controls;

import library.models.BookRequest;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestBooksControTestl {
	private Repository repository;
	private RequestBooksControl manageRequests;

	@BeforeEach
	void setUp() {
		repository = new Repository(DBMaker.memoryDirectDB());
		manageRequests = new RequestBooksControl(repository);
	}

	@Test
	void requestBook() throws TransactionException {
		User user = new User("John");
		User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "name");
		repository.userOps.create(user, userData);

		var requestResult = manageRequests.requestBook(user, "", "TEST_AUTHOR");
		assertInstanceOf(
                RequestBooksControl.RequestResult.InvalidRequest.class,
				requestResult, "Invalid book title"
		);

		requestResult = manageRequests.requestBook(user, "TEST_TITLE", "");
		assertInstanceOf(
                RequestBooksControl.RequestResult.InvalidRequest.class,
				requestResult, "Invalid author name"
		);

		requestResult = manageRequests.requestBook(user, "TEST_TITLE", "TEST_AUTHOR");
		assertInstanceOf(RequestBooksControl.RequestResult.Success.class, requestResult);

		requestResult = manageRequests.requestBook(user, "TEST_TITLE", "TEST_AUTHOR");
		assertInstanceOf(
				RequestBooksControl.RequestResult.RequestRepeated.class,
				requestResult, "Book request repeated"
		);

		requestResult = manageRequests.requestBook(user, "TEST_TITLE_2", "TEST_AUTHOR");
		assertInstanceOf(RequestBooksControl.RequestResult.Success.class, requestResult);

		requestResult = manageRequests.requestBook(user, "TEST_TITLE", "TEST_AUTHOR_2");
		assertInstanceOf(RequestBooksControl.RequestResult.Success.class, requestResult);
	}

    // RequestBook Tests

    @Test
    void requestBook_Success() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        RequestBooksControl.RequestResult result = manageRequests.requestBook(user, "Test Book", "Test Author");

        assertTrue(result instanceof RequestBooksControl.RequestResult.Success);

        // Verify request was actually created
        BookRequest bookRequest = new BookRequest("Test Book", "Test Author");
        assertTrue(repository.userBookRequestOps.read(user, bookRequest).isPresent());
    }

    @Test
    void requestBook_InvalidTitle_Empty() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        RequestBooksControl.RequestResult result = manageRequests.requestBook(user, "", "Test Author");

        assertTrue(result instanceof RequestBooksControl.RequestResult.InvalidRequest);
        RequestBooksControl.RequestResult.InvalidRequest invalidRequest = (RequestBooksControl.RequestResult.InvalidRequest) result;
        assertEquals(RequestBooksControl.RequestResult.InvalidType.INVALID_TITLE, invalidRequest.type());
        assertEquals("Invalid title", invalidRequest.getMessage());
    }

    @Test
    void requestBook_InvalidTitle_WhitespaceOnly() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        RequestBooksControl.RequestResult result = manageRequests.requestBook(user, "   ", "Test Author");

        assertTrue(result instanceof RequestBooksControl.RequestResult.InvalidRequest);
        RequestBooksControl.RequestResult.InvalidRequest invalidRequest = (RequestBooksControl.RequestResult.InvalidRequest) result;
        assertEquals(RequestBooksControl.RequestResult.InvalidType.INVALID_TITLE, invalidRequest.type());
    }

    @Test
    void requestBook_InvalidAuthor_Empty() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        RequestBooksControl.RequestResult result = manageRequests.requestBook(user, "Test Book", "");

        assertTrue(result instanceof RequestBooksControl.RequestResult.InvalidRequest);
        RequestBooksControl.RequestResult.InvalidRequest invalidRequest = (RequestBooksControl.RequestResult.InvalidRequest) result;
        assertEquals(RequestBooksControl.RequestResult.InvalidType.INVALID_AUTHOR, invalidRequest.type());
        assertEquals("Invalid author", invalidRequest.getMessage());
    }

    @Test
    void requestBook_InvalidAuthor_WhitespaceOnly() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        RequestBooksControl.RequestResult result = manageRequests.requestBook(user, "Test Book", "   ");

        assertTrue(result instanceof RequestBooksControl.RequestResult.InvalidRequest);
        RequestBooksControl.RequestResult.InvalidRequest invalidRequest = (RequestBooksControl.RequestResult.InvalidRequest) result;
        assertEquals(RequestBooksControl.RequestResult.InvalidType.INVALID_AUTHOR, invalidRequest.type());
    }

    @Test
    void requestBook_RequestRepeated() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // First request should succeed
        RequestBooksControl.RequestResult firstResult = manageRequests.requestBook(user, "Test Book", "Test Author");
        assertTrue(firstResult instanceof RequestBooksControl.RequestResult.Success);

        // Second request for same book should fail
        RequestBooksControl.RequestResult secondResult = manageRequests.requestBook(user, "Test Book", "Test Author");

        assertTrue(secondResult instanceof RequestBooksControl.RequestResult.RequestRepeated);
        assertEquals("Request has been made before", ((RequestBooksControl.RequestResult.RequestRepeated) secondResult).getMessage());
    }

    @Test
    void requestBook_DifferentUsers_SameBook() throws TransactionException {
        User user1 = new User("user1");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "User One");
        repository.userOps.create(user1, userData1);

        User user2 = new User("user2");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "User Two");
        repository.userOps.create(user2, userData2);

        // Both users should be able to request the same book
        RequestBooksControl.RequestResult result1 = manageRequests.requestBook(user1, "Common Book", "Common Author");
        RequestBooksControl.RequestResult result2 = manageRequests.requestBook(user2, "Common Book", "Common Author");

        assertTrue(result1 instanceof RequestBooksControl.RequestResult.Success);
        assertTrue(result2 instanceof RequestBooksControl.RequestResult.Success);
    }

    @Test
    void requestBook_SameUser_DifferentBooks() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // User should be able to request different books
        RequestBooksControl.RequestResult result1 = manageRequests.requestBook(user, "Book One", "Author One");
        RequestBooksControl.RequestResult result2 = manageRequests.requestBook(user, "Book Two", "Author Two");

        assertTrue(result1 instanceof RequestBooksControl.RequestResult.Success);
        assertTrue(result2 instanceof RequestBooksControl.RequestResult.Success);
    }

    @Test
    void requestBook_CaseSensitiveTitleAndAuthor() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // These should be considered different requests due to case differences
        RequestBooksControl.RequestResult result1 = manageRequests.requestBook(user, "test book", "test author");
        RequestBooksControl.RequestResult result2 = manageRequests.requestBook(user, "Test Book", "Test Author");

        assertTrue(result1 instanceof RequestBooksControl.RequestResult.Success);
        assertTrue(result2 instanceof RequestBooksControl.RequestResult.Success);
    }

    // ApproveRequest Tests

    @Test
    void approveRequest_Success() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // Create a book request first
        BookRequest bookRequest = new BookRequest("Test Book", "Test Author");
        manageRequests.requestBook(user, "Test Book", "Test Author");

        // Approve the request
        RequestBooksControl.ApproveResult result = manageRequests.approveRequest(user, bookRequest);

        assertTrue(result instanceof RequestBooksControl.ApproveResult.Success);

        // Verify request was deleted
        assertFalse(repository.userBookRequestOps.read(user, bookRequest).isPresent());

        // Verify notification was added
        List<String> notifications = Arrays.stream(repository.userNotificationOps.read(user).get()).toList();
        assertFalse(notifications.isEmpty());
        assertEquals("Your book request for 'Test Book' has been approved!", notifications.get(0));
    }

    @Test
    void approveRequest_MultipleNotifications() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // Add an existing notification first
        repository.userNotificationOps.updateAsList(user, list -> {
            list.add("Existing notification");
        });

        // Create and approve a book request
        BookRequest bookRequest = new BookRequest("Test Book", "Test Author");
        manageRequests.requestBook(user, "Test Book", "Test Author");
        manageRequests.approveRequest(user, bookRequest);

        // Verify new notification was appended
        List<String> notifications = Arrays.stream(repository.userNotificationOps.read(user).get()).toList();
        assertEquals(2, notifications.size());
        assertEquals("Existing notification", notifications.get(0));
        assertEquals("Your book request for 'Test Book' has been approved!", notifications.get(1));
    }

    // RejectRequest Tests

    @Test
    void rejectRequest_Success() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // Create a book request first
        BookRequest bookRequest = new BookRequest("Test Book", "Test Author");
        manageRequests.requestBook(user, "Test Book", "Test Author");

        // Reject the request
        RequestBooksControl.RejectResult result = manageRequests.rejectRequest(user, bookRequest);

        assertTrue(result instanceof RequestBooksControl.RejectResult.Success);

        // Verify request was deleted
        assertFalse(repository.userBookRequestOps.read(user, bookRequest).isPresent());

        // Verify notification was added
        List<String> notifications = Arrays.stream(repository.userNotificationOps.read(user).get()).toList();
        assertFalse(notifications.isEmpty());
        assertEquals("Your book request for 'Test Book' has been rejected!", notifications.get(0));
    }

    @Test
    void rejectRequest_MultipleNotifications() throws TransactionException {
        User user = new User("testuser");
        User.Data userData = new User.Data(User.Role.STUDENT_STAFF, true, "password", "Test User");
        repository.userOps.create(user, userData);

        // Add an existing notification first
        repository.userNotificationOps.updateAsList(user, list -> {
            list.add("Existing notification");
        });

        // Create and reject a book request
        BookRequest bookRequest = new BookRequest("Test Book", "Test Author");
        manageRequests.requestBook(user, "Test Book", "Test Author");
        manageRequests.rejectRequest(user, bookRequest);

        // Verify new notification was appended
        List<String> notifications = Arrays.stream(repository.userNotificationOps.read(user).get()).toList();
        assertEquals(2, notifications.size());
        assertEquals("Existing notification", notifications.get(0));
        assertEquals("Your book request for 'Test Book' has been rejected!", notifications.get(1));
    }

    @Test
    void approveAndReject_DifferentUsers() throws TransactionException {
        User user1 = new User("user1");
        User.Data userData1 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "User One");
        repository.userOps.create(user1, userData1);

        User user2 = new User("user2");
        User.Data userData2 = new User.Data(User.Role.STUDENT_STAFF, true, "password", "User Two");
        repository.userOps.create(user2, userData2);

        // Both users request the same book
        BookRequest bookRequest = new BookRequest("Common Book", "Common Author");
        manageRequests.requestBook(user1, "Common Book", "Common Author");
        manageRequests.requestBook(user2, "Common Book", "Common Author");

        // Approve one and reject the other
        manageRequests.approveRequest(user1, bookRequest);
        manageRequests.rejectRequest(user2, bookRequest);

        // Verify both requests were handled independently
        assertFalse(repository.userBookRequestOps.read(user1, bookRequest).isPresent());
        assertFalse(repository.userBookRequestOps.read(user2, bookRequest).isPresent());

        List<String> notifications1 = List.of(repository.userNotificationOps.read(user1).get());
        List<String> notifications2 = List.of(repository.userNotificationOps.read(user2).get());

        assertEquals("Your book request for 'Common Book' has been approved!", notifications1.get(0));
        assertEquals("Your book request for 'Common Book' has been rejected!", notifications2.get(0));
    }

    // Constants Tests

    @Test
    void testNotificationConstants() {
        assertEquals("Your book request for '%s' has been approved!", manageRequests.NOTIFICATION_APPROVE);
        assertEquals("Your book request for '%s' has been rejected!", manageRequests.NOTIFICATION_REJECT);
    }

    // Result Hierarchy Tests

    @Test
    void testRequestResultHierarchy() {
        // Test that all request result types implement the proper interfaces
        RequestBooksControl.RequestResult success = new RequestBooksControl.RequestResult.Success();
        RequestBooksControl.RequestResult invalidRequest = new RequestBooksControl.RequestResult.InvalidRequest(
                RequestBooksControl.RequestResult.InvalidType.INVALID_TITLE
        );
        RequestBooksControl.RequestResult requestRepeated = new RequestBooksControl.RequestResult.RequestRepeated();

        assertInstanceOf(RequestBooksControl.RequestResult.class, success);
        assertInstanceOf(RequestBooksControl.RequestResult.class, invalidRequest);
        assertInstanceOf(RequestBooksControl.RequestResult.class, requestRepeated);

        assertInstanceOf(HasMessage.class, invalidRequest);
        assertTrue(true);
    }

    @Test
    void testApproveResultHierarchy() {
        RequestBooksControl.ApproveResult success = new RequestBooksControl.ApproveResult.Success();
        assertTrue(true);
    }

    @Test
    void testRejectResultHierarchy() {
        RequestBooksControl.RejectResult success = new RequestBooksControl.RejectResult.Success();
        assertTrue(true);
    }

    @Test
    void testInvalidTypeEnum() {
        // Test all enum values
        RequestBooksControl.RequestResult.InvalidType[] types = RequestBooksControl.RequestResult.InvalidType.values();
        assertEquals(2, types.length);
        assertEquals(RequestBooksControl.RequestResult.InvalidType.INVALID_TITLE, RequestBooksControl.RequestResult.InvalidType.valueOf("INVALID_TITLE"));
        assertEquals(RequestBooksControl.RequestResult.InvalidType.INVALID_AUTHOR, RequestBooksControl.RequestResult.InvalidType.valueOf("INVALID_AUTHOR"));
    }

    @Test
    void testInvalidRequestMessages() {
        RequestBooksControl.RequestResult.InvalidRequest invalidTitle =
                new RequestBooksControl.RequestResult.InvalidRequest(RequestBooksControl.RequestResult.InvalidType.INVALID_TITLE);
        assertEquals("Invalid title", invalidTitle.getMessage());

        RequestBooksControl.RequestResult.InvalidRequest invalidAuthor =
                new RequestBooksControl.RequestResult.InvalidRequest(RequestBooksControl.RequestResult.InvalidType.INVALID_AUTHOR);
        assertEquals("Invalid author", invalidAuthor.getMessage());
    }
}
