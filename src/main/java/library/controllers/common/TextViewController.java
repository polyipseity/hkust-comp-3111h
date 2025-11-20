package library.controllers.common;

import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public final class TextViewController implements RequiresLoggedIn, Initializable {
	public TextArea contentArea;
	public Slider zoomSlider;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		contentArea.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/common/TextViewContentArea.css")).toExternalForm());
		zoomSlider.valueProperty().addListener((_, _, newValue) ->
				contentArea.setFont(Font.font(contentArea.getFont().getFamily(), newValue.doubleValue())));
	}

	public void setContent(@NotNull String content) {
		contentArea.setText(content);
	}
}
