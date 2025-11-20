package library;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.utils.Tuple2;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ContextImpl implements Context {
	public static final double WINDOW_INITIAL_WIDTH = 640;
	public static final double WINDOW_INITIAL_HEIGHT = 480;

	@Getter
	@NotNull
	private final Stage primaryStage;
	@Getter
	@NotNull
	private final Repository repository;
	@Getter
	@NotNull
	private final BorrowBooksControl borrowBooksControl;
	@Getter
	@NotNull
	private final ManageBooksControl manageBooksControl;
	@Getter
	@NotNull
	private final ManageNotificationsControl manageNotificationsControl;
	@Getter
	@NotNull
	private final ManageProfileControl manageProfileControl;
	@Getter
	@NotNull
	private final ManageUsersControl manageUsersControl;
	@Getter
	@NotNull
	private final PublishBooksControl publishBooksControl;
	@Getter
	@NotNull
	private final RequestBooksControl requestBooksControl;
	@Getter
	@NotNull
	private final StatsControl statsControl;

	@Setter
	@Getter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

	public ContextImpl(@NotNull Stage primaryStage, @NotNull Repository repository) {
		this.primaryStage = primaryStage;
		this.repository = repository;
		this.borrowBooksControl = new BorrowBooksControl(repository);
		this.manageBooksControl = new ManageBooksControl(repository);
		this.manageNotificationsControl = new ManageNotificationsControl();
		this.manageProfileControl = new ManageProfileControl(repository);
		this.manageUsersControl = new ManageUsersControl(repository);
		this.publishBooksControl = new PublishBooksControl();
		this.requestBooksControl = new RequestBooksControl(repository);
		this.statsControl = new StatsControl();
	}

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
	public void close() {
		getRepository().close();
	}

	@Override
	public void setScene(@NotNull Parent value) {
		final var oldScene = getPrimaryStage().getScene();
		getPrimaryStage().setScene(new Scene(value, oldScene.getWidth(), oldScene.getHeight()));
	}

	@Override
	public @NotNull Stage newWindow(@NotNull String title, @NotNull Parent parent, @Nullable Stage stage) {
		stage = stage == null ? new Stage() : stage;
		stage.setScene(new Scene(parent, WINDOW_INITIAL_WIDTH, WINDOW_INITIAL_HEIGHT));
		stage.setTitle(title);
		stage.setResizable(true);
		return stage;
	}
}
