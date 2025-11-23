package library;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;
import library.controls.*;
import library.models.User;
import library.persistence.Repository;
import library.utils.ThrowingFunction;
import library.utils.Tuple2;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DBMaker;

import java.util.function.Consumer;

@RequiredArgsConstructor
public final class FakeContext implements Context {
	@Getter
	private final boolean testing;
	@Getter
	private final Repository repository = new Repository(DBMaker.memoryDirectDB());
	private final AIServiceControl aiServiceControl = new AIServiceControl();
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
	public AIServiceControl getAIServiceControl() {
		return aiServiceControl;
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
	public Object getLoggedInToken() {
		return new Object();
	}

	@Override
	public Tuple2<User, User.Data> getLoggedInUser() {
		final var user = new User("reader");
		return new Tuple2<>(user, repository.userOps.readOrThrow(user));
	}

	@Override
	public void setLoggedInUser(@Nullable Tuple2<User, User.Data> loggedInUser) {
		// noop
	}

	@Override
	public void close() {
		repository.close();
	}
}
