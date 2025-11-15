package library.controllers.student_staff;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import library.Context;
import library.Main;
import library.models.Author;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.ByteArray;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

public class AvailableBooksController {
	private final Context context = Main.getContext();
	private final Repository repository = context.getRepository();
	private final User user = context.getLoggedInUser()._1();
	private final User.Data userData = context.getLoggedInUser()._2();

	@FXML
	private Text titleText, authorText, publishDateText, summaryText;
	@FXML
	private TableView<tableRow> table;
	@FXML
	private TableColumn<tableRow, String> titleCol, authorCol, publishDateCol, summaryCol;

	public record tableRow(
			@NotNull @Getter String title,
			@NotNull @Getter String author,
			@NotNull @Getter String publishDate,
			@NotNull @Getter String summary
	) {
	}

	@FXML
	public void initialize() {
		titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
		publishDateCol.setCellValueFactory(new PropertyValueFactory<>("publishDate"));
		summaryCol.setCellValueFactory(new PropertyValueFactory<>("summary"));

		ObservableList<tableRow> data = FXCollections.observableArrayList();
		// TODO: populate data with available books

		table.setItems(data);
		table.getSelectionModel().selectedItemProperty().addListener(
				(_, _, newValue) -> {
					if (newValue != null) {
						titleText.setText(newValue.getTitle());
						authorText.setText(newValue.getAuthor());
						publishDateText.setText(newValue.getPublishDate());
						summaryText.setText(newValue.getSummary());
					}
				}
		);
		table.getSelectionModel().selectFirst();
	}
}
