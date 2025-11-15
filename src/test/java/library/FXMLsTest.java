package library;

import javafx.application.Platform;
import javafx.scene.Parent;
import library.models.User;
import library.utils.Tuple2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class FXMLsTest {

	@BeforeAll
	static void setUpAll() {
		try {
			Platform.startup(() -> {
			});   // starts the toolkit
		} catch (IllegalStateException _) {
		}
		@SuppressWarnings("SuspiciousInvocationHandlerImplementation") final var context = (Context) Proxy.newProxyInstance(FXMLsTest.class.getClassLoader(), new Class[]{Context.class}, ((_, method, _) -> {
			if ("getLoggedInUser".equals(method.getName())) {
				return new Tuple2<>(new User("username"), new User.Data(User.Role.values()[0], true, "password", "full name"));
			}
			return null;
		}));
		Main.setContext(context);
	}

	@AfterAll
	static void tearDownAll() {
		Main.getContext().close();
		Main.setContext(null);
	}

	@Test
	@DisplayName("All FXML files should load without error")
	void allFXMLFilesLoad() {
		for (final var fxml : FXMLs.values()) {
			try {
				// The generic type is Parent because most FXML roots are subclasses of it.
				final var root = fxml.load();
				assertNotNull(root, "Loaded root for %s should not be null".formatted(fxml));
				assertInstanceOf(Parent.class, root, "Loaded root for %s should be an instance of `Parent`".formatted(fxml));
			} catch (IOException e) {
				fail("Failed to load FXML file for %s".formatted(fxml), e);
			}
		}
	}
}
