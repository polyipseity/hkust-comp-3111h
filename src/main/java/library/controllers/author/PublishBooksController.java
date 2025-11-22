package library.controllers.author;

import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import library.Main;
import library.SpringApplicationPackage.ChatService;
import library.controllers.common.RequiresLoggedIn;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public final class PublishBooksController implements RequiresLoggedIn {
	private final Repository repository = Main.getContext().getRepository();

	private final ChatService chatService = new ChatService();
	@UnknownNullability
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
				Alerts.showErrorDialog(e.getMessage());
			}
		}
	}

	//Method for generating summary of the book based on the title
	public void generateSummary() {
		if (ContentTxt == null || titleField.getText() == null) {
			Alerts.showErrorDialog("You must enter the book title and upload the book content first!");
		} else {
			var input = "Create a professional book abstract under 30 words for \"$title\" that summarizes the main themes and content. You should avoid \"In this book... (redundant), In the novel... (obvious), This story is about... (weak opening), In [Title]... (formulaic)\". Title:" + titleField.getText() + " and content:" + ContentTxt;
			var response = chatService.getResponse(input);
			summaryField.setText(response);
		}
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
				Alerts.showInfoDialog("Published and awaiting approval.");
			} catch (TransactionException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
