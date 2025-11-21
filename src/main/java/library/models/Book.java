package library.models;

import library.utils.ZonedDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Comparator;

public record Book(
		String title,
		Author author,
		boolean temporary
) implements Comparable<Book> {
	public Book(String title, Author author) {
		this(title, author, false);
	}

	/**
	 * Compares this object with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 *
	 * <p>The implementor must ensure {@link Integer#signum
	 * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
	 * all {@code x} and {@code y}.  (This implies that {@code
	 * x.compareTo(y)} must throw an exception if and only if {@code
	 * y.compareTo(x)} throws an exception.)
	 *
	 * <p>The implementor must also ensure that the relation is transitive:
	 * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
	 * {@code x.compareTo(z) > 0}.
	 *
	 * <p>Finally, the implementor must ensure that {@code
	 * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
	 * == signum(y.compareTo(z))}, for all {@code z}.
	 *
	 * @param o the object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object
	 * is less than, equal to, or greater than the specified object.
	 * @throws NullPointerException if the specified object is null
	 * @throws ClassCastException   if the specified object's type prevents it
	 *                              from being compared to this object.
	 * @apiNote It is strongly recommended, but <i>not</i> strictly required that
	 * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
	 * class that implements the {@code Comparable} interface and violates
	 * this condition should clearly indicate this fact.  The recommended
	 * language is "Note: this class has a natural ordering that is
	 * inconsistent with equals."
	 */
	@Override
	public int compareTo(Book o) {
		return Comparator
				.comparing(Book::title)
				.thenComparing(Book::author)
				.thenComparingInt(book -> book.temporary ? 1 : 0)
				.compare(this, o);
	}

	public enum ApprovalStatus {
		PENDING("pending"),
		APPROVED("approved"),
		REJECTED("rejected"),
		;

		public final String name;

		ApprovalStatus(String name) {
			this.name = name;
		}
	}

	@With
	public record Data(
			String summary,
			String content,
			ApprovalStatus approvalStatus,
			@Nullable ZonedDateTime publishDate,
			@Nullable Book originalOrModified,
			long timesBorrowed
	) {
		/**
		 * Indicates whether this book data represents a publicly available, published edition.
		 *
		 * <p>A {@code Data} instance is considered published when the following conditions are met:
		 * <ul>
		 *   <li>The {@link ApprovalStatus#APPROVED} status has been granted.</li>
		 *   <li>It is not an edited or derived copy of another book
		 *       (i.e. {@code originalOrModified == null}).</li>
		 * </ul>
		 *
		 * <p>Both conditions must hold simultaneously; if either fails, the method returns {@code false}.
		 *
		 * @return {@code true} when the data is approved and not a modification of an existing book,
		 * otherwise {@code false}
		 */
		public boolean published() {
			return switch (approvalStatus) {
				case PENDING, REJECTED -> false;
				case APPROVED -> originalOrModified == null;
			};
		}

		public boolean active() {
			return switch (approvalStatus) {
				case PENDING -> true;
				case APPROVED -> originalOrModified == null;
				case REJECTED -> false;
			};
		}

		@RequiredArgsConstructor
		public static final class S extends GroupSerializerObjectArray<Data> {
			private final Serializer<Book> bookSerializer;

			/**
			 * Serializes the content of the given value into the given
			 * {@link DataOutput2}.
			 *
			 * @param out   DataOutput2 to save object into
			 * @param value Object to serialize
			 * @throws IOException in case of an I/O error
			 */
			@Override
			public void serialize(DataOutput2 out, Data value) throws IOException {
				out.writeUTF(value.summary());
				out.writeUTF(value.content());
				out.writeInt(value.approvalStatus().ordinal());
				if (value.publishDate() == null) {
					out.writeBoolean(false);  // flag that it is absent
				} else {
					out.writeBoolean(true);
					ZonedDateTimeSerializer.INSTANCE.serialize(out, value.publishDate());
				}
				if (value.originalOrModified() == null) {
					out.writeBoolean(false);  // flag that it is absent
				} else {
					out.writeBoolean(true);
					bookSerializer.serialize(out, value.originalOrModified());
				}
				out.writeLong(value.timesBorrowed());
			}

			/**
			 * Deserializes and returns the content of the given {@link DataInput2}.
			 *
			 * @param input     DataInput2 to de-serialize data from
			 * @param available how many bytes that are available in the DataInput2 for
			 *                  reading, may be -1 (in streams) or 0 (null).
			 * @return the de-serialized content of the given {@link DataInput2}
			 * @throws IOException in case of an I/O error
			 */
			@Override
			public Data deserialize(DataInput2 input, int available) throws IOException {
				final var summary = input.readUTF();
				final var content = input.readUTF();
				final var status = ApprovalStatus.values()[input.readInt()];
				@SuppressWarnings("SwitchStatementWithTooFewBranches") final var publishDate = switch (input.readBoolean() ? 1 : 0) {
					case 1 -> ZonedDateTimeSerializer.INSTANCE.deserialize(input, available);
					default -> null;
				};
				@SuppressWarnings("SwitchStatementWithTooFewBranches") final var originalOrModified = switch (input.readBoolean() ? 1 : 0) {
					case 1 -> bookSerializer.deserialize(input, available);
					default -> null;
				};
				final var times = input.readLong();
				return new Data(summary, content, status, publishDate, originalOrModified, times);
			}
		}
	}

	@RequiredArgsConstructor
	public static final class S extends GroupSerializerObjectArray<Book> {
		private final Serializer<Author> authorSerializer;

		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(DataOutput2 out, Book value) throws IOException {
			out.writeUTF(value.title());
			authorSerializer.serialize(out, value.author());
			out.writeBoolean(value.temporary());
		}

		/**
		 * Deserializes and returns the content of the given {@link DataInput2}.
		 *
		 * @param input     DataInput2 to de-serialize data from
		 * @param available how many bytes that are available in the DataInput2 for
		 *                  reading, may be -1 (in streams) or 0 (null).
		 * @return the de-serialized content of the given {@link DataInput2}
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public Book deserialize(DataInput2 input, int available) throws IOException {
			final var title = input.readUTF();
			final var author = authorSerializer.deserialize(input, available);
			final var temporary = input.readBoolean();
			return new Book(title, author, temporary);
		}
	}
}
