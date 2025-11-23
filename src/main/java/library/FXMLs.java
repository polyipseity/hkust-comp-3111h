package library;

import javafx.fxml.FXMLLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * An enumeration that manages and provides access to FXML resource files used within the application.
 * <p>
 * Each constant in this enumeration represents a specific FXML file with an associated path.
 * The enum provides utility methods to load FXML resources, allowing controllers to be defined or manipulated
 * programmatically before the FXML hierarchy is instantiated.
 * <p>
 * The FXML files are categorized for different user roles and functionalities, such as author, librarian,
 * student/staff, or common resources.
 * <p>
 * Constructors:
 * - Initializes the FXML resource using a given path and ensures the resource exists at runtime.
 * <p>
 * Methods:
 * - {@link #load(Consumer)}: Loads the FXML resource using the specified callback for configuring the {@link FXMLLoader}.
 * - {@link #load()}: Simplified method to load the FXML resource without additional configuration.
 * <p>
 * Enum Constants:
 * - AUTHOR_DASHBOARD: Path to the author dashboard view.
 * - AUTHOR_MY_BOOKS: Path to the author's books management view.
 * - AUTHOR_MODIFY_WINDOW: Path to the window for modifying books.
 * - AUTHOR_PUBLISH_BOOKS: Path for publishing books.
 * - AUTHOR_STATUS_VIEW: Path to the author status overview.
 * - COMMON_INFORM_BOARD: Path to the informational notice board.
 * - COMMON_MY_PROFILE: Path to the user profile view.
 * - COMMON_NOTIFICATION_CELL: Path to the notification cell template.
 * - COMMON_TEXT_VIEW: Path to the simple text view template.
 * - HOME: Path to the application's home or role selection view.
 * - LIBRARIAN_BOOK_DOWNLOAD: Path to the book download view for librarians.
 * - LIBRARIAN_BOOK_REQUESTS: Path to the librarian's book requests management view.
 * - LIBRARIAN_BORROWED_BOOKS: Path to the librarian's borrowed books view.
 * - LIBRARIAN_DASHBOARD: Path to the librarian dashboard view.
 * - LIBRARIAN_MANAGE_USERS: Path to the manage users view for librarians.
 * - LIBRARIAN_PENDING_APPROVALS: Path to pending approval requests in the librarian's interface.
 * - LIBRARIAN_PUBLISHED_BOOKS: Path to the librarian's view of published books.
 * - LOGIN: Path to the login screen.
 * - REGISTER: Path to the registration screen.
 * - STUDENT_STAFF_BOOK_VIEW: Path to the view for browsing books as a student or staff member.
 * - STUDENT_STAFF_BORROWED_BOOKS: Path to the borrowed books list for students or staff.
 * - STUDENT_STAFF_DASHBOARD: Path to the student/staff dashboard view.
 * - STUDENT_STAFF_AVAILABLE_BOOKS: Path to available books view for students or staff.
 * - STUDENT_STAFF_REQUEST_BOOK: Path to the book request view for students or staff.
 * <p>
 * Thread Safety:
 * This class assumes all FXML files are immutable resources and thread safety is only applicable
 * during the resource loading process. Concurrent usage may require external synchronization.
 * <p>
 * Exception Handling:
 * - If the FXML resource path is invalid or cannot be located, an error will be thrown during initialization.
 * - Issues such as missing controllers or malformed FXML files may occur during the resource loading process.
 * <p>
 * Usage of {@link #load()} or {@link #load(Consumer)} ensures the FXML loading process is customizable,
 * including access to modify the {@link FXMLLoader} before completion.
 */
public enum FXMLs {
    /**
     * Author dashboard fxm ls.
     */
    AUTHOR_DASHBOARD("/fxml/author/Dashboard.fxml"),
    /**
     * Author my books fxm ls.
     */
    AUTHOR_MY_BOOKS("/fxml/author/MyBooks.fxml"),
    /**
     * Author modify window fxm ls.
     */
    AUTHOR_MODIFY_WINDOW("/fxml/author/ModifyWindow.fxml"),
    /**
     * Author publish books fxm ls.
     */
    AUTHOR_PUBLISH_BOOKS("/fxml/author/PublishBooks.fxml"),
    /**
     * Author status view fxm ls.
     */
    AUTHOR_STATUS_VIEW("/fxml/author/StatusView.fxml"),
    /**
     * Common inform board fxm ls.
     */
    COMMON_INFORM_BOARD("/fxml/common/InformBoard.fxml"),
    /**
     * Common my profile fxm ls.
     */
    COMMON_MY_PROFILE("/fxml/common/MyProfile.fxml"),
    /**
     * Common notification cell fxm ls.
     */
    COMMON_NOTIFICATION_CELL("/fxml/common/NotificationCell.fxml"),
    /**
     * Common text view fxm ls.
     */
    COMMON_TEXT_VIEW("/fxml/common/TextView.fxml"),
    /**
     * Home fxm ls.
     */
    HOME("/fxml/Home.fxml"),
    /**
     * Librarian book download fxm ls.
     */
    LIBRARIAN_BOOK_DOWNLOAD("/fxml/librarian/BookDownload.fxml"),
    /**
     * Librarian book requests fxm ls.
     */
    LIBRARIAN_BOOK_REQUESTS("/fxml/librarian/BookRequests.fxml"),
    /**
     * Librarian borrowed books fxm ls.
     */
    LIBRARIAN_BORROWED_BOOKS("/fxml/librarian/BorrowedBooks.fxml"),
    /**
     * Librarian dashboard fxm ls.
     */
    LIBRARIAN_DASHBOARD("/fxml/librarian/Dashboard.fxml"),
    /**
     * Librarian manage users fxm ls.
     */
    LIBRARIAN_MANAGE_USERS("/fxml/librarian/ManageUsers.fxml"),
    /**
     * Librarian pending approvals fxm ls.
     */
    LIBRARIAN_PENDING_APPROVALS("/fxml/librarian/PendingApprovals.fxml"),
    /**
     * Librarian published books fxm ls.
     */
    LIBRARIAN_PUBLISHED_BOOKS("/fxml/librarian/PublishedBooks.fxml"),
    /**
     * Login fxm ls.
     */
    LOGIN("/fxml/Login.fxml"),
    /**
     * Register fxm ls.
     */
    REGISTER("/fxml/Register.fxml"),
    /**
     * Student staff book view fxm ls.
     */
    STUDENT_STAFF_BOOK_VIEW("/fxml/student_staff/BookView.fxml"),
    /**
     * Student staff borrowed books fxm ls.
     */
    STUDENT_STAFF_BORROWED_BOOKS("/fxml/student_staff/BorrowedBooks.fxml"),
    /**
     * Student staff dashboard fxm ls.
     */
    STUDENT_STAFF_DASHBOARD("/fxml/student_staff/Dashboard.fxml"),
    /**
     * Student staff available books fxm ls.
     */
    STUDENT_STAFF_AVAILABLE_BOOKS("/fxml/student_staff/AvailableBooks.fxml"),
    /**
     * Student staff request book fxm ls.
     */
    STUDENT_STAFF_REQUEST_BOOK("/fxml/student_staff/RequestBook.fxml"),
	;

    /**
     * The Resource.
     */
    public final URL resource;

	FXMLs(String name) {
		this.resource = Objects.requireNonNull(getClass().getResource(name));
	}

    /**
     * Loads an FXML file using the given callback to configure the {@link FXMLLoader}.
     * The method initializes the loader with the specified resource and invokes
     * the provided callback for any additional configuration. Once configured, the FXML
     * file is loaded and the root element is returned.
     *
     * @param <T>      the type of the root element of the loaded FXML.
     * @param callback a {@link Consumer} that accepts an {@link FXMLLoader}                 instance to perform additional custom configuration                 before loading the FXML file.
     * @return the root element of the loaded FXML file, guaranteed to be non-null.
     * @throws IOException if an error occurs during FXML loading,                     such as a missing or invalid FXML file.
     */
    @NotNull
	public <T> T load(Consumer<FXMLLoader> callback) throws IOException {
		final var loader = new FXMLLoader(resource);
		callback.accept(loader);
		return loader.load();
	}

    /**
     * Loads a resource and returns the loaded instance of the specified type.
     * <p>
     * This method is useful for loading resources, particularly FXML files, into a
     * usable object for further processing or rendering in a JavaFX application.
     * It utilizes a no-operation callback to perform the loading operation.
     * <p>
     * The returned object is guaranteed to be non-null and represents the loaded
     * content of the resource.
     *
     * @param <T> the type of the object to be loaded and returned
     * @return the loaded instance of the specified type
     * @throws IOException if an I/O error occurs during resource loading
     */
    @NotNull
	public <T> T load() throws IOException {
		return load(_ -> {
		});
	}
}
