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
	@Getter
	@NotNull
	private final Stage primaryStage;
	@Getter
	@NotNull
	private final Repository repository;
	@Getter
	@NotNull
	private final ManageBooksControl manageBooksControl;
	@Getter
	@NotNull
	private final ManageBorrowControl manageBorrowsControl;
	@Getter
	@NotNull
	private final ManageProfileControl manageProfileControl;
	@Getter
	@NotNull
	private final ManageRequestControl manageRequestsControl;
	@Getter
	@NotNull
	private final ManageUserReadControl manageUserReadsControl;

	@Setter
	@Getter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

	public ContextImpl(@NotNull Stage primaryStage, @NotNull Repository repository) {
		this.primaryStage = primaryStage;
		this.repository = repository;
		this.manageBooksControl = new ManageBooksControl(repository);
		this.manageBorrowsControl = new ManageBorrowControl(repository);
		this.manageProfileControl = new ManageProfileControl(repository);
		this.manageRequestsControl = new ManageRequestControl(repository);
		this.manageUserReadsControl = new ManageUserReadControl(repository);
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
}
