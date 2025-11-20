package library.controllers.librarian;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;

public final class DashboardController extends CommonDashboardController {
	public Node pendingApprovals;
	public PendingApprovalsController pendingApprovalsController;

	public Node manageUsers;
	public ManageUsersController manageUsersController;

	public Node borrowedBooks;
	public BorrowedBooksController borrowedBooksController;

	public Node publishedBooks;
	public PublishedBooksController publishedBooksController;

	public Node bookRequests;
	public BookRequestsController bookRequestsController;

	public Node bookDownload;
	public BookDownloadController bookDownloadController;

	public void loadPendingApprovals() {
		pendingApprovalsController.loadTable();
	}

	public void loadManageUsers() {
		manageUsersController.loadTable();
	}

	public void loadBorrowedBooks() {
		borrowedBooksController.loadTable();
	}

	public void loadPublishedBooks() {
		publishedBooksController.loadTable();
	}

	public void loadBookRequests() {
		bookRequestsController.loadTable();
	}

	public void loadBookDownload() {
		bookDownloadController.loadTable();
	}
}
