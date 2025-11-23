package library.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializer;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;

/**
 * A final, immutable class representing a wrapper around a byte array.
 * This class includes a custom serializer for use in data serialization and
 * deserialization processes.
 */
@Data
public final class ByteArray {
    /**
     * The constant SERIALIZER.
     */
    public static GroupSerializer<ByteArray> SERIALIZER = new S(GroupSerializerObjectArray.BYTE_ARRAY);
	private final byte[] data;

    /**
     * The type S.
     */
    @RequiredArgsConstructor
	public static final class S extends GroupSerializerObjectArray<ByteArray> {
		private final GroupSerializer<byte[]> byteArraySerializer;

		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(DataOutput2 out, ByteArray value) throws IOException {
			byteArraySerializer.serialize(out, value.data);
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
		public ByteArray deserialize(DataInput2 input, int available) throws IOException {
			return new ByteArray(byteArraySerializer.deserialize(input, available));
		}
	}
}
