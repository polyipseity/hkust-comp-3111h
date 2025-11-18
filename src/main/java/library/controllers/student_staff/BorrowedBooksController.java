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
import library.utils.Dates;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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
			@NotNull @Getter String borrowedOn,
			@NotNull @Getter String timeLeft,
			@NotNull Book book
	) {
	}

	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@FXML
	private void initialize() {
		titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
		borrowedOnCol.setCellValueFactory(new PropertyValueFactory<>("borrowedOn"));
		timeLeftCol.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));

		reload();
	}

	private void reload() {
		ObservableList<tableRow> data = FXCollections.observableArrayList();
		Map<Book, Borrow> borrowedBooksMap = repository.borrowOps.read(user);
		for (var entry: borrowedBooksMap.entrySet()) {
			Book book = entry.getKey();
			Borrow borrow = entry.getValue();
			long s = Duration.between(borrow.borrowDate(), Dates.nowZoned()).getSeconds();
			data.add(new tableRow(
					book.title(),
					book.author().toString(),
					borrow.borrowDate().format(dateTimeFormatter),
					String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60)),
					book
			));
		}
		table.setItems(data);
	}

	@FXML
	private void readSelectedBook() {
	}
}
