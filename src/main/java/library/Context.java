package library;

import javafx.scene.Parent;
import javafx.stage.Stage;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public interface Context extends Closeable {
	void setScene(@NotNull Parent value);

	@NotNull
	Stage newWindow(@NotNull String title, @NotNull Parent parent, @Nullable Stage stage);

	@NotNull Stage getPrimaryStage();

	@NotNull Repository getRepository();

	@NotNull BookDownloadControl getBookDownloadControl();

	@NotNull BorrowBooksControl getBorrowBooksControl();

	@NotNull ManageBooksControl getManageBooksControl();

	@NotNull ManageNotificationsControl getManageNotificationsControl();

	@NotNull ManageProfileControl getManageProfileControl();

	@NotNull ManageUsersControl getManageUsersControl();

	@NotNull PublishBooksControl getPublishBooksControl();

	@NotNull RequestBooksControl getRequestBooksControl();

	@NotNull StatsControl getStatsControl();

	@Nullable
	Tuple2<User, User.Data> getLoggedInUser();

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
