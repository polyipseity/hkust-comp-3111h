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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@With
public record Borrow(
		@NotNull ZonedDateTime borrowDate,
		@NotNull Duration duration,
		@NotNull ByteArray pdfContent
) {
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
			// `borrowDate`: second, nano, zone
			final var borrowDate = value.borrowDate();
			out.writeLong(borrowDate.toEpochSecond());
			out.writeLong(borrowDate.getNano());
			out.writeUTF(borrowDate.getZone().getId());

			out.writeLong(value.duration().toNanos());

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
			final var borrowDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(input.readLong(), input.readLong()), ZoneId.of(input.readUTF()));
			final var duration = Duration.ofNanos(input.readLong());

			final var file = new byte[input.readInt()];
			input.readFully(file);
			return new Borrow(borrowDate, duration, new ByteArray(file));
		}
	}
}
