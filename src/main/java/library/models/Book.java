package library.models;

import library.utils.ByteArray;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    public record Borrow(
            @NotNull Date borrowDate,
            @NotNull Duration duration,
            @NotNull ByteArray pdfFile
    ) {
        public record S() implements Serializer<Borrow> {
            @Override
            public void serialize(@NotNull DataOutput2 out, @NotNull Book.Borrow value) throws IOException {
                // Date – use MapDB’s built‑in DATE serializer
                DATE.serialize(out, value.borrowDate());

                // Duration – store as long milliseconds
                out.writeLong(value.duration().toMillis());

                // pdfFile – length + raw bytes
                byte[] data = value.pdfFile().getData();
                out.writeInt(data.length);
                out.write(data);
            }

            @Override
            public Borrow deserialize(@NotNull DataInput2 input, int available) throws IOException {
                Date d = DATE.deserialize(input, available);
                long millis = input.readLong();
                Duration dur = Duration.ofMillis(millis);

                int len = input.readInt();
                byte[] file = new byte[len];
                input.readFully(file);

                return new Borrow(d, dur, new ByteArray(file));
            }
        }
    }

    @With
    public record Data(
            @NotNull String summary,
            @NotNull String content,
            @NotNull ApprovalStatus approvalStatus,
            @Nullable Book originalOrModified,
            @NotNull Map<User, Borrow> borrows,
            long timesBorrowed
    ) {
        @NotNull
        public Data withBorrow(@NotNull User user, @Nullable Borrow borrow) {
            var borrows = new HashMap<>(this.borrows);
            if (borrow == null) {
                borrows.remove(user);
            } else {
                borrows.put(user, borrow);
            }
            return withBorrows(borrows);
        }

        public record S(Serializer<User> userSerializer, Serializer<Book> bookSerializer,
                        Serializer<Borrow> borrowSerializer) implements Serializer<Data> {

            @Override
            public void serialize(@NotNull DataOutput2 out, @NotNull Book.Data value) throws IOException {
                // summary string
                out.writeUTF(value.summary());

                out.writeUTF(value.content());
                // approvalStatus – ordinal int
                out.writeInt(value.approvalStatus().ordinal());

                /* originalOrModified (optional) */
                if (value.originalOrModified() == null) {
                    out.writeBoolean(false);   // flag that it is absent
                } else {
                    out.writeBoolean(true);
                    bookSerializer.serialize(out, value.originalOrModified());
                }

                /* borrows map */
                Map<User, Borrow> m = value.borrows();
                out.writeInt(m.size());
                for (Map.Entry<User, Borrow> e : m.entrySet()) {
                    userSerializer.serialize(out, e.getKey());
                    borrowSerializer.serialize(out, e.getValue());
                }

                // timesBorrowed
                out.writeLong(value.timesBorrowed());
            }

            @Override
            public Data deserialize(@NotNull DataInput2 input, int available) throws IOException {
                String sum = input.readUTF();
                String con = input.readUTF();
                ApprovalStatus status = ApprovalStatus.values()[input.readInt()];

                /* originalOrModified */
                Book origMod;
                if (input.readBoolean()) {   // flag true → present
                    origMod = bookSerializer.deserialize(input, available);
                } else {
                    origMod = null;
                }

                /* borrows map */
                int size = input.readInt();
                Map<User, Borrow> borrows = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    User u = userSerializer.deserialize(input, available);
                    Borrow b = borrowSerializer.deserialize(input, available);
                    borrows.put(u, b);
                }

                long times = input.readLong();

                return new Data(sum, con, status, origMod, borrows, times);
            }
        }
    }

    public record S(Serializer<Author> authorSerializer) implements Serializer<Book> {
        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull Book value) throws IOException {
            // title string + author + modified
            out.writeUTF(value.title());
            authorSerializer.serialize(out, value.author());
            out.writeBoolean(value.modified());
        }

        @Override
        public Book deserialize(@NotNull DataInput2 input, int available) throws IOException {
            String t = input.readUTF();
            Author a = authorSerializer.deserialize(input, available);
            boolean m = input.readBoolean();
            return new Book(t, a, m);
        }
    }
}
