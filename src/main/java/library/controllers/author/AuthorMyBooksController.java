package library.controllers.author;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import library.FXMLs;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.controllers.common.TextViewController;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import library.utils.TimeUtil;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public final class AuthorMyBooksController implements RequiresLoggedIn {
    private final Repository repository = Main.getContext().getRepository();
	Map<Book, Book.Data> authorBooks;

    @Setter
    private DashboardController parentController;

    public static final class BookRecord {
        private final Book book;
        private final String title;
        private final String status;
        private final String date;
        private final long readers;
        private final String summary;

        public BookRecord(Book book, String Title, String Status, String Date, long Readers, String Abstract) {
            this.book = book;
            this.title = Title;
            this.status = Status;
            this.date = Date;
            this.readers = Readers;
            this.summary = Abstract;
        }

        public Book getBook() { return book; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public String getDate() { return date; }
        public long getReaders() { return readers; }
        public String getSummary() { return summary; }
    }

    @FXML
    private TableView<@Nullable BookRecord> BooksTable;

    @FXML
    private TableColumn<BookRecord, @Nullable String> Title, Status, Date, Abstract;

    @FXML
    private TableColumn<BookRecord, @Nullable Long> Readers;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		//Binding Table Column
		Title.setCellValueFactory(new PropertyValueFactory<>("title"));
		Status.setCellValueFactory(new PropertyValueFactory<>("status"));
		Date.setCellValueFactory(new PropertyValueFactory<>("date"));
		Readers.setCellValueFactory(new PropertyValueFactory<>("readers"));
		Abstract.setCellValueFactory(new PropertyValueFactory<>("summary"));

		//LoadTable
		reload();
	}

    public void reload(){
        //Get book authorized by the current author
	    final var author = new Author.ByRef(getLoggedInUser()._1());
	    authorBooks = repository.bookOps.read(entry -> author.equals(entry.getKey().author()));

        ObservableList<BookRecord> tableData = FXCollections.observableArrayList();

        //For each book in the db, write it in the tableView
	    for (final var bookEntry : authorBooks.entrySet()) {
		    final var book = bookEntry.getKey();
		    final var data = bookEntry.getValue();
	        var date = switch (data.publishDate()) {
		        case ZonedDateTime val -> TimeUtil.toStringZonedLocal(val);
		        case null -> data.approvalStatus().toString();
            };
            var record = new BookRecord(book, book.title(), book.temporary() ? "MODIFIED" : data.approvalStatus().toString(), book.temporary() ? "MODIFIED" : date, data.timesBorrowed(), data.summary());
            if(data.approvalStatus()!= Book.ApprovalStatus.REJECTED) {
                tableData.add(record);
            }
        }
        BooksTable.setItems(tableData);
    }

    @FXML
    private void AuthorViewBook() {
        BookRecord selected = BooksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
        try {
            // Load the FXML file for the new window's content
	        final var content = repository.bookOps.readOrThrow(selected.book).content();
	        final var root = FXMLs.COMMON_TEXT_VIEW.<Parent>load(loader -> {
		        final var controller = loader.<TextViewController>getController();
		        //Passing content to new window
		        controller.setContent(content);
	        });
	        // Create a new Stage (window) and show it
	        Main.getContext().newWindow(
			        TextViewController.WINDOW_TITLE.formatted(selected.title),
			        root,
			        null
	        ).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void AuthorModifyBook(){
        BookRecord selected = BooksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
        try {
            if(selected.book.temporary()){
                Alerts.showErrorDialog("Selected book is temporary. Please select the original book.");
                return;
            }
            if(repository.bookOps.read(selected.book).get().originalOrModified() != null){
                Alerts.showErrorDialog("Already modified this book, delete the modified version to make new modifications.");
                return;
            }
            // Load the FXML file for the new window's content
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/author/AuthorModifyWindow.fxml"));
            Parent root = fxmlLoader.load();

            //Passing content to new window
            AuthorModifyWindowController controller = fxmlLoader.getController();
            Book.Data data = repository.bookOps.read(selected.book).get();
            controller.setData(selected.book.title(),data.summary(),selected.book);

            // Create a new Stage (window)
            Stage newStage = new Stage();
            newStage.setResizable(false);
            newStage.setTitle("Modify Book");
            newStage.setScene(new Scene(root));

            // Show the new window
            newStage.show();
            newStage.setOnHidden(event -> {
                reload();});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void AuthorDeleteBook() throws TransactionException {
        BookRecord selected = BooksTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alerts.showErrorDialog("Please select a book first.");
            return;
        }
        //Get selected book from db
	    List<Book> selectedBook = authorBooks.keySet().stream()
                .filter(book -> selected.title.equals(book.title()))
                .toList();

        if(selectedBook.isEmpty()){
            Alerts.showErrorDialog("Selected book not found");
            return;
        }

        //Drop selected book in db
        for (Book book : selectedBook) {
            var data = repository.bookOps.read(book).get();
            if(data.approvalStatus().equals(Book.ApprovalStatus.REJECTED)){
                //Handle deleting rejected book
                Alerts.showErrorDialog("Cannot delete the book that is rejected");
            }else if(data.approvalStatus().equals(Book.ApprovalStatus.PENDING)){
                //Handle deleting pending book
                deleteBookCondition(book, data, selected);
            }else{
                //If the book is approved
                if(repository.borrowOps.read(book).isEmpty()){
                    deleteBookCondition(book, data, selected);
                }else{
                    Alerts.showErrorDialog("Cannot delete the book that is already borrowed");
                }
            }
        }
        //Reload table after deleting a book
        reload();
    }


    private void deleteBookCondition(Book book, Book.Data data, BookRecord record) throws TransactionException {
        if(data.summary().equals(record.getSummary())){
            if(book.temporary()){
                //Delete the modified version
                if(deleteConfirmation(record.title)) {
                    repository.bookOps.delete(book, data);
                }
            }else{
                //Can't delete originalBook
                if(data.originalOrModified() == null){
                    if(deleteConfirmation(record.title)) {
                        repository.bookOps.delete(book, data);
                    }
                }else{
                    Alerts.showErrorDialog("Delete the modified version to delete the original book");
                }
            }
        }
    }

    private Boolean deleteConfirmation(String message) {
        Optional<ButtonType> result = Alerts.showConfirmDialog("Delete \"" + message + "\"?");
        // Return true if confirmed
        return !(result.isPresent() && result.get() == ButtonType.CANCEL);
    }
}
