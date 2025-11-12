package library;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import library.persistence.Repository;
import lombok.Getter;
import org.mapdb.DBMaker;

import java.util.Objects;

public class Main extends Application {

	/**
	 * The primary JavaFX Stage, made available statically.
	 */
	@Getter
	private static Stage primaryStage;

	@Getter
	private static Repository repository;

	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		try (var repository = new Repository(() -> DBMaker.fileDB("repository.db"))) {
			Main.repository = repository;
			// Show the Home (role‐select) first, not the Login screen directly:
			Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Home.fxml")));
			stage.setTitle("Library Management System");
			stage.setScene(new Scene(root, 640, 480));
			stage.show();
		}
	}
}
