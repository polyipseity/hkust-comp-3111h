package library.controllers.common;

import javafx.fxml.Initializable;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public final class TextViewController implements RequiresLoggedIn, Initializable {
	public static final double CONTENT_SCROLL_BAR_PREF_WIDTH = 20;

	public TextArea contentArea;
	public Slider zoomSlider;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		ScrollBar vertScrollBar = (ScrollBar) contentArea.lookup(".scroll-bar:vertical");
		vertScrollBar.setPrefWidth(CONTENT_SCROLL_BAR_PREF_WIDTH);
		zoomSlider.valueProperty().addListener((_, _, newValue) ->
				contentArea.setFont(Font.font(contentArea.getFont().getFamily(), newValue.doubleValue())));
	}

	public void setContent(@NotNull String content) {
		contentArea.setText(content);
	}
}
