package library.controllers.student_staff;

import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingNode;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import library.controllers.common.RequiresLoggedIn;
import library.controllers.common.TextViewController;
import library.utils.Alerts;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.util.FontPropertiesManager;
import org.icepdf.ri.util.ViewerPropertiesManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ResourceBundle;

public final class BookViewController implements RequiresLoggedIn, Initializable {
	public static final String WINDOW_TITLE = TextViewController.WINDOW_TITLE;

	public final Stage stage;
	private final String currentPath;
	@UnknownNullability
	public BorderPane borderPane;
	private SwingController swingController;
	private JComponent viewerPanel;

	public BookViewController(Stage stage, String path) {
		this.stage = stage;
		currentPath = path;
	}

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		RequiresLoggedIn.super.initialize(location, resources);
		createViewer();

		stage.setOnHiding(_ -> SwingUtilities.invokeLater(() -> swingController.dispose()));
		final var scene = stage.sceneProperty();
		final var sceneWidth = scene.flatMap(Scene::widthProperty);
		final var sceneHeight = scene.flatMap(Scene::heightProperty);
		Bindings.createObjectBinding(
				() -> {
					final var width = sceneWidth.getValue();
					final var height = sceneHeight.getValue();
					return width == null || height == null ? null : new Dimension(width.intValue(), height.intValue());
				},
				sceneWidth,
				sceneHeight
		).addListener((_, _, newValue) -> {
			if (newValue == null) return;
			viewerPanel.setSize(newValue);
			viewerPanel.setPreferredSize(newValue);
			viewerPanel.repaint();
		});

		load();
	}

	public void load() {
		SwingUtilities.invokeLater(() -> {
			swingController.openDocument(currentPath);
			viewerPanel.revalidate();
		});
	}

	public void save() {
		try (FileOutputStream os = new FileOutputStream(currentPath)) {
			SwingUtilities.invokeLater(() -> {
				try {
					swingController.getDocument().writeToOutputStream(os);
				} catch (IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}
				viewerPanel.revalidate();
			});
			Alerts.showInfoDialog("Changes saved.");
		} catch (Exception e) {
			Alerts.showErrorDialog("Could not save changes.");
		}
	}

	private void createViewer() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				swingController = new SwingController();
				swingController.setIsEmbeddedComponent(true);

				FontPropertiesManager.getInstance().loadOrReadSystemFonts();

				ViewerPropertiesManager properties = ViewerPropertiesManager.getInstance();
				properties.getPreferences().putBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_OPEN, false);
				properties.getPreferences().putBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_SAVE, false);
				properties.getPreferences().putBoolean(ViewerPropertiesManager.PROPERTY_SHOW_UTILITY_PRINT, false);
				properties.getPreferences().putBoolean(ViewerPropertiesManager.PROPERTY_SHOW_STATUSBAR, false);

				swingController.getDocumentViewController().setAnnotationCallback(
						new org.icepdf.ri.common.MyAnnotationCallback(swingController.getDocumentViewController()));

				SwingViewBuilder factory = new SwingViewBuilder(swingController, properties);

				// add toolbar to the top.
				FlowPane toolBarFlow = new FlowPane();
				JToolBar mainToolbar = factory.buildCompleteToolBar(true);
				buildJToolBar(toolBarFlow, mainToolbar);
				borderPane.setTop(toolBarFlow);

				SwingNode swingNode = new SwingNode();
				viewerPanel = factory.buildUtilityAndDocumentSplitPane(true);
				swingNode.setContent(viewerPanel);
				borderPane.setCenter(swingNode);
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void buildJToolBar(FlowPane flowPane, JToolBar jToolBar) {
		SwingNode swingNode = new SwingNode();
		swingNode.setContent(jToolBar);
		flowPane.getChildren().add(swingNode);
	}
}
