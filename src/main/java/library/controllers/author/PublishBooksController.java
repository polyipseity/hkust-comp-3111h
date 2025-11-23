package library.controllers.author;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controls.ManageBooksControl;
import library.controls.PublishBooksControl;
import library.models.Author;
import library.models.Book;
import library.models.json.OpenAIChatCompletionBody;
import library.models.json.OpenAIChatCompletionResponse;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.HasMessage;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * The PublishBooksController class provides functionality for managing the operations
 * related to book publishing. It ensures that only a logged-in user can interact with the controller
 * features for uploading book content, generating summaries, validating book titles, and publishing books.
 * <p>
 * This class is responsible for:
 * - Allowing users to upload text files containing book content.
 * - Automatically generating a summary based on the uploaded book content and title.
 * - Validating the book title for correct formatting and meaningfulness.
 * - Publishing books with the required information and status.
 * <p>
 * Implements the {@link RequiresLoggedIn} interface to enforce that the user must be logged in
 * to access the features provided by this controller.
 */
public final class PublishBooksController implements RequiresLoggedIn {
    /**
     * The Title field.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextField titleField, /**
     * The Content field.
     */
    contentField, /**
     * The Summary field.
     */
    summaryField;
	@Nullable
	private String ContentTxt;

    /**
     * Opens a file chooser dialog to allow the user to select a text file.
     * The method filters the selectable files to only show `.txt` files. If the user selects
     * a valid file, the content of the file is read and stored for further use.
     * Additionally, if the title field is empty, the file name (excluding the file extension)
     * is set as the title.
     * <p>
     * If an I/O exception occurs while reading the file, an error dialog is displayed.
     */
//Method for choosing text file
	public void chooseTextFile() {
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
				if (titleField.getText().isEmpty()) {
					titleField.setText(file.getName().substring(0, file.getName().lastIndexOf('.')));
				}
				ContentTxt = Files.readString(file.toPath());
				contentField.setText(file.getName());
			} catch (IOException e) {
				Alerts.showErrorDialog(e.getLocalizedMessage());
			}
		}
	}

    /**
     * Generates a summary of the book based on its title and content.
     * <p>
     * This method verifies if both the book title and content are provided.
     * If either is missing, an error dialog is displayed, and the process is terminated.
     * <p>
     * The method interacts with the AI service to generate a summary based on
     * the book's title and an excerpt of its content. It uses a structured request
     * to implement the summarization, which includes major topics and their details
     * within a concise word limit.
     * <p>
     * During the request execution, a loading dialog is displayed to ensure a better
     * user experience. The summary generation process also logs the time taken for
     * completion.
     * <p>
     * Any errors or failures in the process, such as service errors, cancellation,
     * or invalid state, are captured and displayed to the user through an appropriate
     * error dialog.
     * <p>
     * Upon successful completion, the generated summary is displayed within the
     * summary field, and the user is informed of the completion through an
     * informational dialog.
     */
//Method for generating summary of the book based on the title
	public void generateSummary() {
		if (ContentTxt == null || titleField.getText() == null) {
			Alerts.showErrorDialog("You must enter the book title and upload the book content first!");
			return;
		}

		final var startTime = System.currentTimeMillis();
		final var responseFuture = Main.getContext().getAIServiceControl()
				.chatComplete(new OpenAIChatCompletionBody(
						"gpt-4o-mini",
						List.of(new OpenAIChatCompletionBody.Message(
										"system",
										"""        
												I want you to act as a book summarizer. Provide a detailed summary of %s. Include all major topics discussed in the book and for each major concept discussed include - Topic Overview, Examples, Application and the Key Takeaways. Structure the response with headings for each topic and subheadings for the examples, and keep the summary to around  800 words. An excerpt of the book is provided below.
												""".formatted(titleField.getText())
								),
								new OpenAIChatCompletionBody.Message(
										"user",
										ContentTxt.length() > 2048 ? "%s\n\n%s".formatted(ContentTxt.substring(0, 1024), ContentTxt.substring(ContentTxt.length() - 1024)) : ContentTxt
								)
						),
						null,
						null,
						null
				))
				.thenApply(response -> response.choices().stream().map(OpenAIChatCompletionResponse.Choice::message).map(OpenAIChatCompletionResponse.Message::content).findFirst().orElseThrow());

// Show loading dialog during request
		final var running = new SimpleBooleanProperty(true);
		responseFuture.whenComplete((_, _) -> Platform.runLater(() -> running.set(false)));
		if (!Alerts.showLoadingDialog("Generating book summary...", running)) {
			responseFuture.cancel(true);
			return;
		}

		responseFuture.<Runnable>thenApply(response -> {
			final var endTime = System.currentTimeMillis();
			return () -> {
				summaryField.setText(response);
				Alerts.showInfoDialog("Summary generated in %d ms".formatted(endTime - startTime));
			};
		}).exceptionally(throwable -> {
			final var endTime = System.currentTimeMillis();
			return () -> Alerts.showErrorDialog("Failed to generate summary in %d ms: %s".formatted(endTime - startTime, throwable.getLocalizedMessage()));
		}).thenAccept(Platform::runLater);
	}

    /**
     * Validates if a given book title is valid based on predefined criteria.
     * <p>
     * This method checks if the provided title is not null or empty after trimming.
     * Additional validation rules can be implemented to enforce constraints such as
     * disallowing certain prohibited characters, ensuring a minimum meaningful length,
     * or limiting the maximum character count.
     *
     * @param title the book title to validate; may be null.
     * @return true if the given title meets the validation criteria; false otherwise.
     */
    public boolean isValidBookTitle(@Nullable String title) {
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

//		// Optional: Check for minimum meaningful length
//		return title.trim().length() >= 2;
        return true;
	}

    /**
     * Publishes a book by validating the provided information and interacting with the publishing control.
     * <p>
     * The method ensures that all required fields for the book, including title, content, and summary,
     * are provided. If any information is missing or invalid, an error dialog is displayed to the user.
     * The book title is validated to meet predefined criteria.
     * <p>
     * Once validation passes, a new book object and its associated data are created. The method then
     * delegates the book publication process to the control mechanism. The result of this operation
     * determines the response shown to the user:
     * <p>
     * - If successful, an informational dialog is displayed indicating the book has been published and
     * is awaiting approval.
     * - If the operation fails, an error message is displayed based on the failure details.
     * <p>
     * If there is a transactional failure during the publishing process, a {@link TransactionException} is thrown.
     *
     * @throws TransactionException if there is a database transaction-related issue during book publishing.
     */
//Method for publishing the book
	public void publishBook() throws TransactionException {
		if (titleField.getText().isEmpty() || ContentTxt == null || ContentTxt.isEmpty() || summaryField.getText().isEmpty()) {
			Alerts.showErrorDialog("Missing information of the book.");
			return;
		}
		if (!isValidBookTitle(titleField.getText())) {
			Alerts.showErrorDialog("The book title is invalid or too short.");
			return;
		}
		var book = new Book(titleField.getText(), new Author.ByRef(getLoggedInUser()._1()), false);
        var data = new Book.Data(summaryField.getText(), ContentTxt, Book.ApprovalStatus.PENDING, null, null, 0);
		final var publishBooksControl = Main.getContext().getPublishBooksControl();
        switch(publishBooksControl.addBook(book,data)) {
            case PublishBooksControl.AddBookResult.Success success -> {
                // Show success response to author
                Alerts.showInfoDialog("Published and awaiting approval.");
            }
            case HasMessage message -> Alerts.showErrorDialog(message.getLocalizedMessage());
		}
	}
}
