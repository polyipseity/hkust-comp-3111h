package library.controllers.author;

import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import library.Main;
import library.controllers.common.LoadsData;
import library.controllers.common.RequiresLoggedIn;
import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class StatusViewController implements RequiresLoggedIn, Initializable, LoadsData {
	private final Repository repository = Main.getContext().getRepository();
	@UnknownNullability
	@SuppressWarnings("unused")
	public PieChart statusChart;
	@UnknownNullability
	@SuppressWarnings("unused")
	public BarChart<String, Number> popularChart;
	private Map<Book, Book.Data> authorBooks = new HashMap<>();

	@Override
	public void loadData() {
		refreshStatus();
		loadPieChartData();
		loadBarChartData();
	}

	private void refreshStatus() {
		final var author = new Author.ByRef(getLoggedInUser()._1());
		authorBooks = repository.bookOps.read(entry -> author.equals(entry.getKey().author()));
	}

	private void loadPieChartData() {
		// Get books by status for the current author
		int approvedBooks = 0;
		int pendingBooks = 0;
		int rejectedBooks = 0;

		for (final var bookEntry : authorBooks.entrySet()) {
			final var data = bookEntry.getValue();
            if (data.approvalStatus() == Book.ApprovalStatus.APPROVED) {
                if(data.originalOrModified()==null) {
                    approvedBooks++;
                }
            } else if (data.approvalStatus() == Book.ApprovalStatus.PENDING) {
                pendingBooks++;
            } else if (data.approvalStatus() == Book.ApprovalStatus.REJECTED) {
                rejectedBooks++;
            }
		}

		List<PieChart.Data> pieChartData = List.of(
				new PieChart.Data("Pending", pendingBooks),
				new PieChart.Data("Approved", approvedBooks),
				new PieChart.Data("Rejected", rejectedBooks)
		);

		statusChart.getData().setAll(pieChartData);
        statusChart.setMinSize(200, 200);
        statusChart.layout();
	}

	@SuppressWarnings("unchecked")
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
		popularChart.getData().setAll(series);
	}
}
