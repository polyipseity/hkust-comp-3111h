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
	@Nullable
	private static Context context = null;

	@NotNull
	public static Context getContext() {
		return Objects.requireNonNull(context);
	}

	@Override
	public void start(Stage stage) throws Exception {
		context = new Context(stage, new Repository(() -> DBMaker.fileDB("repository.db")));
		// Show the Home (role‐select) first, not the Login screen directly:
		final var root = FXMLLoader.<Parent>load(Objects.requireNonNull(getClass().getResource("/fxml/Home.fxml")));
		stage.setTitle("Library Management System");
		stage.setScene(new Scene(root, 640, 480));
		stage.show();
	}

	@Override
	public void stop() {
		try (final var _ = context) {
			context = null;
		}
	}
}
