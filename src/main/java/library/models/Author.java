package library.models;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public sealed interface Author {
	record ByRef(@NotNull User value) implements Author {
	}

	record ByName(@NotNull String value) implements Author {
	}

	record S(Serializer<User> userSerializer) implements Serializer<Author> {
		private static final byte TAG_BY_REF = 0;
		private static final byte TAG_BY_NAME = 1;

		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull Author value) throws IOException {
			switch (value) {
				case ByRef(var val) -> {
					out.writeByte(TAG_BY_REF);
					userSerializer.serialize(out, val);
				}
				case ByName(var val) -> {
					out.writeByte(TAG_BY_NAME);
					out.writeUTF(val);
				}
			}
		}

		@Override
		public Author deserialize(@NotNull DataInput2 input, int available) throws IOException {
			byte tag = input.readByte();
			return switch (tag) {
				case TAG_BY_REF -> new ByRef(userSerializer.deserialize(input, available));
				case TAG_BY_NAME -> {
					String s = input.readUTF();
					yield new ByName(s);
				}
				default -> throw new IOException("Unknown Author tag: " + tag);
			};
		}
	}
}
