package library.controllers.student_staff;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import library.Context;
import library.Main;
import library.controllers.common.RequiresLoggedIn;
import library.models.BookRequest;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.Alerts;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ResourceBundle;

public class RequestBookController implements RequiresLoggedIn {
    private final Context context = Main.getContext();
    private final Repository repository = context.getRepository();
    private final User user = this.getLoggedInUser()._1();

    @FXML
    private TextField titleField, authorField;

    enum RequestResult {
        REQUEST_SUCCESS,
        REQUEST_INVALID,
        REQUEST_REPEATED,
        REQUEST_ERROR
    }

    @Override
    public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
        RequiresLoggedIn.super.initialize(location, resources);
    }

    @FXML
    private void requestBookButtonAction() {
        String title = titleField.getText();
        String author = authorField.getText();
        RequestResult result = requestBook(title, author);
        switch (result) {
            case REQUEST_SUCCESS -> Alerts.showInfoDialog("Book request submitted.");
            case REQUEST_INVALID -> Alerts.showErrorDialog("Invalid request.");
            case REQUEST_REPEATED -> Alerts.showErrorDialog("Request has already been submitted.");
            case REQUEST_ERROR -> Alerts.showErrorDialog("Unknown error occurred.");
        }
    }

    private RequestResult requestBook(String title, String author) {
        // Check if either title or author field is an empty string
        if (title.isEmpty() || author.isEmpty()) return RequestResult.REQUEST_INVALID;

        BookRequest bookRequest = new BookRequest(title, author);
        BookRequest.Data bookRequestData = new BookRequest.Data(ZonedDateTime.now());

        try {
            // Check if the user has made the same book request in the past
            if (repository.userBookRequestOps.read(user, bookRequest).isPresent())
                return RequestResult.REQUEST_REPEATED;
            else {
                repository.userBookRequestOps.create(user, bookRequest, bookRequestData);
                return RequestResult.REQUEST_SUCCESS;
            }
        } catch (TransactionException e) {
            return RequestResult.REQUEST_ERROR;
        }
    }
}
