package library;

import javafx.application.Platform;
import javafx.scene.Parent;
import library.models.*;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.TimeUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;

class FXMLsTest {
	private static boolean unsupported = false;

	@SuppressWarnings("SameReturnValue")
	private static boolean populate(Repository.Data data) {
		final var reader = new User("reader");
		final var author = new User("author");
		final var librarian = new User("librarian");
		data.users().put(reader, new User.Data(User.Role.STUDENT_STAFF, true, "reader", "reader"));
		data.users().put(author, new User.Data(User.Role.AUTHOR, true, "author", "author"));
		data.users().put(librarian, new User.Data(User.Role.LIBRARIAN, true, "librarian", "librarian"));
		final var reader2 = new User("reader2");
		final var reader3 = new User("reader3");
		final var reader4 = new User("reader4");
		data.users().put(reader2, new User.Data(User.Role.STUDENT_STAFF, true, "reader", "reader"));
		data.users().put(reader3, new User.Data(User.Role.STUDENT_STAFF, false, "reader", "reader"));
		data.users().put(reader4, new User.Data(User.Role.STUDENT_STAFF, false, "reader", "reader"));

		final var book = new Book("book", new Author.ByRef(author));
		final var book2 = new Book("book", new Author.ByName("author"));
		final var oldBook = new Book("book2", new Author.ByRef(author));
		final var newBook = new Book("book2", new Author.ByRef(author), true);
		data.books().put(book, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
		data.books().put(book2, new Book.Data("summary", "content", Book.ApprovalStatus.REJECTED, null, null, 42));
		data.books().put(oldBook, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42)); // `originalOrModified`: newBook
		data.books().put(newBook, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, null, oldBook, 42));
		final var book3 = new Book("book3", new Author.ByRef(author));
		final var book4 = new Book("book4", new Author.ByRef(author));
		final var book5 = new Book("book5", new Author.ByRef(author));
		data.books().put(book3, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
		data.books().put(book4, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, TimeUtil.nowZoned(), null, 42));
		data.books().put(book5, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
		final var book6 = new Book("book6", new Author.ByRef(reader));
		final var book7 = new Book("book7", new Author.ByRef(reader));
		final var book8 = new Book("book8", new Author.ByRef(reader));
		data.books().put(book6, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));
		data.books().put(book7, new Book.Data("summary", "content", Book.ApprovalStatus.PENDING, TimeUtil.nowZoned(), null, 42));
		data.books().put(book8, new Book.Data("summary", "content", Book.ApprovalStatus.APPROVED, TimeUtil.nowZoned(), null, 42));

		data.userNotifications().put(reader, new String[]{"notification", "notification2"});
		data.userNotifications().put(author, new String[]{"notification"});
		data.userNotifications().put(librarian, new String[]{});

		data.userBookRequests().put(new Object[]{reader, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));
		data.userBookRequests().put(new Object[]{author, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));
		data.userBookRequests().put(new Object[]{librarian, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));

		data.borrows().put(new Object[]{reader, book}, new Borrow(TimeUtil.nowZoned(), Duration.ofMinutes(42), "test.pdf"));
		data.borrows().put(new Object[]{reader, book3}, new Borrow(TimeUtil.nowZoned(), Duration.ofMinutes(42), "test.pdf"));
		data.borrows().put(new Object[]{author, book2}, new Borrow(TimeUtil.nowZoned(), Duration.ofMinutes(42), "test.pdf"));
		data.borrows().put(new Object[]{librarian, oldBook}, new Borrow(TimeUtil.nowZoned(), Duration.ofMinutes(42), "test.pdf"));

		data.userBookRequests().put(new Object[]{reader, new BookRequest("title", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));
		data.userBookRequests().put(new Object[]{reader, new BookRequest("title2", "author")}, new BookRequest.Data(TimeUtil.nowZoned()));

		return true;
	}

	@BeforeAll
	static void setUpAll() throws TransactionException {
		try {
			Platform.startup(() -> {
			});   // starts the toolkit
		} catch (IllegalStateException _) {
		} catch (UnsupportedOperationException ex) {
			// java.lang.UnsupportedOperationException: Unable to open DISPLAY
			unsupported = true;
			abort(ex.getLocalizedMessage());
		}
		final var context = new FakeContext(true);
		Main.setContext(context);
		context.getRepository().transact(FXMLsTest::populate, () -> "");
	}

	@AfterAll
	static void tearDownAll() {
		if (unsupported) {
			return;
		}
		Main.getContext().close();
		Main.setContext(null);
	}

	@Test
	@DisplayName("All FXML files should load without error")
	void allFXMLFilesLoad() {
		for (final var fxml : FXMLs.values()) {
			try {
				// The generic type is usually Parent because most FXML roots are subclasses of it.
				final var root = fxml.load(loader -> loader.setControllerFactory(clazz -> {
					try {
						// Use reflection to invoke the private empty constructor of clazz
						final var constructor = clazz.getDeclaredConstructor();
						constructor.setAccessible(true); // Make private constructor accessible
						return constructor.newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}));
				assertNotNull(root, "Loaded root for %s should not be null".formatted(fxml));
				assertInstanceOf(Parent.class, root, "Loaded root for %s should be an instance of `Parent`".formatted(fxml));
			} catch (IOException e) {
				fail("Failed to load FXML file for %s".formatted(fxml), e);
			}
		}
	}

}
