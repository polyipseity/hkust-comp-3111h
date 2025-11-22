package library.controllers.librarian;

import javafx.scene.Node;
import library.controllers.common.CommonDashboardController;
import org.jetbrains.annotations.UnknownNullability;

public final class DashboardController extends CommonDashboardController {
	@UnknownNullability
	@SuppressWarnings("unused")
	public Node pendingApprovals;
	@UnknownNullability
	@SuppressWarnings("unused")
	public PendingApprovalsController pendingApprovalsController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node manageUsers;
	@UnknownNullability
	@SuppressWarnings("unused")
	public ManageUsersController manageUsersController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node borrowedBooks;
	@UnknownNullability
	@SuppressWarnings("unused")
	public BorrowedBooksController borrowedBooksController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node publishedBooks;
	@UnknownNullability
	@SuppressWarnings("unused")
	public PublishedBooksController publishedBooksController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node bookRequests;
	@UnknownNullability
	@SuppressWarnings("unused")
	public BookRequestsController bookRequestsController;

	@UnknownNullability
	@SuppressWarnings("unused")
	public Node bookDownload;
	@UnknownNullability
	@SuppressWarnings("unused")
	public BookDownloadController bookDownloadController;

	public void loadPendingApprovals() {
		pendingApprovalsController.loadData();
	}

	public void loadManageUsers() {
		manageUsersController.loadData();
	}

	public void loadBorrowedBooks() {
		borrowedBooksController.loadData();
	}

	public void loadPublishedBooks() {
		publishedBooksController.loadData();
	}

	public void loadBookRequests() {
		bookRequestsController.loadData();
	}

	public void loadBookDownload() {
		bookDownloadController.loadData();
	}
}
