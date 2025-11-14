package library.models;

import library.utils.ByteArray;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;

@With
public record Borrow(
		@NotNull Date borrowDate,
		@NotNull Duration duration,
		@NotNull ByteArray pdfContent
) {
	public Borrow(@NotNull Date borrowDate, @NotNull Duration duration, @NotNull ByteArray pdfContent) {
		this.borrowDate = borrowDate;
		this.duration = Duration.ofMillis(duration.toMillis());
		this.pdfContent = pdfContent;
	}

	@RequiredArgsConstructor
	public static class S extends GroupSerializerObjectArray<Borrow> {
		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull Borrow value) throws IOException {
			DATE.serialize(out, value.borrowDate());
			out.writeLong(value.duration().toMillis());

			// `pdfContent`: length, raw bytes
			final var data = value.pdfContent().getData();
			out.writeInt(data.length);
			out.write(data);
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
		public Borrow deserialize(@NotNull DataInput2 input, int available) throws IOException {
			final var d = DATE.deserialize(input, available);
			final var dur = Duration.ofMillis(input.readLong());

			final var file = new byte[input.readInt()];
			input.readFully(file);
			return new Borrow(d, dur, new ByteArray(file));
		}
	}
}
