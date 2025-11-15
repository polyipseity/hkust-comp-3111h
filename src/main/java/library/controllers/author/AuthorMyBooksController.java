package library.controllers.author;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import library.Main;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthorMyBooksController {
    private final Repository repository = Main.getContext().getRepository();
    List<Book> authorBooks;

    public class BookRecord{
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
    private TableView<BookRecord> BooksTable;

    @FXML
    private TableColumn<BookRecord, String> Title, Status, Date, Abstract;

    @FXML
    private TableColumn<BookRecord, Long> Readers;

    @FXML
    private void initialize(){
        //Binding Table Column
        Title.setCellValueFactory(new PropertyValueFactory<>("title"));
        Status.setCellValueFactory(new PropertyValueFactory<>("status"));
        Date.setCellValueFactory(new PropertyValueFactory<>("date"));
        Readers.setCellValueFactory(new PropertyValueFactory<>("readers"));
        Abstract.setCellValueFactory(new PropertyValueFactory<>("summary"));

        //LoadTable
        reload();
    }

    @FXML
    private void refresh(){
        reload();
    }

    public void reload(){
        //Get book authorized by the current author
        authorBooks = repository.bookOps.getBooksWithAuthor(new Author.ByRef(Main.getContext().getLoggedInUser()._1()));

        ObservableList<BookRecord> tableData = FXCollections.observableArrayList();

        //For each book in the db, write it in the tableView
        for(Book book : authorBooks){
            var data = repository.bookOps.read(book).get();
            var date = switch (data.dates()){
                case null -> "";
                case "" -> data.approvalStatus().toString();
                default -> data.dates();
            };
            var record = new BookRecord(book, book.title(),data.approvalStatus().toString(), date, data.timesBorrowed(),data.summary());
            tableData.add(record);
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
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/author/AuthorViewWindow.fxml"));
            Parent root = fxmlLoader.load();

            //Passing content to new window
            AuthorViewWindowController controller = fxmlLoader.getController();
            controller.setContent(repository.bookOps.read(selected.book).get().content());

            // Create a new Stage (window)
            Stage newStage = new Stage();
            newStage.setTitle("Reading: " + selected.title + ".txt");
            newStage.setScene(new Scene(root));

            // Show the new window
            newStage.show();

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
            // Load the FXML file for the new window's content
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/author/AuthorModifyWindow.fxml"));
            Parent root = fxmlLoader.load();

            //Passing content to new window
            AuthorModifyWindowController controller = fxmlLoader.getController();
            Book.Data data = repository.bookOps.read(selected.book).get();
            controller.setData(selected.book.title(),data.summary());

            // Create a new Stage (window)
            Stage newStage = new Stage();
            newStage.setResizable(false);
            newStage.setTitle("Modify Book");
            newStage.setScene(new Scene(root));

            // Show the new window
            newStage.show();

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
        List<Book> selectedBook = authorBooks.stream()
                .filter(book -> selected.title.equals(book.title()))
                .toList();

        if(selectedBook.isEmpty()){
            Alerts.showErrorDialog("Selected book not found");
            return;
        }

        //Drop selected book in db
        for (Book book : selectedBook) {
            repository.bookOps.delete(book);
        }

        //Reload table after deleting a book
        reload();
    }
}
