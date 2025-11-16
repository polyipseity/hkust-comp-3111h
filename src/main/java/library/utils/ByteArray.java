package library.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializer;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;

@Data
public final class ByteArray {
	@NotNull
	public static GroupSerializer<ByteArray> SERIALIZER = new S(GroupSerializerObjectArray.BYTE_ARRAY);
	private final byte @NotNull [] data;

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
		public void serialize(@NotNull DataOutput2 out, @NotNull ByteArray value) throws IOException {
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
		public ByteArray deserialize(@NotNull DataInput2 input, int available) throws IOException {
			return new ByteArray(byteArraySerializer.deserialize(input, available));
		}
	}
}
