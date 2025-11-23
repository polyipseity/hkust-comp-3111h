package library.controllers.common;

import javafx.fxml.Initializable;
import library.Main;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The {@code LoadsData} interface defines behavior for classes that require
 * implementation of data loading mechanisms. It extends {@code Initializable}
 * and {@code RequiresLoggedIn} to ensure initialization and user session validation.
 *
 * Classes implementing this interface must provide logic for loading necessary
 * data in their context. The default behavior also ensures that the data is
 * reloaded automatically based on application timeline events.
 */
public interface LoadsData extends Initializable, RequiresLoggedIn {
    /**
     * Load data.
     */
    void loadData();

	@Override
	default void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		loadData();
		Main.getContext().addMinuteTimelineListener(getLoggedInToken(), _ -> loadData());
	}
}
