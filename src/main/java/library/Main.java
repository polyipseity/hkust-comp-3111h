package library;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.persistence.Repository;
import lombok.AccessLevel;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DBMaker;

import java.util.Objects;

public final class Main extends Application {
	@Nullable
	@Setter(AccessLevel.PACKAGE)
	private static Context context = null;

	public static Context getContext() {
		return Objects.requireNonNull(context);
	}

	@Override
	public void start(Stage stage) throws Exception {
		context = new ContextImpl(stage, new Repository(DBMaker.fileDB("repository.db")));
		// Show the Home (role‐select) first, not the Login screen directly:
		getContext().newWindow("Library Management System", FXMLs.HOME.<Parent>load(), stage);
		stage.show();
	}

	@Override
	public void stop() {
		try (final var _ = context) {
			context = null;
		}
	}
}
