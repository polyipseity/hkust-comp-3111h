package library;

import javafx.fxml.FXMLLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

public enum FXMLs {
	AUTHOR_DASHBOARD("/fxml/author/AuthorDashboard.fxml"),
	HOME("/fxml/Home.fxml"),
	LIBRARIAN_DASHBOARD("/fxml/LibrarianDashboard.fxml"),
	LOGIN("/fxml/Login.fxml"),
	REGISTER("/fxml/Register.fxml"),
	STUDENT_DASHBOARD("/fxml/student_staff/StudentDashboard.fxml"),
	;

	@NotNull
	public final URL resource;

	FXMLs(String name) {
		this.resource = Objects.requireNonNull(getClass().getResource(name));
	}

	@NotNull
	public <T> T load(@NotNull Consumer<FXMLLoader> callback) throws IOException {
		final var loader = new FXMLLoader(resource);
		final var ret = loader.<T>load();
		callback.accept(loader);
		return ret;
	}

	@NotNull
	public <T> T load() throws IOException {
		return load(_ -> {
		});
	}
}
