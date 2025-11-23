package library.controllers.librarian;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Controller to manage the various sections of the dashboard.
 * This class provides methods to load specific dashboard components.
 * It extends {@code CommonDashboardController} which includes shared functionality for all dashboard controllers.
 */
public final class DashboardController extends CommonDashboardController {
    /**
     * The Pending approvals.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node pendingApprovals;
    /**
     * The Pending approvals controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public PendingApprovalsController pendingApprovalsController;

    /**
     * The Manage users.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node manageUsers;
    /**
     * The Manage users controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public ManageUsersController manageUsersController;

    /**
     * The Borrowed books.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node borrowedBooks;
    /**
     * The Borrowed books controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public BorrowedBooksController borrowedBooksController;

    /**
     * The Published books.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node publishedBooks;
    /**
     * The Published books controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public PublishedBooksController publishedBooksController;

    /**
     * The Book requests.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node bookRequests;
    /**
     * The Book requests controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public BookRequestsController bookRequestsController;

    /**
     * The Book download.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public Node bookDownload;
    /**
     * The Book download controller.
     */
    @UnknownNullability
	@SuppressWarnings("unused")
	public BookDownloadController bookDownloadController;

    /**
     * Load pending approvals.
     */
    public void loadPendingApprovals() {
		pendingApprovalsController.loadData();
	}

    /**
     * Load manage users.
     */
    public void loadManageUsers() {
		manageUsersController.loadData();
	}

    /**
     * Load borrowed books.
     */
    public void loadBorrowedBooks() {
		borrowedBooksController.loadData();
	}

    /**
     * Load published books.
     */
    public void loadPublishedBooks() {
		publishedBooksController.loadData();
	}

    /**
     * Load book requests.
     */
    public void loadBookRequests() {
		bookRequestsController.loadData();
	}

    /**
     * Load book download.
     */
    public void loadBookDownload() {
		bookDownloadController.loadData();
	}
}
