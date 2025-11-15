package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AuthorModifyWindowController {
    private String initialTitle, initalSummary, currentTitle, currentSummary;

    @FXML
    private Button saveButton;

    @FXML
    private TextArea SummaryArea;

    @FXML
    private TextField TitleField;

    public void setData(String title, String summary) {
        initialTitle = title;
        initalSummary = summary;
        TitleField.setText(title);
        SummaryArea.setText(summary);
    }

    @FXML
    private void initialize() {
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
        if (initialTitle.equals(title) && initalSummary.equals(summary)) {
            saveButton.setDisable(true);
        }else{
            saveButton.setDisable(false);
        }
    }

    @FXML
    private void cancelModification(){
        Stage currentStage = (Stage) TitleField.getScene().getWindow();
        currentStage.close();
    }
}
