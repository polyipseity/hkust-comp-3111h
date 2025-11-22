package library.controllers.author;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.ManageBooksControl;
import library.models.Author;
import library.models.Book;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import library.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;

@RequiredArgsConstructor
public final class ModifyWindowController implements RequiresLoggedIn, Initializable {
	public final Stage stage;
	public final Book book;
	public final Book.Data bookData;
	public final Runnable confirmCallback;

	@UnknownNullability
	@SuppressWarnings("unused")
	public TextField titleField;
	@UnknownNullability
	@SuppressWarnings("unused")
	public TextArea summaryArea;
	@UnknownNullability
	@SuppressWarnings("unused")
	public Button confirmButton;

	@SuppressWarnings({"DataFlowIssue", "unused"}) // for testing
	private ModifyWindowController() {
		this.stage = null;
		this.book = new Book("", new Author.ByName(""));
		this.bookData = new Book.Data("", "", Book.ApprovalStatus.values()[0], TimeUtil.nowZoned(), null, 0);
		this.confirmCallback = () -> {
		};
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		titleField.setText(book.title());
		summaryArea.setText(bookData.summary());
		confirmButton.disableProperty().bind(
				SimpleBooleanProperty.booleanExpression(titleField.textProperty().map(book.title()::equals))
						.and(SimpleBooleanProperty.booleanExpression(summaryArea.textProperty().map(bookData.summary()::equals)))
		);
	}

	public void confirm() {
		final var title = titleField.getText();
		final var summary = summaryArea.getText();
		try {
			switch (Main.getContext().getManageBooksControl()
					.modifyBook(book, title, summary)) {
				case ManageBooksControl.ModifyResult.Success success -> {
					// Show success response to author
					Alerts.showInfoDialog(success.getLocalizedMessage());
					// Close the current window
					stage.close();
					confirmCallback.run();
				}
				case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
			}
		} catch (TransactionException e) {
			Alerts.showErrorDialog(e.getLocalizedMessage());
		}
	}

	public void cancel() {
		stage.close();
	}
}
