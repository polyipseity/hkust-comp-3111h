package library.controls;

import library.models.Author;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.HasMessage;
import library.utils.TimeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public record ManageBooksControl(Repository repository) {
	public static final String NOTIFICATION_APPROVE = "Your book '%s' has been approved!";
	public static final String NOTIFICATION_REJECT = "Your book '%s' has been rejected!";
	public static final String NOTIFICATION_DELETE_BOOK = "Your book '%s' has been deleted!";
	public static final String NOTIFICATION_DELETE_BORROWED_BOOK = "The book '%s' you were borrowing has been deleted!";

	/**
	 * Approves a pending book and updates the database in one transaction.
	 *
	 * <p>For a pending book without an {@code originalOrModified} reference,
	 * it becomes published immediately with the current timestamp as its publish date.</p>
	 *
	 * <p>If the pending book has an {@code originalOrModified} reference:
	 * <ul>
	 *   <li>When the pending book is temporary, the data of the referenced
	 *       original/modified book is updated to match the pending book’s
	 *       summary and content, and the temporary book is removed.</li>
	 *   <li>When it is not temporary, the publish date of the referenced
	 *       book is copied to the pending book, the pending book becomes
	 *       published, and the old book record is deleted.</li>
	 * </ul></p>
	 *
	 * <p>The author of the book (or the original/modified book) receives a
	 * notification about the approval outcome.</p>
	 */
	public ApproveResult approveBook(Book pending) throws TransactionException {
		repository.transact(_ -> {
			// read current data for the pending book
			final var pendingData = repository.bookOps.readOrThrow(pending);
			if (Book.ApprovalStatus.PENDING != pendingData.approvalStatus()) {
				throw new IllegalArgumentException("Book is not pending: %s".formatted(pending));
			}

			// update books
			switch (pendingData) {
				case Book.Data(_, _, _, _, final Book original, _) when original == null -> {
					// no original/modified reference → publish directly
					final var newData = pendingData.withPublishDate(TimeUtil.nowZoned()).withApprovalStatus(Book.ApprovalStatus.APPROVED);
					repository.bookOps.update(pending, newData, pendingData);
				}
				case Book.Data(_, _, _, _, final Book original, _) -> {
					final var originalData = repository.bookOps.readOrThrow(original);
					// has original/modified reference
					if (pending.temporary()) {
						// temporary → use original
						final var newData = modifyBookData(originalData, pendingData, true);
						repository.bookOps.update(original, newData, originalData);
						repository.bookOps.delete(pending, pendingData);
					} else {
						// not temporary → use new
						final var newData = modifyBookData(originalData, pendingData, false);
						repository.bookOps.update(pending, newData, pendingData);
						repository.bookOps.delete(original, originalData);
					}
				}
			}

			// notify the author
			if (pending.author() instanceof Author.ByRef(final var author)) {
				repository.userNotificationOps.updateAsList(author, notifications -> {
					notifications.add(NOTIFICATION_APPROVE.formatted(pending.title()));
				});
			}
			return true;
		}, () -> "Failed to approve book: %s".formatted(pending));
		return new ApproveResult.Success();
	}

	/**
	 * Rejects a pending book and updates the database in one transaction.
	 *
	 * <p>For a pending book without an {@code originalOrModified} reference,
	 * it becomes rejected immediately.</p>
	 *
	 * <p>If the pending book has an {@code originalOrModified} reference,
	 * the pending record is simply deleted – the referenced original/modified
	 * book remains unchanged.</p>
	 *
	 * <p>The author of the book receives a notification about the rejection.</p>
	 *
	 * @param pending the pending book to reject; must be in {@link Book.ApprovalStatus#PENDING} state
	 * @return a {@link RejectResult} indicating success (no failure path is exposed)
	 * @throws TransactionException if any database operation fails during the transaction
	 */
	public RejectResult rejectBook(Book pending) throws TransactionException {
		repository.transact(_ -> {
			// read current data for the pending book
			final var pendingData = repository.bookOps.readOrThrow(pending);
			if (Book.ApprovalStatus.PENDING != pendingData.approvalStatus()) {
				throw new IllegalArgumentException("Book is not pending: %s".formatted(pending));
			}

			// update books according to the presence of an original/modified reference
			switch (pendingData) {
				case Book.Data(_, _, _, _, final Book original, _) when original == null -> {
					// no original/modified reference → just reject it
					final var newData = pendingData
							.withApprovalStatus(Book.ApprovalStatus.REJECTED);
					repository.bookOps.update(pending, newData, pendingData);
				}
				case Book.Data val -> // has an original/modified reference → delete the pending book
						repository.bookOps.delete(pending, val);
			}

			// notify the author of the rejection
			if (pending.author() instanceof Author.ByRef(final var author)) {
				repository.userNotificationOps.updateAsList(author,
						notifications -> {
							notifications.add(NOTIFICATION_REJECT.formatted(pending.title()));
						});
			}
			return true;
		}, () -> "Failed to reject book: %s".formatted(pending));
		return new RejectResult.Success();
	}

	public Book.Data modifyBookData(Book.Data oldData, Book.Data newData, boolean newIsTemporary) {
		final var ret = oldData
				.withSummary(newData.summary())
				.withContent(newData.content())
				.withApprovalStatus(Book.ApprovalStatus.APPROVED)
				.withPublishDate(oldData.publishDate() == null ? TimeUtil.nowZoned() : oldData.publishDate());
		return newIsTemporary ? ret : ret.withOriginalOrModified(newData.originalOrModified());
	}

	/**
	 * Deletes a book and notifies the author (if not deleted by author) and all borrowers.
	 *
	 * <p>Before deleting the book, it retrieves all borrows associated with the book.
	 * It then removes each borrow record from the database.</p>
	 *
	 * <p>After removing the borrows, it notifies the author and all borrowers about
	 * the deletion of the book.</p>
	 *
	 * @param book the book to delete; must exist in the database
	 * @return a {@link DeleteResult} indicating success or failure
	 * @throws TransactionException if any database operation fails during the transaction
	 */
	public DeleteResult deleteBook(Book book, User.Role role) throws TransactionException {
		return switch (role) {
			case STUDENT_STAFF -> new DeleteResult.BadRole(role);
			case AUTHOR, LIBRARIAN -> {
				final var ret = new AtomicReference<@Nullable DeleteResult>();
				try {
					repository.transact(_ -> {
						// Read the current book data to ensure it exists
						final var bookData = repository.bookOps.readOrThrow(book);

						// Retrieve all borrows associated with the book
						final var borrows = repository.borrowOps.read(book);
						if (role != User.Role.LIBRARIAN && !borrows.isEmpty()) {
							ret.set(new DeleteResult.HasBorrows(borrows));
							return false;
						}

						// Process each borrow and remove it from the database
						for (final var borrowEntry : borrows.entrySet()) {
							repository.borrowOps.delete(borrowEntry.getKey(), book, borrowEntry.getValue());
						}
						// After removing all borrows, delete the book
						repository.bookOps.delete(book, bookData);


						if (role != User.Role.AUTHOR) {
							// Notify the author of the book deletion
							if (book.author() instanceof Author.ByRef(final var author)) {
								repository.userNotificationOps.updateAsList(author, notifications -> {
									notifications.add(NOTIFICATION_DELETE_BOOK.formatted(book.title()));
								});
							}
						}
						// Notify each borrower about the deletion
						for (final var borrower : borrows.keySet()) {
							repository.userNotificationOps.updateAsList(borrower, notifications -> {
								notifications.add(NOTIFICATION_DELETE_BORROWED_BOOK.formatted(book.title()));
							});
						}

						return true;
					}, () -> "Failed to delete book: %s".formatted(book));
				} catch (TransactionException e) {
					final var ret2 = ret.get();
					if (ret2 != null) {
						yield ret2;
					}
					throw e;
				}
				yield new DeleteResult.Success();
			}
		};
	}

	public ModifyResult modifyBook(Book book, String title, String summary) throws TransactionException {
		final var ret = new AtomicReference<@Nullable ModifyResult>();
		try {
			repository.transact(_ -> {
				// Read the current book data to ensure it exists
				final var bookData = repository.bookOps.readOrThrow(book);

				if (title.equals(book.title()) && summary.equals(bookData.summary())) {
					ret.set(new ModifyResult.SameDetails(bookData));
					return false;
				}

				// Check if the book is pending or approved
				return switch (bookData.approvalStatus()) {
					case PENDING -> {
						// New book
						final var newBook = new Book(title, book.author(), switch (bookData.originalOrModified()) {
							case null -> false;
							case Book(final String title2, _, _) -> title2.equals(title);
						});
						final var newBookData = bookData.withSummary(summary);

						// Update or create it
						if (newBook.equals(book)) {
							// Update book in place
							repository.bookOps.update(book, newBookData, bookData);
						} else {
							// Delete first to remove the original book link
							repository.bookOps.delete(book, bookData);
							// Add later to add the original book link
							try {
								repository.bookOps.create(newBook, newBookData);
							} catch (TransactionException e) {
								ret.set(new ModifyResult.AlreadyExists(newBook));
								throw e;
							}
						}

						ret.set(new ModifyResult.Success(newBook, newBookData));
						yield true;
					}
					case APPROVED -> {
						// Check if the book is borrowed by any student/staff
						final var borrows = repository.borrowOps.read(book);
						if (!borrows.isEmpty()) {
							ret.set(new ModifyResult.HasBorrows(borrows));
							yield false;
						}
						// New book
						final var newBook = new Book(title, book.author(), title.equals(book.title()));
						final var newBookData = bookData
								.withSummary(summary)
								.withApprovalStatus(Book.ApprovalStatus.PENDING)
								.withOriginalOrModified(book);
						// Create it
						try {
							repository.bookOps.create(newBook, newBookData);
						} catch (TransactionException e) {
							ret.set(new ModifyResult.AlreadyExists(newBook));
							throw e;
						}

						ret.set(new ModifyResult.Success(newBook, newBookData));
						yield true;
					}
					case REJECTED -> {
						ret.set(new ModifyResult.AlreadyRejected(bookData));
						yield false;
					}
				};
			}, () -> "Failed to modify book: %s, %s, %s".formatted(book, title, summary));
		} catch (TransactionException e) {
			final var ret2 = ret.get();
			if (ret2 != null) {
				return ret2;
			}
			throw e;
		}
		return Objects.requireNonNull(ret.get());
	}

	/**
	 * Result type for the approval operation.
	 */
	public sealed interface ApproveResult permits ApproveResult.Success {
		record Success() implements ApproveResult {
		}
	}

	/**
	 * Result type for the rejection operation.
	 */
	public sealed interface RejectResult permits RejectResult.Success {
		record Success() implements RejectResult {
		}
	}

	/**
	 * Result type for the deletion operation.
	 */
	public sealed interface DeleteResult permits DeleteResult.BadRole, DeleteResult.HasBorrows, DeleteResult.Success {
		record Success() implements DeleteResult {
		}

		record BadRole(User.Role role) implements DeleteResult, HasMessage {
			@Override
			public String getMessage() {
				return "Bad role: %s".formatted(role.name);
			}
		}

		record HasBorrows(Map<User, Borrow> borrows) implements DeleteResult, HasMessage {
			@Override
			public String getMessage() {
				return "Cannot modify a book that is borrowed by students/staff";
			}
		}
	}

	/**
	 * Result type for the modification operation.
	 */
	public sealed interface ModifyResult permits ModifyResult.HasBorrows, ModifyResult.AlreadyExists, ModifyResult.AlreadyRejected, ModifyResult.SameDetails, ModifyResult.Success {
		record Success(Book newBook, Book.Data newBookData) implements ModifyResult, HasMessage {
			@Override
			public String getMessage() {
				return "Book updated and waiting for approval";
			}
		}

		record SameDetails(Book.Data oldBookData) implements ModifyResult, HasMessage {
			@Override
			public String getMessage() {
				return "Book details are the same";
			}
		}

		record AlreadyExists(Book conflictBook) implements ModifyResult, HasMessage {
			@Override
			public String getMessage() {
				return "Book of the same title and author already exists";
			}
		}

		record HasBorrows(Map<User, Borrow> borrows) implements ModifyResult, HasMessage {
			@Override
			public String getMessage() {
				return "Cannot modify a book that is borrowed by students/staff";
			}
		}

		record AlreadyRejected(Book.Data oldBookData) implements ModifyResult, HasMessage {
			@Override
			public String getMessage() {
				return "Cannot modify a rejected book";
			}
		}
	}
}
