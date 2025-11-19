package library.controls;

import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RequestBooksControlTest {
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
}
