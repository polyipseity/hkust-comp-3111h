package library.controllers.student_staff;

import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.WindowEvent;
import library.utils.Alerts;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.util.FontPropertiesManager;
import org.icepdf.ri.util.ViewerPropertiesManager;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public final class BookViewController {
    @FXML
    private BorderPane borderPane;

    private SwingController swingController;
    private JComponent viewerPanel;

    private final String currentPath;

    public BookViewController(String path) { currentPath = path; }

    @FXML
    private void initialize() {
        createViewer();
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

    public void createResizeListeners(WindowEvent event) {
        Scene scene = borderPane.getScene();
        scene.widthProperty().addListener((_, _, newValue) -> SwingUtilities.invokeLater(() -> {
            Dimension target = new Dimension(newValue.intValue(), (int) scene.getHeight());
            viewerPanel.setSize(target);
            viewerPanel.setPreferredSize(target);
            viewerPanel.repaint();
        }));

        scene.heightProperty().addListener((_, _, newValue) -> SwingUtilities.invokeLater(() -> {
            Dimension target = new Dimension((int) scene.getWidth(), newValue.intValue());
            viewerPanel.setSize(target);
            viewerPanel.setPreferredSize(target);
            viewerPanel.repaint();
        }));
    }

    public void disposeController(WindowEvent event) {
        SwingUtilities.invokeLater(() -> swingController.dispose());
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

    private void buildJToolBar(FlowPane flowPane, JToolBar jToolBar){
        SwingNode swingNode = new SwingNode();
        swingNode.setContent(jToolBar);
        flowPane.getChildren().add(swingNode);
    }
}
