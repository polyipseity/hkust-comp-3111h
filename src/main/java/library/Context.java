package library;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import library.controls.ManageProfileControl;
import library.models.User;
import library.persistence.Repository;
import library.utils.Tuple2;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public class Context implements Closeable {
	@NotNull
	public final Stage primaryStage;
	@NotNull
	public final Repository repository;
	@NotNull
	public final ManageProfileControl manageProfile;

	@Getter
	@Setter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

	public Context(@NotNull Stage primaryStage, @NotNull Repository repository) {
		this.primaryStage = primaryStage;
		this.repository = repository;
		this.manageProfile = new ManageProfileControl(repository);
	}

	@Override
	public void close() {
		repository.close();
	}

	public void setScene(@NotNull Parent value) {
		final var oldScene = primaryStage.getScene();
		primaryStage.setScene(new Scene(value, oldScene.getWidth(), oldScene.getHeight()));
	}
}
