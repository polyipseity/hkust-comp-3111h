package library;

import javafx.fxml.FXMLLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public enum FXMLs {
	AUTHOR_DASHBOARD("/fxml/AuthorDashboard.fxml"),
	HOME("/fxml/Home.fxml"),
	LIBRARIAN_DASHBOARD("/fxml/LibrarianDashboard.fxml"),
	LOGIN("/fxml/Login.fxml"),
	REGISTER("/fxml/Register.fxml"),
	STUDENT_DASHBOARD("/fxml/StudentDashboard.fxml"),
	;

	@NotNull
	public final URL resource;

	FXMLs(String name) {
		this.resource = Objects.requireNonNull(getClass().getResource(name));
	}

	@NotNull
	public FXMLLoader loader() {
		return new FXMLLoader(resource);
	}

	@NotNull
	public <T> T load() throws IOException {
		return loader().load();
	}
}
