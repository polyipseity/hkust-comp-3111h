package library.controllers.author;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import library.controllers.common.CommonDashboardController;
import library.controllers.common.InformBoardController;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The {@code DashboardController} class manages the loading and interaction of different
 * dashboard components such as "My Books", "Status View", and "Information Board".
 * It extends {@code CommonDashboardController}, inheriting its functionality.
 * This class acts as a controller in an MVC architecture, coordinating between
 * user interface elements and their respective data controllers.
 * <p>
 * This controller interacts with the following:
 * - {@code MyBooksController} for managing books.
 * - {@code StatusViewController} for displaying the current status views.
 * - {@code InformBoardController} for managing the informational board.
 * <p>
 * The main responsibilities of this class include:
 * - Controlling the lifecycle and data loading of dashboard components.
 * - Ensuring specific components load their data when appropriate.
 * - Managing user interaction for each component.
 */
public final class DashboardController extends CommonDashboardController {
    /**
     * The My books.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node myBooks;
    /**
     * The My books controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public MyBooksController myBooksController;

    /**
     * The Status view.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node statusView;
    /**
     * The Status view controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public StatusViewController statusViewController;

    /**
     * The Inform board.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node informBoard;
    /**
     * The Inform board controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public InformBoardController informBoardController;

    @FXML
    private Tab statusViewTab;

    /**
     * Loads the user's books into the "My Books" view of the dashboard.
     * <p>
     * This method triggers the `loadData` method of `MyBooksController`,
     * which is responsible for retrieving and displaying the active books
     * associated with the currently logged-in author.
     * <p>
     * The data is fetched from the repository and filtered based on the
     * active status and the author's information, ensuring only relevant
     * books are displayed. Once retrieved, the data is set into the
     * table controller for visualization in the user interface.
     * <p>
     * This method is intended to be called when the "My Books" component
     * of the dashboard needs to be populated or refreshed.
     */
    public void loadMyBooks() {
		myBooksController.loadData();
	}

    /**
     * Loads the data for the "Status View" tab when it is selected.
     * This method ensures that the status view's data is refreshed
     * and properly displayed by invoking the {@code loadData()} method
     * of the {@code StatusViewController}.
     * <p>
     * It first checks if the "Status View" tab is currently selected
     * using the {@code statusViewTab.isSelected()} condition. If true,
     * it calls the {@code loadData()} method of the associated
     * {@code StatusViewController}.
     */
    public void loadStatusView() {
        if(statusViewTab.isSelected()){
            statusViewController.loadData();
        }
	}

    /**
     * Loads the data for the "Inform Board" section of the dashboard.
     * <p>
     * This method triggers the `loadData` method of the `InformBoardController`,
     * which is responsible for fetching and setting up the notifications to
     * be displayed in the inform board.
     * <p>
     * The `InformBoardController` retrieves notifications pertaining to the
     * currently logged-in user from the repository, ensuring the displayed
     * content is user-specific and up-to-date. Once the data is fetched, it
     * is set into a list for visualization in the user interface.
     * <p>
     * This method is intended to be called when the "Inform Board" component
     * of the dashboard needs to be populated or refreshed.
     */
    public void loadInformBoard() {
		informBoardController.loadData();
	}
}
