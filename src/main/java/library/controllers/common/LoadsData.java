package library.controllers.common;

import javafx.fxml.Initializable;
import library.Main;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

public interface LoadsData extends Initializable, RequiresLoggedIn {
	void loadData();

	@Override
	default void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);

		loadData();
		Main.getContext().addMinuteTimelineListener(getLoggedInToken(), _ -> loadData());
	}
}
