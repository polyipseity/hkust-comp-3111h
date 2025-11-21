package library.controllers.student_staff;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import library.Context;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.BorrowBooksControl;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public final class BorrowedBooksController implements RequiresLoggedIn {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();
	private final User user =  this.getLoggedInUser()._1();

	@Setter
	private DashboardController parentController;

	@FXML
	private TableView<@Nullable tableRow> table;
	@FXML
	private TableColumn<tableRow, @Nullable String> titleCol, authorCol, borrowedOnCol, timeLeftCol, actionsCol;

	public record tableRow(
			@Getter String title,
			@Getter String author,
			@Getter String borrowedOn,
			@Getter String timeLeft,
			Book book
	) {
	}

	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	class returnBtnCell extends TableCell<tableRow, String>{
		final Button btn = new Button("Return");
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			setText(null);
			if (empty) setGraphic(null);
			else {
				Book book = getTableRow().getItem().book();
				btn.setOnAction(_ -> returnButtonAction(book));
				setGraphic(btn);
			}
		}
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
		borrowedOnCol.setCellValueFactory(new PropertyValueFactory<>("borrowedOn"));
		timeLeftCol.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));

		actionsCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		actionsCol.setCellFactory(_ -> new returnBtnCell());

		reload();
	}

	public void reload() {
		ObservableList<tableRow> data = FXCollections.observableArrayList();
		Map<Book, Borrow> borrowedBooksMap = repository.borrowOps.read(getLoggedInUser()._1());
		for (var entry: borrowedBooksMap.entrySet()) {
			Book book = entry.getKey();
			Borrow borrow = entry.getValue();
			long s = borrow.durationLeft().getSeconds();
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
	private void readSelectedBook() throws IOException {
		tableRow currentRow = table.getSelectionModel().getSelectedItem();
		if (currentRow == null) {
			Alerts.showErrorDialog("Please select a book first.");
			return;
		}

		Book selectedBook = currentRow.book;
		String title = currentRow.title;
		String author = currentRow.author;

		switch (context.getBorrowBooksControl().readBook(user, selectedBook)) {
			case BorrowBooksControl.ReadResult.Success(String path) -> displayPdfFile(path, title, author);
			case BorrowBooksControl.ReadResult.NewPdfGenerated(String path) -> {
				Alerts.showInfoDialog("PDF file not found, generating a new one...");
				displayPdfFile(path, title, author);
			}
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}

	private void displayPdfFile(String path, String title, String author) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
				"/fxml/student_staff/BookViewer.fxml"));
		BookViewerController controller = new BookViewerController(path);
		fxmlLoader.setController(controller);
		BorderPane borderPane = fxmlLoader.load();
		
		Stage stage = new Stage();
		stage.setScene(new Scene(borderPane));
		stage.setTitle("Reading: " + title + " by " + author);
		stage.setOnShown(controller::createResizeListeners);
		stage.setOnCloseRequest(controller::disposeController);
		stage.show();
	}

	/**
	 * Executes when the "Return" button of the table in the "My Borrowed Books" tab is pressed.
	 * @param book The book to be returned by the current user,
	 *                which should be the book pointed to by the button's table row.
	 */
	private void returnButtonAction(Book book) {
		BorrowBooksControl.ReturnResult result;
		try {
			result = context.getBorrowBooksControl().returnBook(user, book);
		} catch (TransactionException e) {
			Alerts.showErrorDialog("Unknown error occurred: " + e.getMessage());
			return;
		}
		switch (result) {
			case BorrowBooksControl.ReturnResult.Success _ -> Alerts.showInfoDialog("Book returned successfully");
			case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
		}
	}
}
