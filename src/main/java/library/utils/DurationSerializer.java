package library.utils;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
public class DurationSerializer extends GroupSerializerObjectArray<Duration> {
	@NotNull
	public static final DurationSerializer INSTANCE = new DurationSerializer();

	/**
	 * Serializes the content of the given value into the given
	 * {@link DataOutput2}.
	 *
	 * @param out   DataOutput2 to save object into
	 * @param value Object to serialize
	 * @throws IOException in case of an I/O error
	 */
	@Override
	public void serialize(@NotNull DataOutput2 out, @NotNull Duration value) throws IOException {
		out.writeLong(value.getSeconds());
		out.writeInt(value.getNano());
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
	public Duration deserialize(@NotNull DataInput2 input, int available) throws IOException {
		final var seconds = input.readLong();
		final var nanoAdjustment = input.readInt();
		return Duration.ofSeconds(seconds, nanoAdjustment);
	}
}
