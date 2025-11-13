package library;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import library.models.User;
import library.persistence.Repository;
import library.utils.Tuple2;
import lombok.Data;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

@Data
public class Context implements Closeable {
	@NotNull
	private final Stage primaryStage;
	@NotNull
	private final Repository repository;
	@Setter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

	public void setScene(@NotNull Parent value) {
		final var oldScene = primaryStage.getScene();
		primaryStage.setScene(new Scene(value, oldScene.getWidth(), oldScene.getHeight()));
	}

	@Override
	public void close() {
		repository.close();
	}
}
