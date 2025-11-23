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

/**
 * Controller for the Modify Window in the user interface, responsible for managing the interactions
 * and logic to modify the details of a book.
 * Implements {@code RequiresLoggedIn} and {@code Initializable}.
 */
@RequiredArgsConstructor
public final class ModifyWindowController implements RequiresLoggedIn, Initializable {
    /**
     * The Stage.
     */
    public final Stage stage;
    /**
     * The Book.
     */
    public final Book book;
    /**
     * The Book data.
     */
    public final Book.Data bookData;
    /**
     * The Confirm callback.
     */
    public final Runnable confirmCallback;

    /**
     * The Title field.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextField titleField;
    /**
     * The Summary area.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextArea summaryArea;
    /**
     * The Confirm button.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Button confirmButton;

	/**
     * Private default constructor for the {@code ModifyWindowController} class.
     * This constructor is used for testing purposes only and initializes the controller
     * with default values for its fields.
     *
     * The following are initialized to default values:
     * - {@code stage} is set to {@code null}.
     * - {@code book} is initialized with an empty title and author name.
     * - {@code bookData} is initialized with default values, including an empty title,
     *   summary, the first approval status, the current time, and default numeric values.
     * - {@code confirmCallback} is assigned an empty no-operation lambda.
     *
     * Suppresses warnings related to data flow issues and unused code.
     */
    @SuppressWarnings({"DataFlowIssue", "unused"}) // for testing
	private ModifyWindowController() {
		this.stage = null;
		this.book = new Book("", new Author.ByName(""));
		this.bookData = new Book.Data("", "", Book.ApprovalStatus.values()[0], TimeUtil.nowZoned(), null, 0);
		this.confirmCallback = () -> {
		};
	}

	/**
     * Initializes the controller for the Modify Window. Sets up the text fields and bindings for
     * updating the book details while ensuring user authentication.
     *
     * @param location the location used to resolve relative paths for the root object, or null if the
     *                 location is not known.
     * @param resources the resources used to localize the root object, or null if no resources are defined.
     */
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

    /**
     * Handles the confirm action initiated by the user to modify a book's details.
     * <p>
     * This method retrieves the title and summary entered by the user, and attempts to
     * modify the associated book data through the system's control logic. Actions taken
     * depend on the result of the modification attempt:
     * <p>
     * - If the modification is successful, displays an
     */
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

    /**
     * Closes the associated stage or window of the Modify Window controller.
     * <p>
     * This action is typically invoked to terminate or cancel the current operation
     * and dismiss the modify window interface without applying any changes.
     */
    public void cancel() {
		stage.close();
	}
}
