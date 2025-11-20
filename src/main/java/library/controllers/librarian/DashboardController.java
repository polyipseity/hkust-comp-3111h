// src/main/java/library/controllers/LibrarianDashboardController.java
package library.controllers.librarian;

import javafx.scene.layout.AnchorPane;
import library.controllers.common.CommonDashboardController;

public class DashboardController extends CommonDashboardController {
	public AnchorPane pendingApprovals;
	public PendingApprovalsController pendingApprovalsController;

	public AnchorPane manageUsers;
	public ManageUsersController manageUsersController;

	public AnchorPane borrowedBooks;
	public BorrowedBooksController borrowedBooksController;

	public AnchorPane publishedBooks;
	public PublishedBooksController publishedBooksController;

	public AnchorPane bookRequests;
	public BookRequestsController bookRequestsController;

	public AnchorPane bookDownload;
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
