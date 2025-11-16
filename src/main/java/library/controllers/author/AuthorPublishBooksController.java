package library.controllers.author;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import library.Main;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class AuthorPublishBooksController {
    private final Repository repository = Main.getContext().getRepository();

    @FXML
    private TextField BookTitle, BookContent, BookAbstract;
    private String ContentTxt;

    //Method for choosing text file
    @FXML
    private void ChooseTextFile() throws IOException {
        FileChooser fileChooser = new FileChooser();

        // Set file filter for .txt files
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                "Text files", "*.txt");
        fileChooser.getExtensionFilters().add(filter);

        // Show open file dialog
        File file = fileChooser.showOpenDialog(Main.getContext().getPrimaryStage());

        if (file != null) {
            try {
                // Read the file content
                String content = Files.readString(file.toPath());
                ContentTxt = Files.readString(file.toPath());
                BookContent.setText(content);
            } catch (IOException e) {
                Alerts.showErrorDialog(e.getMessage());
            }
        }
    }

    //Method for generating summary of the book based on the title
    @FXML
    private void Generate() throws IOException {
        BookAbstract.setText("Example summary of the Book");
    }

    //Method for publishing the book
    @FXML
    private void PublishBook() throws IOException {
        if(BookTitle.getText().isEmpty() || ContentTxt.isEmpty() || BookAbstract.getText().isEmpty()) {
            Alerts.showErrorDialog("Missing information of the book.");
            return;
        }
        if(ContentTxt.isEmpty()){
            Alerts.showErrorDialog("Use text file for the content of the book.");
            return;
        }else if(!BookContent.getText().equals(ContentTxt.replaceAll("\n",""))){
            Alerts.showErrorDialog("Different content. Please upload the text file again.");
            return;
        }
        assert Main.getContext().getLoggedInUser() != null;
        var book = new Book(BookTitle.getText(), new Author.ByRef(Main.getContext().getLoggedInUser()._1()), false);
        Optional<Book.Data> opt = repository.bookOps.read(book);
        if(opt.isPresent()) {
            Alerts.showErrorDialog("Duplicated Book Title.");
        }else{
	        var data = new Book.Data(BookAbstract.getText(), ContentTxt, Book.ApprovalStatus.PENDING, null, null, 0);
            try {
                repository.bookOps.create(book, data);
                Alerts.showInfoDialog("Book is awaiting approval.");
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/author/AuthorMyBooks.fxml"));
                Parent root = fxmlLoader.load();
                AuthorMyBooksController controller = fxmlLoader.getController();
                controller.reload();
            } catch (TransactionException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
