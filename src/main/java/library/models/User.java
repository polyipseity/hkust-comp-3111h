package library.models;

import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public record User(
		@NotNull String username
) implements Comparable<User> {
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
	public int compareTo(@NotNull User o) {
		return username.compareTo(o.username);
	}

	public enum Role {
		STUDENT_STAFF("Student/Staff"),
		LIBRARIAN("Librarian"),
		AUTHOR("Author"),
		;

		public final String name;

		Role(String name) {
			this.name = name;
		}
	}

	@With
	public record Data(
			@NotNull Role role,
			boolean active,
			@NotNull String password,
			@NotNull String fullName
	) {
		public record S(Serializer<BookRequest> bookRequestSerializer,
		                Serializer<BookRequest.Data> bookRequestDataSerializer) implements Serializer<Data> {

			@Override
			public void serialize(@NotNull DataOutput2 out, @NotNull Data value) throws IOException {
				out.writeInt(value.role().ordinal());
				out.writeBoolean(value.active());
				out.writeUTF(value.password());
				out.writeUTF(value.fullName());
			}

			@Override
			public Data deserialize(@NotNull DataInput2 input, int available) throws IOException {
				final var r = Role.values()[input.readInt()];
				final var act = input.readBoolean();
				final var pwd = input.readUTF();
				final var fn = input.readUTF();
				return new Data(r, act, pwd, fn);
			}
		}
	}

	public record S() implements Serializer<User> {
		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull User value) throws IOException {
			out.writeUTF(value.username());
		}

		@Override
		public User deserialize(@NotNull DataInput2 input, int available) throws IOException {
			final var username = input.readUTF();
			return new User(username);
		}
	}
}
