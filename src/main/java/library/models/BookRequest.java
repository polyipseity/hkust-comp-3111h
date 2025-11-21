package library.models;

import lombok.RequiredArgsConstructor;
import lombok.With;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

public record BookRequest(
		String title,
		String author
) implements Comparable<BookRequest> {
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
	public int compareTo(BookRequest o) {
		return Comparator
				.comparing(BookRequest::title)
				.thenComparing(BookRequest::author)
				.compare(this, o);
	}

	@With
	public record Data(
			ZonedDateTime requestDate
	) {
		@RequiredArgsConstructor
		public static final class S extends GroupSerializerObjectArray<Data> {
			/**
			 * Serializes the content of the given value into the given
			 * {@link DataOutput2}.
			 *
			 * @param out   DataOutput2 to save object into
			 * @param value Object to serialize
			 * @throws IOException in case of an I/O error
			 */
			@Override
			public void serialize(DataOutput2 out, BookRequest.Data value) throws IOException {
				// `requestDate`: second, nano, zone
				final var requestDate = value.requestDate();
				out.writeLong(requestDate.toEpochSecond());
				out.writeLong(requestDate.getNano());
				out.writeUTF(requestDate.getZone().getId());
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
				final var requestDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(input.readLong(), input.readLong()), ZoneId.of(input.readUTF()));
				return new Data(requestDate);
			}
		}
	}

	@RequiredArgsConstructor
	public static final class S extends GroupSerializerObjectArray<BookRequest> {
		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(DataOutput2 out, BookRequest value) throws IOException {
			out.writeUTF(value.title());
			out.writeUTF(value.author());
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
		public BookRequest deserialize(DataInput2 input, int available) throws IOException {
			final var title = input.readUTF();
			final var author = input.readUTF();
			return new BookRequest(title, author);
		}
	}
}
