package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import library.Main;
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

public class AuthorViewWindowController {
    @FXML
    private TextArea ContentArea;

    @FXML
    private Slider ZoomSlider;

    double prefSize = 20;

    @FXML
    private void initialize() {
        ZoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ContentArea.setFont(Font.font(ContentArea.getFont().getFamily(), newValue.doubleValue()));
            ScrollBar vertScrollBar = (ScrollBar) ContentArea.lookup(".scroll-bar:vertical");
            vertScrollBar.setPrefWidth(prefSize);
        });
    }

    public void setContent(String content) {
        ContentArea.setText(content);
    }
}
