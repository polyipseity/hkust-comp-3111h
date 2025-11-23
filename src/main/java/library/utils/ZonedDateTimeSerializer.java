package library.utils;

import lombok.RequiredArgsConstructor;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A serializer for {@link ZonedDateTime} that provides custom serialization and deserialization
 * methods for use with MapDB. This serializer is responsible for converting {@link ZonedDateTime}
 * objects to and from a binary format compatible with {@link DataOutput2} and {@link DataInput2}.
 * <p>
 * The serialized {@link ZonedDateTime} includes the epoch seconds, nanoseconds, and the time zone ID.
 */
@RequiredArgsConstructor
public class ZonedDateTimeSerializer extends GroupSerializerObjectArray<ZonedDateTime> {
    /**
     * The constant INSTANCE.
     */
    public static final ZonedDateTimeSerializer INSTANCE = new ZonedDateTimeSerializer();

	/**
	 * Serializes the content of the given value into the given
	 * {@link DataOutput2}.
	 *
	 * @param out   DataOutput2 to save object into
	 * @param value Object to serialize
	 * @throws IOException in case of an I/O error
	 */
	@Override
	public void serialize(DataOutput2 out, ZonedDateTime value) throws IOException {
		out.writeLong(value.toEpochSecond());
		out.writeLong(value.getNano());
		out.writeUTF(value.getZone().getId());
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
	public ZonedDateTime deserialize(DataInput2 input, int available) throws IOException {
		final var epochSecond = input.readLong();
		final var nanoAdjustment = input.readLong();
		final var zoneId = input.readUTF();
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nanoAdjustment), ZoneId.of(zoneId));
	}
}
