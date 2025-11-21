package library;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.function.Consumer;

public interface Context extends Closeable {
	void setScene(Parent value);

	Stage newWindow(String title, Parent parent, @Nullable Stage stage);

	Stage getPrimaryStage();

	Repository getRepository();

	BookDownloadControl getBookDownloadControl();

	BorrowBooksControl getBorrowBooksControl();

	ManageBooksControl getManageBooksControl();

	ManageNotificationsControl getManageNotificationsControl();

	ManageProfileControl getManageProfileControl();

	ManageUsersControl getManageUsersControl();

	PublishBooksControl getPublishBooksControl();

	RequestBooksControl getRequestBooksControl();

	StatsControl getStatsControl();

	void addSecondTimerListener(Object key, Consumer<? super ActionEvent> listener);

	@Nullable
	Tuple2<User, User.Data> getLoggedInUser();

	@SuppressWarnings("EmptyMethod")
	void setLoggedInUser(@Nullable Tuple2<library.models.User, library.models.User.Data> loggedInUser);

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
