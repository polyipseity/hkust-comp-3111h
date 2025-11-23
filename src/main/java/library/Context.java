package library;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.utils.ThrowingFunction;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Represents a context providing access to various controls, services, and resources within the application.
 * This interface supports the management of application windows, user sessions, and timeline listeners.
 * It also facilitates the control of system operations through various control interfaces.
 */
public interface Context extends Closeable {
    /**
     * Is testing boolean.
     *
     * @return the boolean
     */
    boolean isTesting();

    /**
     * Sets scene.
     *
     * @param value the value
     */
    void setScene(Parent value);

    /**
     * New window stage.
     *
     * @param <E>            the type parameter
     * @param title          the title
     * @param parentSupplier the parent supplier
     * @param stage          the stage
     * @return the stage
     * @throws E the e
     */
    <E extends Throwable> Stage newWindow(String title, ThrowingFunction<? super Stage, ? extends Parent, ? extends E> parentSupplier, @Nullable Stage stage) throws E;

    /**
     * New window stage.
     *
     * @param title  the title
     * @param parent the parent
     * @param stage  the stage
     * @return the stage
     */
    default Stage newWindow(String title, Parent parent, @Nullable Stage stage) {
		return newWindow(title, _ -> parent, stage);
	}

    /**
     * Gets primary stage.
     *
     * @return the primary stage
     */
    Stage getPrimaryStage();

    /**
     * Gets repository.
     *
     * @return the repository
     */
    Repository getRepository();

    /**
     * Gets ai service control.
     *
     * @return the ai service control
     */
    AIServiceControl getAIServiceControl();

    /**
     * Gets book download control.
     *
     * @return the book download control
     */
    BookDownloadControl getBookDownloadControl();

    /**
     * Gets borrow books control.
     *
     * @return the borrow books control
     */
    BorrowBooksControl getBorrowBooksControl();

    /**
     * Gets manage books control.
     *
     * @return the manage books control
     */
    ManageBooksControl getManageBooksControl();

    /**
     * Gets manage notifications control.
     *
     * @return the manage notifications control
     */
    ManageNotificationsControl getManageNotificationsControl();

    /**
     * Gets manage profile control.
     *
     * @return the manage profile control
     */
    ManageProfileControl getManageProfileControl();

    /**
     * Gets manage users control.
     *
     * @return the manage users control
     */
    ManageUsersControl getManageUsersControl();

    /**
     * Gets publish books control.
     *
     * @return the publish books control
     */
    PublishBooksControl getPublishBooksControl();

    /**
     * Gets request books control.
     *
     * @return the request books control
     */
    RequestBooksControl getRequestBooksControl();

    /**
     * Gets stats control.
     *
     * @return the stats control
     */
    StatsControl getStatsControl();

    /**
     * Add second timeline listener.
     *
     * @param key      the key
     * @param listener the listener
     */
    void addSecondTimelineListener(Object key, Consumer<? super ActionEvent> listener);

    /**
     * Add minute timeline listener.
     *
     * @param key      the key
     * @param listener the listener
     */
    void addMinuteTimelineListener(Object key, Consumer<? super ActionEvent> listener);

    /**
     * Gets logged in user.
     *
     * @return the logged in user
     */
    @Nullable
	Tuple2<User, User.Data> getLoggedInUser();

    /**
     * Sets logged in user.
     *
     * @param loggedInUser the logged in user
     */
    @SuppressWarnings("EmptyMethod")
	void setLoggedInUser(@SuppressWarnings("unused") @Nullable Tuple2<library.models.User, library.models.User.Data> loggedInUser);

    /**
     * Gets logged in token.
     *
     * @return the logged in token
     */
    @Nullable
	Object getLoggedInToken();

	/**
	 * Closes this stream and releases any system resources associated
	 * with it. If the stream is already closed then invoking this
	 * method has no effect.
	 *
	 * <p> As noted in {@link AutoCloseable#close()}, cases where the
	 * close may fail require careful attention. It is strongly advised
	 * to relinquish the underlying resources and to internally
	 * <em>mark</em> the {@code Closeable} as closed, prior to throwing
	 * the {@code IOException}.
	 */
	@Override
	void close();
}
