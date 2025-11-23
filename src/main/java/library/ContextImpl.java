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
import library.persistence.TransactionException;
import library.utils.ThrowingFunction;
import library.utils.Tuple2;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Implementation of the {@link Context} interface that provides a centralized context
 * for managing application state, control modules, and UI interactions.
 * <p>
 * This class initializes and maintains various control modules related to application
 * functionality such as managing books, users, notifications, and more. It also
 * handles primary application stage, scene transitions, and timeline-based event handling.
 * <p>
 * The class supports both second-level and minute-level timelines, allowing consumers to
 * register listeners for periodic actions. It also manages logged-in user state and provides
 * mechanisms for interacting with repository operations.
 */
public final class ContextImpl implements Context {
    /**
     * The constant WINDOW_INITIAL_WIDTH.
     */
    public static final double WINDOW_INITIAL_WIDTH = 640;
    /**
     * The constant WINDOW_INITIAL_HEIGHT.
     */
    public static final double WINDOW_INITIAL_HEIGHT = 480;

	@Getter
	private final boolean testing = false;
	@Getter
	private final Stage primaryStage;
	@Getter
	private final Repository repository;
	private final AIServiceControl aiServiceControl;
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
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private final List<Object> loggedInTokens = new ArrayList<>();
	@Getter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

    /**
     * Constructs a new ContextImpl instance and initializes the associated controls, timelines,
     * and repository for the application context.
     *
     * @param primaryStage the primary stage of the application used for setting main window scenes
     * @param repository   the repository instance providing access to data and persistent storage
     */
    public ContextImpl(Stage primaryStage, Repository repository) {
		this.primaryStage = primaryStage;
		this.repository = repository;
		this.aiServiceControl = new AIServiceControl();
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
     * Sets the logged-in user and manages related state and behavior such as
     * token clearing or pruning operations for user-specific data.
     *
     * @param loggedInUser a tuple containing the logged-in user and their associated data.
     *                     It can be null to clear the logged-in user and reset related state.
     */
    @SuppressWarnings("CallToPrintStackTrace")
	@Override
	public void setLoggedInUser(@Nullable Tuple2<User, User.Data> loggedInUser) {
		this.loggedInUser = loggedInUser;
		if (loggedInUser == null) {
			loggedInTokens.clear();
		} else {
			addMinuteTimelineListener(Objects.requireNonNull(getLoggedInToken()), _ -> {
				try {
					repository.borrowOps.prune();
				} catch (TransactionException e) {
					e.printStackTrace(); // nothing we can do
				}
			});
		}
	}

	/**
     * Retrieves a unique token representing the current logged-in user.
     * If no user is logged in, this method returns null. Each token returned
     * is tracked internally for management purposes.
     *
     * @return an Object representing a unique token for the current logged-in user,
     *         or null if no user is currently logged in.
     */
    @Override
	public @Nullable Object getLoggedInToken() {
		if (loggedInUser == null) return null;
		final var ret = new Object();
		loggedInTokens.add(ret);
		return ret;
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
		minuteTimeline.stop();
		secondTimeline.stop();
		repository.close();
	}

	/**
     * Sets the scene for the primary stage with the specified content, maintaining
     * the existing stage width and height.
     *
     * @param value the root node of the new scene to be displayed in the primary stage
     */
    @Override
	public void setScene(Parent value) {
		final var oldScene = primaryStage.getScene();
		primaryStage.setScene(new Scene(value, oldScene.getWidth(), oldScene.getHeight()));
	}

	/**
     * Creates a new window (stage) with the specified title, using the provided parent node supplier,
     * and assigns a new scene to the stage. If the input stage is null, a new stage is created.
     * The stage is configured to be resizable and initializes its scene with fixed dimensions.
     *
     * @param <E>           the type of throwable that may be thrown by the parent node supplier
     * @param title         the title to set for the new window
     * @param parentSupplier a function that accepts the stage as input and supplies the root node
     *                        (of type {@link Parent}) to be used as the content of the scene
     * @param stage         the existing stage to use, or null to create a new stage
     * @return the created or updated {@link Stage} instance with the specified configuration
     * @throws E if the parent node supplier fails with an exception
     */
    @Override
	public <E extends Throwable> Stage newWindow(String title, ThrowingFunction<? super Stage, ? extends Parent, ? extends E> parentSupplier, @Nullable Stage stage) throws E {
		stage = stage == null ? new Stage() : stage;
		stage.setScene(new Scene(parentSupplier.apply(stage), WINDOW_INITIAL_WIDTH, WINDOW_INITIAL_HEIGHT));
		stage.setTitle(title);
		stage.setResizable(true);
		return stage;
	}

	/**
     * Retrieves the AIServiceControl instance associated with the current context.
     *
     * @return the AIServiceControl instance, which manages AI-related services in the application.
     */
    @Override
	public AIServiceControl getAIServiceControl() {
		return aiServiceControl;
	}

	/**
     * Adds a listener to the second timeline, which responds to specified timeline events.
     * The provided listener is associated with a unique key for identification and management.
     *
     * @param key       the unique identifier used to associate the listener; must not be null
     * @param listener  the listener to handle timeline events; must not be null
     */
    @Override
	public void addSecondTimelineListener(Object key, Consumer<? super ActionEvent> listener) {
		secondTimelineListeners.put(key, listener);
	}

	/**
     * Adds a listener to the minute timeline, which responds to events occurring on the timeline.
     * The provided listener is associated with a unique key for identification and management.
     *
     * @param key       the unique identifier used to associate the listener; must not be null
     * @param listener  the listener to handle timeline events; must not be null
     */
    @Override
	public void addMinuteTimelineListener(Object key, Consumer<? super ActionEvent> listener) {
		minuteTimelineListeners.put(key, listener);
	}
}
