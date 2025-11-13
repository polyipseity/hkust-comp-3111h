package library;

import javafx.application.Application;
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
		stage.setScene(new Scene(FXMLs.HOME.load(), 640, 480));
		stage.setResizable(true);
		stage.show();
	}

	@Override
	public void stop() {
		try (final var _ = context) {
			context = null;
		}
	}
}
