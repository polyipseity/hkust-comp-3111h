package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import library.Main;
import library.SpringApplicationPackage.ChatService;
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

    private final ChatService chatService = new ChatService();

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
        if(ContentTxt == null || BookTitle.getText() == null) {
            Alerts.showErrorDialog("You must enter the book title and upload the book content first!");
        }else{
            var input = "Base on the given title and content, generate a abstract for the book. Note you must return the abstract paragraph only and focusing on the content and title but not the author. Also, you must keep your response short that is under 30 words. title:" + BookTitle.getText() + " and content:" + ContentTxt;
            var response = chatService.getResponse(input);
            BookAbstract.setText(response);
        }
    }

    //Method for publishing the book
    @FXML
    private void PublishBook() throws IOException {
        if(BookTitle.getText().isEmpty() || ContentTxt.isEmpty() || BookAbstract.getText().isEmpty()) {
            Alerts.showErrorDialog("Missing information of the book.");
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
            } catch (TransactionException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
