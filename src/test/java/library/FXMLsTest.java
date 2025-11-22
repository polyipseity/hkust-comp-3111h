package library;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.ThrowingFunction;
import library.utils.Tuple2;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.util.function.Consumer;

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
			abort(ex.getLocalizedMessage());
		}
		final var context = new Context() {
			@Getter
			private final Repository repository = new Repository(DBMaker.memoryDirectDB());
			@Getter
			private final BookDownloadControl bookDownloadControl = new BookDownloadControl();
			@Getter
			private final BorrowBooksControl borrowBooksControl = new BorrowBooksControl(repository);
			@Getter
			private final ManageBooksControl manageBooksControl = new ManageBooksControl(repository);
			@Getter
			private final ManageNotificationsControl manageNotificationsControl = new ManageNotificationsControl();
			@Getter
			private final ManageProfileControl manageProfileControl = new ManageProfileControl(repository);
			@Getter
			private final ManageUsersControl manageUsersControl = new ManageUsersControl(repository);
			@Getter
			private final PublishBooksControl publishBooksControl = new PublishBooksControl(repository);
			@Getter
			private final RequestBooksControl requestBooksControl = new RequestBooksControl(repository);
			@Getter
			private final StatsControl statsControl = new StatsControl();

			@Getter
			private final Tuple2<User, User.Data> loggedInUser = new Tuple2<>(new User("username"), new User.Data(User.Role.values()[0], true, "password", "full name"));

			{
				repository.userOps.create(loggedInUser._1(), loggedInUser._2());
			}

			@Override
			public void setScene(Parent value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <E extends Throwable> Stage newWindow(String title, ThrowingFunction<? super Stage, ? extends Parent, ? extends E> parentSupplier, @Nullable Stage stage) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Stage getPrimaryStage() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void addSecondTimelineListener(Object key, Consumer<? super ActionEvent> listener) {
				// noop
			}

			@Override
			public void addMinuteTimelineListener(Object key, Consumer<? super ActionEvent> listener) {
				// noop
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
