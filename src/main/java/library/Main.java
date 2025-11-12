package library;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import library.persistence.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DBMaker;

import java.util.Objects;

public class Main extends Application {
	/**
	 * The primary JavaFX Stage, made available statically.
	 */
	@Nullable
	private static Stage primaryStage = null;

	@Nullable
	private static Repository repository = null;

	@NotNull
	public static Stage getPrimaryStage() {
		return Objects.requireNonNull(primaryStage);
	}

	@NotNull
	public static Repository getRepository() {
		return Objects.requireNonNull(repository);
	}

	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		// Show the Home (role‐select) first, not the Login screen directly:
		Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Home.fxml")));
		stage.setTitle("Library Management System");
		stage.setScene(new Scene(root, 640, 480));
		stage.show();
	}

	@Override
	public void init() throws Exception {
		super.init();
		repository = new Repository(() -> DBMaker.fileDB("repository.db"));
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		primaryStage = null;
		try (final var _ = repository) {
			repository = null;
		}
	}
}
