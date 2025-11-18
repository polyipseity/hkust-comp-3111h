package library.models;

import library.utils.ByteArray;
import library.utils.DurationSerializer;
import library.utils.ZonedDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;

@With
public record Borrow(
		@NotNull ZonedDateTime borrowDate,
		@NotNull Duration duration,
		@NotNull String pdfPath
) {
	@RequiredArgsConstructor
	public static final class S extends GroupSerializerObjectArray<Borrow> {
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
			ZonedDateTimeSerializer.INSTANCE.serialize(out, value.borrowDate());
			DurationSerializer.INSTANCE.serialize(out, value.duration());
			out.writeUTF(value.pdfPath);
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
			final var borrowDate = ZonedDateTimeSerializer.INSTANCE.deserialize(input, available);
			final var duration = DurationSerializer.INSTANCE.deserialize(input, available);
			final var pdfPath = input.readUTF();
			return new Borrow(borrowDate, duration, pdfPath);
		}
	}
}
