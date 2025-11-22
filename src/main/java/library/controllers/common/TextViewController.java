package library.controllers.common;

import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.net.URL;
import java.util.ResourceBundle;

@RequiredArgsConstructor
public final class TextViewController implements RequiresLoggedIn, Initializable {
	public static final String WINDOW_TITLE = "Reading: %s.txt";
	@Getter
	public final String content;
	@UnknownNullability
	public TextArea contentArea;
	@UnknownNullability
	public Slider zoomSlider;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		zoomSlider.valueProperty().addListener((_, _, newValue) ->
				contentArea.setFont(Font.font(contentArea.getFont().getFamily(), newValue.doubleValue())));
	}
}
