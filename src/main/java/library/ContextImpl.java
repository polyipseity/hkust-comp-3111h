package library;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.utils.ThrowingFunction;
import library.utils.Tuple2;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class ContextImpl implements Context {
	public static final double WINDOW_INITIAL_WIDTH = 640;
	public static final double WINDOW_INITIAL_HEIGHT = 480;

	@Getter
	private final Stage primaryStage;
	@Getter
	private final Repository repository;
	@Getter
	private final BookDownloadControl bookDownloadControl;
	@Getter
	private final BorrowBooksControl borrowBooksControl;
	@Getter
	private final ManageBooksControl manageBooksControl;
	@Getter
	private final ManageNotificationsControl manageNotificationsControl;
	@Getter
	private final ManageProfileControl manageProfileControl;
	@Getter
	private final ManageUsersControl manageUsersControl;
	@Getter
	private final PublishBooksControl publishBooksControl;
	@Getter
	private final RequestBooksControl requestBooksControl;
	@Getter
	private final StatsControl statsControl;

	private final Timeline secondTimeline;
	private final WeakHashMap<Object, Consumer<? super ActionEvent>> secondTimelineListeners = new WeakHashMap<>();
	private final Timeline minuteTimeline;
	private final WeakHashMap<Object, Consumer<? super ActionEvent>> minuteTimelineListeners = new WeakHashMap<>();

	@Setter
	@Getter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

	public ContextImpl(Stage primaryStage, Repository repository) {
		this.primaryStage = primaryStage;
		this.repository = repository;
		this.bookDownloadControl = new BookDownloadControl();
		this.borrowBooksControl = new BorrowBooksControl(repository);
		this.manageBooksControl = new ManageBooksControl(repository);
		this.manageNotificationsControl = new ManageNotificationsControl();
		this.manageProfileControl = new ManageProfileControl(repository);
		this.manageUsersControl = new ManageUsersControl(repository);
		this.publishBooksControl = new PublishBooksControl(repository);
		this.requestBooksControl = new RequestBooksControl(repository);
		this.statsControl = new StatsControl();

		final var secondTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event ->
				secondTimelineListeners.values().forEach(listener -> listener.accept(event))));
		secondTimeline.setCycleCount(Animation.INDEFINITE);
		secondTimeline.play();
		this.secondTimeline = secondTimeline;

		final var minuteTimeline = new Timeline(new KeyFrame(Duration.minutes(1), event ->
				minuteTimelineListeners.values().forEach(listener -> listener.accept(event))));
		minuteTimeline.setCycleCount(Animation.INDEFINITE);
		minuteTimeline.play();
		this.minuteTimeline = minuteTimeline;
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
		secondTimeline.stop();
		repository.close();
	}

	@Override
	public void setScene(Parent value) {
		final var oldScene = primaryStage.getScene();
		primaryStage.setScene(new Scene(value, oldScene.getWidth(), oldScene.getHeight()));
	}

	@Override
	public <E extends Throwable> Stage newWindow(String title, ThrowingFunction<? super Stage, ? extends Parent, ? extends E> parentSupplier, @Nullable Stage stage) throws E {
		stage = stage == null ? new Stage() : stage;
		stage.setScene(new Scene(parentSupplier.apply(stage), WINDOW_INITIAL_WIDTH, WINDOW_INITIAL_HEIGHT));
		stage.setTitle(title);
		stage.setResizable(true);
		return stage;
	}

	@Override
	public void addSecondTimelineListener(Object key, Consumer<? super ActionEvent> listener) {
		secondTimelineListeners.put(key, listener);
	}

	@Override
	public void addMinuteTimelineListener(Object key, Consumer<? super ActionEvent> listener) {
		minuteTimelineListeners.put(key, listener);
	}
}
