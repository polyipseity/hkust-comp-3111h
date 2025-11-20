package library.controls;

import library.models.Author;
import library.models.Book;
import library.persistence.Repository;
import library.persistence.TransactionException;
import library.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;

public record ManageBooksControl(@NotNull Repository repository) {
	public static final @NotNull String NOTIFICATION_APPROVE = "Your book '%s' has been approved!";
	public static final @NotNull String NOTIFICATION_REJECT = "Your book '%s' has been rejected!";
	public static final @NotNull String NOTIFICATION_DELETE_BOOK = "Your book '%s' has been deleted!";
	public static final @NotNull String NOTIFICATION_DELETE_BORROWED_BOOK = "The book '%s' you were borrowing has been deleted!";

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
	public @NotNull ApproveResult approveBook(@NotNull Book pending) throws TransactionException {
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
	public @NotNull RejectResult rejectBook(@NotNull Book pending) throws TransactionException {
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

	public @NotNull Book.Data modifyBookData(@NotNull Book.Data oldData, @NotNull Book.Data newData, boolean newIsTemporary) {
		final var ret = oldData
				.withSummary(newData.summary())
				.withContent(newData.content())
				.withApprovalStatus(Book.ApprovalStatus.APPROVED)
				.withPublishDate(oldData.publishDate() == null ? TimeUtil.nowZoned() : oldData.publishDate());
		return newIsTemporary ? ret : ret.withOriginalOrModified(newData.originalOrModified());
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
	 * Deletes a book and notifies the author and all borrowers.
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
	public @NotNull DeleteResult deleteBook(@NotNull Book book) throws TransactionException {
		repository.transact(_ -> {
			// Read the current book data to ensure it exists
			final var bookData = repository.bookOps.readOrThrow(book);

			// Retrieve all borrows associated with the book
			final var borrows = repository.borrowOps.read(book);
			// Process each borrow and remove it from the database
			for (final var borrowEntry : borrows.entrySet()) {
				repository.borrowOps.delete(borrowEntry.getKey(), book, borrowEntry.getValue());
			}
			// After removing all borrows, delete the book
			repository.bookOps.delete(book, bookData);

			// Notify the author of the book deletion
			if (book.author() instanceof Author.ByRef(final var author)) {
				repository.userNotificationOps.updateAsList(author, notifications -> {
					notifications.add(NOTIFICATION_DELETE_BOOK.formatted(book.title()));
				});
			}
			// Notify each borrower about the deletion
			for (final var borrower : borrows.keySet()) {
				repository.userNotificationOps.updateAsList(borrower, notifications -> {
					notifications.add(NOTIFICATION_DELETE_BORROWED_BOOK.formatted(book.title()));
				});
			}

			return true;
		}, () -> "Failed to delete book: %s".formatted(book));
		return new DeleteResult.Success();
	}

	/**
	 * Result type for the deletion operation.
	 */
	public sealed interface DeleteResult permits DeleteResult.Success {
		record Success() implements DeleteResult {
		}
	}
}
