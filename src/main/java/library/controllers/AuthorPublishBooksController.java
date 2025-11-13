package library.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import library.FXMLs;
import library.Main;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AuthorPublishBooksController {
    private TextField BookTitle, BookContent, BookAbstract;

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

                // Process the file content (your logic here)
                processUploadedFile(content, file.getName());

                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "File uploaded successfully: " + file.getName());

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Could not read file: " + e.getMessage());
            }
        }
    }

    private void processUploadedFile(String content, String fileName) {
        // Your business logic here
        System.out.println("Processing file: " + fileName);
        System.out.println("Content: " + content);

        // Example: Update UI, save to database, etc.
        // expensesTableView.getItems().add(...);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Method for generating summary of the book based on the title
    @FXML
    private void Generate() throws IOException {

    }

    //Method for publishing the book
    @FXML
    private void PublishBook() throws IOException {

    }
}
