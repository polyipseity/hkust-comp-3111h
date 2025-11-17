package library.controllers.author;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import library.Main;
import library.models.Author;
import library.models.Book;
import library.models.User;
import library.persistence.Repository;
import library.utils.Dates;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthorStatusController {
    private Repository repository = Main.getContext().getRepository();
    private final User user = Main.getContext().getLoggedInUser()._1();
    private Map<Book, Book.Data> authorBooks;

    private int pendingBooks = 0;
    private int approvedBooks = 0;

    @FXML private PieChart BooksStatusPieChart;
    @FXML private BarChart<String, Number> PopularBooksBarChart;

    @FXML
    public void initialize() {
        refreshStatus();
        loadPieChartData();
        loadBarChartData();
    }

    private void refreshStatus(){
        authorBooks = repository.bookOps.read(new Author.ByRef(user));
    }

    @FXML
    private void refresh(){
        initialize();
    }

    private void loadPieChartData() {
        // Get books by status for the current author
        approvedBooks = 0;
        pendingBooks = 0;

        for (final var bookEntry : authorBooks.entrySet()) {
            final var data = bookEntry.getValue();
            if(data.approvalStatus() == Book.ApprovalStatus.APPROVED){
                approvedBooks++;
            }else if(data.approvalStatus() == Book.ApprovalStatus.PENDING){
                pendingBooks++;
            }
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Pending", pendingBooks),
                new PieChart.Data("Approved", approvedBooks)
        );

        BooksStatusPieChart.setData(pieChartData);
    }
    private void loadBarChartData() {
        // Get all approved books for the current author
        Map<Book, Book.Data> allApprovedBook = authorBooks.entrySet().stream().filter(book ->
            book.getValue().approvalStatus() == Book.ApprovalStatus.APPROVED
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        //Sort the approved books by times borrowed
        List<Map.Entry<Book, Book.Data>> sortedByReaders = allApprovedBook.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> -entry.getValue().timesBorrowed())) // Negative for descending
                .limit(5) // Take top 5 only
                .toList();

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (Map.Entry<Book, Book.Data> entry : sortedByReaders) {
            Book book = entry.getKey();
            Book.Data data = entry.getValue();

            series.getData().add(new XYChart.Data<>(book.title(), data.timesBorrowed()));
        }

        // Clear existing data and add new series
        PopularBooksBarChart.getData().clear();
        PopularBooksBarChart.getData().add(series);
    }
}
