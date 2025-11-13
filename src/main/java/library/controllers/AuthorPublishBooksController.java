package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import library.FXMLs;
import library.Main;
import library.controls.UserValidator;
import library.models.Author;
import library.models.Book;
import library.models.User;
import library.persistence.Repository;
import library.utils.Alerts;

import javafx.scene.control.TextField;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

public class AuthorPublishBooksController {

    private final Repository repository = Main.getContext().repository;

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
        File file = fileChooser.showOpenDialog(Main.getContext().primaryStage);

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
            Alerts.showErrorDialog("Missing information of the book");
        }
        assert Main.getContext().getLoggedInUser() != null;
        var book = new Book(BookTitle.getText(), new Author.ByRef(Main.getContext().getLoggedInUser()._1()), false);
        Optional<Book.Data> opt = repository.readBook(book);
        if(opt.isPresent()) {
            Alerts.showErrorDialog("Duplicated Book Title");
        }else{
            var data = new Book.Data(BookAbstract.getText(), ContentTxt, Book.ApprovalStatus.PENDING, null, Collections.emptyMap(), 0);
            try {
                repository.createBook(book, data);
            } catch (Repository.TransactionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
