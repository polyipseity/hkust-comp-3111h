package library.controllers.author;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import library.Main;
import library.SpringApplicationPackage.ChatService;
import library.controllers.common.RequiresLoggedIn;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public final class AuthorPublishBooksController implements RequiresLoggedIn {
    private final Repository repository = Main.getContext().getRepository();

    private final ChatService chatService = new ChatService();

    @Setter
    private DashboardController parentController;

    @FXML
    private TextField BookTitle, BookContent, BookAbstract;
    private String ContentTxt;

    //Method for choosing text file
    @FXML
    private void ChooseTextFile(){
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
                if(BookTitle.getText().isEmpty()){
                    BookTitle.setText(file.getName().substring(0, file.getName().lastIndexOf('.')));
                }
                String content = Files.readString(file.toPath());
                ContentTxt = Files.readString(file.toPath());
                BookContent.setText(file.getName());
            } catch (IOException e) {
                Alerts.showErrorDialog(e.getMessage());
            }
        }
    }

    //Method for generating summary of the book based on the title
    @FXML
    private void Generate() {
        if(ContentTxt == null || BookTitle.getText() == null) {
            Alerts.showErrorDialog("You must enter the book title and upload the book content first!");
        }else{
            var input = "Create a professional book abstract under 30 words for \"$title\" that summarizes the main themes and content. You should avoid \"In this book... (redundant), In the novel... (obvious), This story is about... (weak opening), In [Title]... (formulaic)\". Title:" + BookTitle.getText() + " and content:" + ContentTxt;
            var response = chatService.getResponse(input);
            BookAbstract.setText(response);
        }
    }

    public boolean isValidBookTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        // Allow: letters, numbers, spaces, basic punctuation
        // Disallow: only spaces, only symbols, prohibited characters
//        String validTitlePattern = "^(?!\\s*$)" +                   // Not only whitespace
//                "(?!.*[$%^&*<>{}|\\\\~`])" +      // No prohibited chars
//                "(?=.*[a-zA-Z0-9])" +             // At least one letter/number
//                "[\\p{L}\\p{N}\\p{Zs}\\p{P}&&[^$%^&*<>{}|\\\\~`]]{1,255}$";
//
//        return title.trim().matches(validTitlePattern);

        // Optional: Check for minimum meaningful length
        return title.trim().length() >= 2;
    }

    //Method for publishing the book
    @FXML
    private void PublishBook() {
        if(BookTitle.getText().isEmpty() || ContentTxt.isEmpty() || BookAbstract.getText().isEmpty()) {
            Alerts.showErrorDialog("Missing information of the book.");
            return;
        }
        if(!isValidBookTitle(BookTitle.getText())) {
            Alerts.showErrorDialog("The book title is invalid or too short.");
            return;
        }
	    var book = new Book(BookTitle.getText(), new Author.ByRef(getLoggedInUser()._1()), false);
        Optional<Book.Data> opt = repository.bookOps.read(book);
        if(opt.isPresent()) {
            Alerts.showErrorDialog("Duplicated Book Title.");
        }else{
	        var data = new Book.Data(BookAbstract.getText(), ContentTxt, Book.ApprovalStatus.PENDING, null, null, 0);
            try {
                repository.bookOps.create(book, data);
                Alerts.showInfoDialog("Published and awaiting approval.");
                parentController.authorMyBooksController.reload();
            } catch (TransactionException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
