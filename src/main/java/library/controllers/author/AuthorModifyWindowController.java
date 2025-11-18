package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AuthorModifyWindowController implements RequiresLoggedIn {
    Repository repository = Main.getContext().getRepository();
    private Book selectedBook;

    private String initialTitle, initalSummary, currentTitle, currentSummary;

    @FXML
    private Button saveButton;

    @FXML
    private TextArea SummaryArea;

    @FXML
    private TextField TitleField;

    public void setData(String title, String summary,Book book) {
        initialTitle = title;
        initalSummary = summary;
        TitleField.setText(title);
        SummaryArea.setText(summary);
        selectedBook = book;
    }

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		TitleField.textProperty().addListener((observable, oldValue, newValue) -> {
			currentTitle = newValue;
			checkSaveCondition(newValue, currentSummary);
		});

		SummaryArea.textProperty().addListener((observable, oldValue, newValue) -> {
			currentSummary = newValue;
			checkSaveCondition(currentTitle, newValue);
		});
	}

    private void checkSaveCondition(String title, String summary) {
	    saveButton.setDisable(initialTitle.equals(title) && initalSummary.equals(summary));
    }

    @FXML
    private void saveModification() throws TransactionException {
	    var book = new Book(currentTitle, new Author.ByRef(getLoggedInUser()._1()), true);
        if(currentTitle != initialTitle){
            Optional<Book.Data> opt = repository.bookOps.read(book);
            if(opt.isPresent()) {
                Alerts.showErrorDialog("Duplicated Book Title.");
                return;
            }
        }
        var Olddata = repository.bookOps.read(selectedBook).get();
        var data = new Book.Data(currentSummary,Olddata.content(), Book.ApprovalStatus.PENDING ,Olddata.publishDate(),selectedBook,Olddata.timesBorrowed());
        repository.bookOps.create(book, data);
        repository.bookOps.update(selectedBook, old -> old.withOriginalOrModified(book));

        //Close the current window
        Stage currentStage = (Stage) TitleField.getScene().getWindow();
        currentStage.close();
    }

    @FXML
    private void cancelModification(){
        Stage currentStage = (Stage) TitleField.getScene().getWindow();
        currentStage.close();
    }
}
