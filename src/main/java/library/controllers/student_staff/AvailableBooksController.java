package library.controllers.student_staff;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Callback;
import library.Context;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.ManageBorrowControl;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class AvailableBooksController implements RequiresLoggedIn {
    private final Context context = Main.getContext();
    private final User user = this.getLoggedInUser()._1();

    @FXML
    private Text titleText, authorText, publishDateText, summaryText;
    @FXML
    private TableView<tableRow> table;
    @FXML
    private TableColumn<tableRow, String> titleCol, authorCol, publishDateCol, summaryCol;

    public record tableRow(
            @NotNull @Getter String title,
            @NotNull @Getter String author,
            @NotNull @Getter String publishDate,
            @NotNull @Getter String summary,
            @NotNull Book book
    ) {
    }

    public record dialogResult(String minutes, String seconds) {
    }

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
		publishDateCol.setCellValueFactory(new PropertyValueFactory<>("publishDate"));
		summaryCol.setCellValueFactory(new PropertyValueFactory<>("summary"));

		table.getSelectionModel().selectedItemProperty().addListener(
				(_, _, newValue) -> {
					if (newValue != null) {
						titleText.setText(newValue.getTitle());
						authorText.setText(newValue.getAuthor());
						publishDateText.setText(newValue.getPublishDate());
						summaryText.setText(newValue.getSummary());
					}
				}
		);

		reload();
	}

	private void reload() {
        ObservableList<tableRow> data = FXCollections.observableArrayList();
        Map<Book, Book.Data> availableBooks = context.getManageBorrows().availableBooks(user);
        for (var entry : availableBooks.entrySet()) {
            Book book = entry.getKey();
            Book.Data bookData = entry.getValue();
            String publishDateString =
                    bookData.publishDate() == null ? "N/A" : bookData.publishDate().format(dateTimeFormatter);
            data.add(new tableRow(book.title(), book.author().toString(),
                    publishDateString, bookData.summary(), book));
        }
        table.setItems(data);
        table.getSelectionModel().selectFirst();
    }

    /**
     * Runs each time the "Borrow Book" button is pressed.
     */
    @FXML
    private void borrowButtonAction() throws TransactionException {
        tableRow currentRow = table.getSelectionModel().getSelectedItem();
        if (currentRow == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
        Book selectedBook = currentRow.book;

        // Try to get the borrow duration for the book
        Dialog<dialogResult> dialog = createDialog();
        Optional<dialogResult> result = dialog.showAndWait();
        if (result.isEmpty()) return; // Exit if user clicks "Cancel"

        // Parse results from input dialog
        int minutes, seconds;
        try {
            minutes = Integer.parseInt(result.get().minutes);
            seconds = Integer.parseInt(result.get().seconds);
        } catch (NumberFormatException e) {
            Alerts.showErrorDialog("Entered values could not be parsed correctly.");
            return;
        }

        switch (context.getManageBorrows().borrowBook(user, selectedBook, minutes, seconds)) {
            case ManageBorrowControl.BorrowResult.Success _ -> Alerts.showInfoDialog("Book borrowed successfully");
            case HasMessage ret -> Alerts.showErrorDialog(ret.getMessage());
        }

        reload();
    }

    /**
     * @return A properly configured input dialog for entering the borrowing duration.
     */
    private Dialog<dialogResult> createDialog() {
        Dialog<dialogResult> dialog = new Dialog<>();
        dialog.setTitle("Borrow Duration");
        dialog.setHeaderText("Please enter the borrowing duration (minutes and seconds).");

        TextField minutesTextField = new TextField();
        TextField secondsTextField = new TextField();

        GridPane grid = new GridPane();
        grid.add(new Label("Minutes:"), 0, 0);
        grid.add(new Label("Seconds:"), 0, 1);
        grid.add(minutesTextField, 1, 0);
        grid.add(secondsTextField, 1, 1);
        grid.setHgap(10);
        grid.setVgap(10);
        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);
        dialog.setResultConverter(b ->
                b == buttonTypeOk ? new dialogResult(minutesTextField.getText(), secondsTextField.getText()) : null);

        return dialog;
    }
}
