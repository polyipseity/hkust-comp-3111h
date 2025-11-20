package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import library.controllers.common.RequiresLoggedIn;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public final class AuthorViewWindowController implements RequiresLoggedIn {
    @FXML
    private TextArea ContentArea;

    @FXML
    private Slider ZoomSlider;

    double prefSize = 20;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		ZoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			ContentArea.setFont(Font.font(ContentArea.getFont().getFamily(), newValue.doubleValue()));
			ScrollBar vertScrollBar = (ScrollBar) ContentArea.lookup(".scroll-bar:vertical");
			vertScrollBar.setPrefWidth(prefSize);
		});
	}

    public void setContent(String content) {
        ContentArea.setText(content);
    }
}
