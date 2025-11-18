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
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.ByteArray;
import library.utils.TimeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class AvailableBooksController {
    private final Context context = Main.getContext();
    private final Repository repository = context.getRepository();
    private final User user = context.getLoggedInUser()._1();
    private final User.Data userData = context.getLoggedInUser()._2();

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

    @FXML
    public void initialize() {
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
        Map<Book, Book.Data> availableBooks = repository.bookOps.read(this::checkBorrowable);
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

    private boolean checkBorrowable(Map.Entry<Book, Book.Data> entry) {
        Book book = entry.getKey();
        Book.Data bookData = entry.getValue();
        return repository.borrowOps.read(user, book).isEmpty();
    }

    /**
     * Runs each time the "Borrow Book" button is pressed.
     */
    @FXML
    private void borrowButtonAction() {
        tableRow currentRow = table.getSelectionModel().getSelectedItem();
        if (currentRow == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
        Book selectedBook = currentRow.book;
        Optional<Book.Data> selectedBookData = repository.bookOps.read(selectedBook);

        // Try to get the borrow duration for the book
        Dialog<dialogResult> dialog = createDialog();
        int minutes, seconds, durationSeconds;
        int durationUpperBound = 1 + 14 * 24 * 60 * 60;
        int durationLowerBound = 1;

        while (true) {
            Optional<dialogResult> result = dialog.showAndWait();
            if (result.isEmpty()) return; // Exit loop if user clicks "Cancel"

            try {
                minutes = Integer.parseInt(result.get().minutes);
                seconds = Integer.parseInt(result.get().seconds);
            } catch (NumberFormatException e) {
                Alerts.showErrorDialog("Entered values could not be parsed correctly.");
                continue;
            }
            if (minutes < 0 || seconds < 0) {
                Alerts.showErrorDialog("At least one of the entered values are negative.");
                continue;
            }
            durationSeconds = minutes * 60 + seconds;
            if (durationSeconds > durationUpperBound) {
                Alerts.showErrorDialog("Entered duration exceeds upper limit of 14 days.");
                continue;
            }
            if (durationSeconds < durationLowerBound) {
                Alerts.showErrorDialog("Entered duration exceeds lower limit of 1 second.");
                continue;
            }
            break; // Exit loop when valid duration obtained
        }

        Borrow borrowData = new Borrow(
		        TimeUtil.nowZoned(),
                Duration.ofSeconds(durationSeconds),
                new ByteArray(new byte[0])
        );
        try {
            repository.borrowOps.create(user, selectedBook, borrowData);
        } catch (TransactionException e) {
            throw new RuntimeException(e);
        }
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

        dialog.setResultConverter(new Callback<>() {
            @Override
            public dialogResult call(ButtonType b) {
                if (b == buttonTypeOk)
                    return new dialogResult(minutesTextField.getText(), secondsTextField.getText());
                else return null;
            }
        });

        return dialog;
    }
}
