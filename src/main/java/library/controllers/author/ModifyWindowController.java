package library.controllers.author;

import javafx.beans.property.SimpleBooleanProperty;
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
	public Runnable confirmCallback = () -> {
	};

	@UnknownNullability
	public TextField titleField;
	@UnknownNullability
	public TextArea summaryArea;
	@UnknownNullability
	public Button saveButton;

	@Nullable
	private Tuple2<Book, Book.Data> bookEntry;

	public Tuple2<Book, Book.Data> getBookEntry() {
		return Objects.requireNonNull(bookEntry);
	}

	public void setBookEntry(Tuple2<Book, Book.Data> bookEntry) {
		final var title = bookEntry._1().title();
		final var summary = bookEntry._2().summary();
		titleField.textProperty().setValue(title);
		summaryArea.textProperty().setValue(summary);
		saveButton.disableProperty().bind(
				SimpleBooleanProperty.booleanExpression(titleField.textProperty().map(title::equals))
						.and(SimpleBooleanProperty.booleanExpression(summaryArea.textProperty().map(summary::equals)))
		);
		this.bookEntry = bookEntry;
	}

	public void confirm() {
		final var bookEntry = getBookEntry();
		final var title = titleField.getText();
		final var summary = summaryArea.getText();
		try {
			switch (Main.getContext().getManageBooksControl()
					.modifyBook(bookEntry._1(), title, summary)) {
				case ManageBooksControl.ModifyResult.Success success -> {
					Alerts.showInfoDialog(success.getMessage());
					// Close the current window
					((Stage) saveButton.getScene().getWindow()).close();
					confirmCallback.run();
				}
				case HasMessage message -> Alerts.showErrorDialog(message.getMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}

	public void cancel() {
		((Stage) saveButton.getScene().getWindow()).close();
	}
}
