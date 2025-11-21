package library.models;

import lombok.RequiredArgsConstructor;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;

public sealed interface Author extends Comparable<Author> permits Author.ByName, Author.ByRef {
	byte getTag();

	String id();

	record ByRef(User value) implements Author {
		public static final byte TAG = 0;

		@Override
		public byte getTag() {
			return TAG;
		}

		@Override
		public String id() {
			return value.username();
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
		public int compareTo(Author o) {
			return switch (o) {
				case ByRef(final var val) -> value.compareTo(val);
				case Author author -> Byte.compare(getTag(), author.getTag());
			};
		}
	}

	record ByName(String value) implements Author {
		public static final byte TAG = 1;

		@Override
		public byte getTag() {
			return TAG;
		}

		@Override
		public String id() {
			return value;
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
		public int compareTo(Author o) {
			return switch (o) {
				case ByName(final var val) -> value.compareTo(val);
				case Author author -> Byte.compare(getTag(), author.getTag());
			};
		}
	}

	@RequiredArgsConstructor
	class S extends GroupSerializerObjectArray<Author> {
		protected final Serializer<User> userSerializer;

		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(DataOutput2 out, Author value) throws IOException {
			switch (value) {
				case ByRef(final var val) -> {
					out.writeByte(ByRef.TAG);
					userSerializer.serialize(out, val);
				}
				case ByName(final var val) -> {
					out.writeByte(ByName.TAG);
					out.writeUTF(val);
				}
			}
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
		public Author deserialize(DataInput2 input, int available) throws IOException {
			final var tag = input.readByte();
			final var ret = switch (tag) {
				case ByRef.TAG -> new ByRef(userSerializer.deserialize(input, available));
				case ByName.TAG -> new ByName(input.readUTF());
				default -> throw new IOException("Unknown `Author` tag: %s".formatted(tag));
			};
			assert tag == ret.getTag();
			return ret;
		}
	}
}
