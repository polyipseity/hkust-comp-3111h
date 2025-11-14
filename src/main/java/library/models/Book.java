package library.models;

import lombok.RequiredArgsConstructor;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.util.Comparator;

public record Book(
		@NotNull String title,
		@NotNull Author author,
		boolean modified
) implements Comparable<Book> {
	public Book(@NotNull String title, @NotNull Author author) {
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
	public int compareTo(@NotNull Book o) {
		return Comparator
				.comparing(Book::title)
				.thenComparing(Book::author)
				.thenComparingInt(book -> book.modified ? 1 : 0)
				.compare(this, o);
	}

	public enum ApprovalStatus {
		PENDING,
		APPROVED,
		REJECTED
	}

	@With
	public record Data(
			@NotNull String summary,
			@NotNull String content,
			@NotNull ApprovalStatus approvalStatus,
			@Nullable Book originalOrModified,
			long timesBorrowed
	) {
		@RequiredArgsConstructor
		public static class S extends GroupSerializerObjectArray<Data> {
			protected final Serializer<Book> bookSerializer;

			/**
			 * Serializes the content of the given value into the given
			 * {@link DataOutput2}.
			 *
			 * @param out   DataOutput2 to save object into
			 * @param value Object to serialize
			 * @throws IOException in case of an I/O error
			 */
			@Override
			public void serialize(@NotNull DataOutput2 out, @NotNull Book.Data value) throws IOException {
				out.writeUTF(value.summary());
				out.writeUTF(value.content());
				out.writeInt(value.approvalStatus().ordinal());
				if (value.originalOrModified() == null) {
					out.writeBoolean(false);   // flag that it is absent
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
			@NotNull
			public Data deserialize(@NotNull DataInput2 input, int available) throws IOException {
				final var summary = input.readUTF();
				final var content = input.readUTF();
				final var status = ApprovalStatus.values()[input.readInt()];
				@SuppressWarnings("SwitchStatementWithTooFewBranches") final var originalOrModified = switch (input.readBoolean() ? 1 : 0) {
					case 1 -> bookSerializer.deserialize(input, available);
					default -> null;
				};
				final var times = input.readLong();
				return new Data(summary, content, status, originalOrModified, times);
			}
		}
	}

	@RequiredArgsConstructor
	public static class S extends GroupSerializerObjectArray<Book> {
		protected final Serializer<Author> authorSerializer;

		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull Book value) throws IOException {
			out.writeUTF(value.title());
			authorSerializer.serialize(out, value.author());
			out.writeBoolean(value.modified());
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
		@NotNull
		public Book deserialize(@NotNull DataInput2 input, int available) throws IOException {
			final var title = input.readUTF();
			final var author = authorSerializer.deserialize(input, available);
			final var modified = input.readBoolean();
			return new Book(title, author, modified);
		}
	}
}
