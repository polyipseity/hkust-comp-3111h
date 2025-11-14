package library.controllers.author;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import library.Main;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorMyBooksController {
    private final Repository repository = Main.getContext().repository;
    List<Book> authorBooks;

    public class BookRecord{
        private final String title;
        private final String status;
        private final String date;
        private final long readers;
        private final String summary;

        public BookRecord(String Title, String Status, String Date, long Readers, String Abstract) {
            this.title = Title;
            this.status = Status;
            this.date = Date;
            this.readers = Readers;
            this.summary = Abstract;
        }

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

    public void reload(){
        //Get book authorized by the current author
        authorBooks = repository.bookOps.getBooksWithAuthor(new Author.ByRef(Main.getContext().getLoggedInUser()._1()));

        ObservableList<BookRecord> tableData = FXCollections.observableArrayList();

        //For each book in the db, write it in the tableView
        for(Book book : authorBooks){
            var data = repository.bookOps.read(book).get();
            var record = new BookRecord(book.title(),data.approvalStatus().toString(),"",data.timesBorrowed(),data.summary());
            tableData.add(record);
        }
        BooksTable.setItems(tableData);
    }

    @FXML
    private void AuthorViewBook() {
        reload();
    }

    @FXML
    private void AuthorDeleteBook() throws TransactionException {
        BookRecord selected = BooksTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alerts.showErrorDialog("Please select a book to delete");
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
