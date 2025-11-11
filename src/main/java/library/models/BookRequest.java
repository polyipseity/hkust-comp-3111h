package library.models;

import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.Date;

public record BookRequest(
		@NotNull String title,
		@NotNull String author
) {
	@With
	public record Data(
			@NotNull Date requestDate
	) {
		public record S() implements Serializer<Data> {
			@Override
			public void serialize(@NotNull DataOutput2 out, @NotNull BookRequest.Data value) throws IOException {
				DATE.serialize(out, value.requestDate());
			}

			@Override
			public Data deserialize(@NotNull DataInput2 input, int available) throws IOException {
				Date d = DATE.deserialize(input, available);
				return new Data(d);
			}
		}
	}

	public record S() implements Serializer<BookRequest> {
		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull BookRequest value) throws IOException {
			out.writeUTF(value.title());
			out.writeUTF(value.author());
		}

		@Override
		public BookRequest deserialize(@NotNull DataInput2 input, int available) throws IOException {
			String title = input.readUTF();
			String author = input.readUTF();
			return new BookRequest(title, author);
		}
	}
}
