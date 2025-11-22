package library.controllers.author;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.models.Author;
import library.models.Book;
import library.models.json.OpenAIChatCompletionBody;
import library.models.json.OpenAIChatCompletionResponse;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public final class PublishBooksController implements RequiresLoggedIn {
	@UnknownNullability
	@SuppressWarnings("unused")
	public TextField titleField, contentField, summaryField;
	@Nullable
	private String ContentTxt;

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

		// Optional: Check for minimum meaningful length
		return title.trim().length() >= 2;
	}

	//Method for publishing the book
	public void publishBook() {
		if (titleField.getText().isEmpty() || ContentTxt == null || ContentTxt.isEmpty() || summaryField.getText().isEmpty()) {
			Alerts.showErrorDialog("Missing information of the book.");
			return;
		}
		if (!isValidBookTitle(titleField.getText())) {
			Alerts.showErrorDialog("The book title is invalid or too short.");
			return;
		}
		var book = new Book(titleField.getText(), new Author.ByRef(getLoggedInUser()._1()), false);
		final var repository = Main.getContext().getRepository();
		Optional<Book.Data> opt = repository.bookOps.read(book);
		if (opt.isPresent()) {
			if (opt.get().approvalStatus() == Book.ApprovalStatus.REJECTED) {
				Alerts.showErrorDialog("Rejected Book of the same title and author already exists");
			} else {
				Alerts.showErrorDialog("Book of the same title and author already exists");
			}
		} else {
			var data = new Book.Data(summaryField.getText(), ContentTxt, Book.ApprovalStatus.PENDING, null, null, 0);
			try {
				repository.bookOps.create(book, data);
			} catch (TransactionException e) {
				throw new RuntimeException(e);
			}
			Alerts.showInfoDialog("Published and awaiting approval.");
		}
	}
}
