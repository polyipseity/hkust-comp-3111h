package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import library.controllers.common.CommonDashboardController;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public final class DashboardController extends CommonDashboardController {

    @FXML
    public GridPane authorPublishBooks;

    @FXML
    public VBox authorMyBooks;

    public AuthorMyBooksController authorMyBooksController;
    public AuthorPublishBooksController authorPublishBooksController;

    @Override
    public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
        super.initialize(location, resources);

        authorMyBooksController.setParentController(this);
        authorPublishBooksController.setParentController(this);
    }
}
