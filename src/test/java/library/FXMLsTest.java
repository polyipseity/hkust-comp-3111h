package library;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.controls.ManageBooksControl;
import library.controls.ManageBorrowControl;
import library.controls.ManageProfileControl;
import library.controls.ManageRequestControl;
import library.controls.ManageUserReadControl;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Tuple2;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;

class FXMLsTest {
	private static boolean unsupported = false;

	@BeforeAll
	static void setUpAll() throws TransactionException {
		try {
			Platform.startup(() -> {
			});   // starts the toolkit
		} catch (IllegalStateException _) {
		} catch (UnsupportedOperationException ex) {
			// java.lang.UnsupportedOperationException: Unable to open DISPLAY
			unsupported = true;
			abort(ex.getMessage());
		}
		final var context = new Context() {
			@Getter
			@NotNull
			private final Repository repository = new Repository(DBMaker.memoryDirectDB());
			@Getter
			@NotNull
			private final ManageBooksControl manageBooksControl = new ManageBooksControl(repository);
			@Getter
			@NotNull
			private final ManageProfileControl manageProfileControl = new ManageProfileControl(repository);
			@Getter
			private final ManageRequestControl manageRequests = new ManageRequestControl(repository);
			@Getter
			private final ManageBorrowControl manageBorrows = new ManageBorrowControl(repository);
			@Getter
			private final ManageUserReadControl manageUserReads = new ManageUserReadControl(repository);

			@Getter
			@NotNull
			private final Tuple2<User, User.Data> loggedInUser = new Tuple2<>(new User("username"), new User.Data(User.Role.values()[0], true, "password", "full name"));

			{
				repository.userOps.create(loggedInUser._1(), loggedInUser._2());
			}

			@Override
			public void setScene(@NotNull Parent value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public @NotNull Stage getPrimaryStage() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setLoggedInUser(@Nullable Tuple2<User, User.Data> loggedInUser) {
				// noop
			}

			@Override
			public void close() {
				repository.close();
			}
		};
		Main.setContext(context);
	}

	@AfterAll
	static void tearDownAll() {
		if (unsupported) {
			return;
		}
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
