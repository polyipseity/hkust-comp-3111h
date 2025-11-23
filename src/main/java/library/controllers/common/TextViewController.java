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

/**
 * The {@code TextViewController} class is a final controller class that
 * manages the textual content display for a UI component. It implements
 * both {@code RequiresLoggedIn} and {@code Initializable}, ensuring user
 * authentication and proper initialization of its components.
 *
 * This controller is primarily designed to handle and render textual content
 * while enabling user interaction such as zooming functionality via a slider.
 */
@RequiredArgsConstructor
public final class TextViewController implements RequiresLoggedIn, Initializable {
    /**
     * The constant WINDOW_TITLE.
     */
    public static final String WINDOW_TITLE = "Reading: '%s' by '%s'";

    /**
     * The Content.
     */
    @Getter
	public final String content;
    /**
     * The Content area.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public TextArea contentArea;
    /**
     * The Zoom slider.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Slider zoomSlider;

	@SuppressWarnings("unused") // for testing
	private TextViewController() {
		this.content = "";
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		zoomSlider.valueProperty().addListener((_, _, newValue) ->
				contentArea.setFont(Font.font(contentArea.getFont().getFamily(), newValue.doubleValue())));
	}
}
