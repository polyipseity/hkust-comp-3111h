package library.controllers.author;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.ManageBooksControl;
import library.models.Book;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

public final class ModifyWindowController implements RequiresLoggedIn {
	public Runnable modifyCallback = () -> {
	};
	@UnknownNullability
	public Button saveButton;
	@UnknownNullability
	public TextArea SummaryArea;
	@UnknownNullability
	public TextField TitleField;
	@Nullable
	private Tuple2<Book, Book.Data> bookEntry;

	public Tuple2<Book, Book.Data> getBookEntry() {
		return Objects.requireNonNull(bookEntry);
	}

	public void setBookEntry(Tuple2<Book, Book.Data> bookEntry) {
		final var title = bookEntry._1().title();
		final var summary = bookEntry._2().summary();
		TitleField.textProperty().setValue(title);
		SummaryArea.textProperty().setValue(summary);
		saveButton.disableProperty().bind(
				SimpleBooleanProperty.booleanExpression(TitleField.textProperty().map(title::equals))
						.and(SimpleBooleanProperty.booleanExpression(SummaryArea.textProperty().map(summary::equals)))
		);
		this.bookEntry = bookEntry;
	}

    @FXML
    private void saveModification() {
	    final var bookEntry = getBookEntry();
	    final var title = TitleField.getText();
	    final var summary = SummaryArea.getText();
	    try {
		    switch (Main.getContext().getManageBooksControl()
				    .modifyBook(bookEntry._1(), title, summary)) {
			    case ManageBooksControl.ModifyResult.Success success -> {
				    Alerts.showInfoDialog(success.getMessage());
				    // Close the current window
				    ((Stage) saveButton.getScene().getWindow()).close();
				    modifyCallback.run();
			    }
			    case HasMessage message -> Alerts.showErrorDialog(message.getMessage());
		    }
	    } catch (TransactionException e) {
		    Alerts.showErrorDialog(e.getLocalizedMessage());
	    }
    }

    @FXML
    private void cancelModification(){
	    ((Stage) saveButton.getScene().getWindow()).close();
    }
}
