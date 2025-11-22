package library;

import javafx.fxml.FXMLLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

public enum FXMLs {
	AUTHOR_DASHBOARD("/fxml/author/Dashboard.fxml"),
	AUTHOR_MY_BOOKS("/fxml/author/MyBooks.fxml"),
	AUTHOR_MODIFY_WINDOW("/fxml/author/ModifyWindow.fxml"),
	AUTHOR_PUBLISH_BOOKS("/fxml/author/PublishBooks.fxml"),
	AUTHOR_STATUS_VIEW("/fxml/author/StatusView.fxml"),
	COMMON_INFORM_BOARD("/fxml/common/InformBoard.fxml"),
	COMMON_MY_PROFILE("/fxml/common/MyProfile.fxml"),
	COMMON_NOTIFICATION_CELL("/fxml/common/NotificationCell.fxml"),
	COMMON_TEXT_VIEW("/fxml/common/TextView.fxml"),
	HOME("/fxml/Home.fxml"),
	LIBRARIAN_BOOK_DOWNLOAD("/fxml/librarian/BookDownload.fxml"),
	LIBRARIAN_BOOK_REQUESTS("/fxml/librarian/BookRequests.fxml"),
	LIBRARIAN_BORROWED_BOOKS("/fxml/librarian/BorrowedBooks.fxml"),
	LIBRARIAN_DASHBOARD("/fxml/librarian/Dashboard.fxml"),
	LIBRARIAN_MANAGE_USERS("/fxml/librarian/ManageUsers.fxml"),
	LIBRARIAN_PENDING_APPROVALS("/fxml/librarian/PendingApprovals.fxml"),
	LIBRARIAN_PUBLISHED_BOOKS("/fxml/librarian/PublishedBooks.fxml"),
	LOGIN("/fxml/Login.fxml"),
	REGISTER("/fxml/Register.fxml"),
	STUDENT_STAFF_BOOK_VIEW("/fxml/student_staff/BookView.fxml"),
	STUDENT_STAFF_BORROWED_BOOKS("/fxml/student_staff/BorrowedBooks.fxml"),
	STUDENT_STAFF_DASHBOARD("/fxml/student_staff/Dashboard.fxml"),
	STUDENT_STAFF_AVAILABLE_BOOKS("/fxml/student_staff/AvailableBooks.fxml"),
	STUDENT_STAFF_REQUEST_BOOK("/fxml/student_staff/RequestBook.fxml"),
	;

	public final URL resource;

	FXMLs(String name) {
		this.resource = Objects.requireNonNull(getClass().getResource(name));
	}

	@NotNull
	public <T> T load(Consumer<FXMLLoader> callback) throws IOException {
		final var loader = new FXMLLoader(resource);
		callback.accept(loader);
		return loader.load();
	}

	@NotNull
	public <T> T load() throws IOException {
		return load(_ -> {
		});
	}
}
