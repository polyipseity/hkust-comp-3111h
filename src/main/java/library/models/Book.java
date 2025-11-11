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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record Book(
		@NotNull String title,
		@NotNull Author author
) {
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
			@NotNull ApprovalStatus approvalStatus,
			@Nullable Book originalOrModified,
			@NotNull Map<User, Borrow> borrows,
			long timesBorrowed
	) {
		public record S(Serializer<User> userSerializer, Serializer<Book> bookSerializer,
		                Serializer<Borrow> borrowSerializer) implements Serializer<Data> {

			@Override
			public void serialize(@NotNull DataOutput2 out, @NotNull Book.Data value) throws IOException {
				// summary string
				out.writeUTF(value.summary());

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

				return new Data(sum, status, origMod, borrows, times);
			}
		}
	}

	public record S(Serializer<Author> authorSerializer) implements Serializer<Book> {
		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull Book value) throws IOException {
			// title string + author
			out.writeUTF(value.title());
			authorSerializer.serialize(out, value.author());
		}

		@Override
		public Book deserialize(@NotNull DataInput2 input, int available) throws IOException {
			String t = input.readUTF();
			Author a = authorSerializer.deserialize(input, available);
			return new Book(t, a);
		}
	}
}
