package library.controllers.student_staff;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import library.controllers.common.CommonDashboardController;

public final class DashboardController extends CommonDashboardController {
    @FXML
    SplitPane availableBooks;
    @FXML
    BorderPane borrowedBooks;

    public AvailableBooksController availableBooksController;
    public BorrowedBooksController borrowedBooksController;

    public void loadAvailableBooks() {
        availableBooksController.reload();
    }

    public void loadBorrowedBooks() {
        borrowedBooksController.reload();
    }
}
