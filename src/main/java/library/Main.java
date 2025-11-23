package library;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.persistence.Repository;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DBMaker;

import java.util.Objects;

/**
 * The Main class serves as the entry point for launching a JavaFX application.
 * It extends the {@link Application} class and initializes the application context.
 * This class is responsible for setting up the primary stage and loading the
 * initial scene of the application.
 * <p>
 * This class manages a static {@link Context} instance that provides various
 * application-wide services and utilities. The {@link #getContext()} method
 * ensures access to the non-null application context, while the {@link #start(Stage)}
 * and {@link #stop()} methods are lifecycle hooks for the JavaFX application.
 */
public final class Main extends Application {
	@Nullable
	@Setter
	private static Context context = null;

    /**
     * Retrieves the application-wide {@link Context} instance.
     * This method ensures that the context is not null and throws a {@link NullPointerException}
     * if the context has not been initialized.
     *
     * @return the non-null {@link Context} instance representing the application's state and services.
     * @throws NullPointerException if the context is null.
     */
    public static Context getContext() {
		return Objects.requireNonNull(context);
	}

	/**
     * Initializes and starts the JavaFX application by setting up the application
     * context and displaying the primary stage with the initial scene.
     *
     * @param stage the primary stage for this application, onto which scenes can
     *              be set. Applications may create other stages, if needed, but
     *              they will not be primary stages.
     * @throws Exception if an error occurs during the initialization or start-up
     *                   process of the application.
     */
    @Override
	public void start(Stage stage) throws Exception {
		context = new ContextImpl(stage, new Repository(DBMaker.fileDB("repository.db")));
		// Show the Home (role‐select) first, not the Login screen directly:
		getContext().newWindow("Library Management System", FXMLs.HOME.<Parent>load(), stage);
		stage.show();
	}

	/**
     * Stops the JavaFX application by performing cleanup operations.
     *
     * This method is executed during the shutdown phase of the application lifecycle
     * and is responsible for releasing resources associated with the application context.
     * It ensures that the application-wide {@link Context} is properly closed and set to null.
     *
     * The method utilizes try-with-resources to guarantee that the {@link Context} is closed
     * safely, allowing any resources held by the context to be released.
     *
     * If the context is already null or an exception occurs during the execution of the
     * context's {@link Context#close()} method, this method ensures proper handling to
     * avoid further impact on the application's shutdown process.
     */
    @Override
	public void stop() {
		try (final var _ = context) {
			context = null;
		}
	}
}
