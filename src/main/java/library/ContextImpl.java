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

public final class ContextImpl implements Context {
	@Getter
	@NotNull
	private final Stage primaryStage;
	@Getter
	@NotNull
	private final Repository repository;
	@Getter
	@NotNull
	private final ManageProfileControl manageProfile;

	@Setter
	@Getter
	@Nullable
	private Tuple2<User, User.Data> loggedInUser;

	public ContextImpl(@NotNull Stage primaryStage, @NotNull Repository repository) {
		this.primaryStage = primaryStage;
		this.repository = repository;
		this.manageProfile = new ManageProfileControl(repository);
	}

	@Override
	public void close() {
		getRepository().close();
	}

	@Override
	public void setScene(@NotNull Parent value) {
		final var oldScene = getPrimaryStage().getScene();
		getPrimaryStage().setScene(new Scene(value, oldScene.getWidth(), oldScene.getHeight()));
	}
}
