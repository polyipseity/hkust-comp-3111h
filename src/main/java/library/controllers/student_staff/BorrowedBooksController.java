package library.controllers.student_staff;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import library.Context;
import library.Main;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

public class BorrowedBooksController {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();
	private final User user = context.getLoggedInUser()._1();
	private final User.Data userData = context.getLoggedInUser()._2();

	@FXML
	private TableView<tableRow> table;
	@FXML
	private TableColumn<tableRow, String> titleCol, authorCol, borrowedOnCol, timeLeftCol;
	@FXML
	private TableColumn<tableRow, Button> actionsCol;

	public record tableRow(
			@NotNull @Getter String title,
			@NotNull @Getter String author,
			@NotNull @Getter ZonedDateTime borrowedOn,
			@NotNull @Getter Duration timeLeft
	) {
	}

	@FXML
	private void initialize() {
		titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
		borrowedOnCol.setCellValueFactory(new PropertyValueFactory<>("borrowedOn"));
		timeLeftCol.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));

		ObservableList<tableRow> data = FXCollections.observableArrayList();

		Map<Book, Borrow> borrowedBooksMap = repository.borrowOps.read(user);
		for (var entry: borrowedBooksMap.entrySet()) {
			Book book = entry.getKey();
			Borrow borrow = entry.getValue();
			@NotNull Optional<Book.Data> bookData = repository.bookOps.read(book);
			// data.add(new tableRow(...));
		}
	}
}
