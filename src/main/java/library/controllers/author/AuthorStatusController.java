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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthorStatusController {
    private Repository repository = Main.getContext().getRepository();
    private final User user = Main.getContext().getLoggedInUser()._1();
    private Map<Book, Book.Data> authorBooks = repository.bookOps.read(new Author.ByRef(user));

    private int pendingBooks = 0;
    private int approvedBooks = 0;

    @FXML private PieChart BooksStatusPieChart;
    @FXML private BarChart<String, Number> PopularBooksBarChart;

    @FXML
    public void initialize() {
        loadPieChartData();
        loadBarChartData();
    }

    @FXML
    private void refresh(){
        initialize();
    }

    private void loadPieChartData() {
        // Get books by status for the current author

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
        // Get books by status for the current author
        List<Book> allApprovedBooks = new ArrayList<>(authorBooks.keySet());

        List<Book> topBooks = allApprovedBooks.stream()
                .sorted((b1, b2) -> {
                    long count1 = repository.bookOps.read(b1).map(Book.Data::timesBorrowed).orElse(0L);
                    long count2 = repository.bookOps.read(b2).map(Book.Data::timesBorrowed).orElse(0L);
                    return Long.compare(count2, count1); // Descending order
                })
                .limit(5)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Times Borrowed");

        for (Book book : topBooks) {
            Book.Data data = repository.bookOps.read(book).orElse(null);
            if (data != null) {
                String bookTitle = book.title().length() > 20 ?
                        book.title().substring(0, 17) + "..." : book.title();

                series.getData().add(new XYChart.Data<>(
                        bookTitle,
                        data.timesBorrowed()
                ));
            }
        }
    }
}
