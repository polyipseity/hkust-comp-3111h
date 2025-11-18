package library.controllers.student_staff;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import library.Context;
import library.Main;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.utils.Alerts;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
			long s = Duration.between(borrow.borrowDate(), ZonedDateTime.now()).getSeconds();
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
		tableRow currentRow = table.getSelectionModel().getSelectedItem();
		if (currentRow == null) {
			Alerts.showErrorDialog("Please select a book first.");
			return;
		}

		Book selectedBook = currentRow.book;
		Optional<Borrow> borrowData = repository.borrowOps.read(user, selectedBook);
		if (borrowData.isEmpty()) {
			Alerts.showErrorDialog("Borrow data not found.");
			return;
		}
		String path = borrowData.get().pdfPath();

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
				"/fxml/student_staff/BookViewer.fxml"));
		BookViewerController controller = new BookViewerController(path);
		fxmlLoader.setController(controller);

		try {
			BorderPane borderPane = fxmlLoader.load();
			Stage stage = new Stage();
			stage.setScene(new Scene(borderPane));
			stage.setTitle("Reading: " + selectedBook.title() + " by " + selectedBook.author());
			stage.setOnShown(controller::createResizeListeners);
			stage.setOnCloseRequest(controller::disposeController);
			stage.show();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
