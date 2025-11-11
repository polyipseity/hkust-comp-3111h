package library.models;

import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record User(
		@NotNull String username
) {
	public enum Role {
		STUDENT_STAFF,
		LIBRARIAN,
		AUTHOR
	}

	@With
	public record Data(
			@NotNull String password,
			boolean active,
			@NotNull Role role,
			@NotNull String fullName,
			@NotNull List<String> notifications,
			@NotNull Map<BookRequest, BookRequest.Data> bookRequests
	) {
		public record S(Serializer<BookRequest> bookRequestSerializer,
		                Serializer<BookRequest.Data> bookRequestDataSerializer) implements Serializer<Data> {

			@Override
			public void serialize(@NotNull DataOutput2 out, @NotNull Data value) throws IOException {
				// String fields
				out.writeUTF(value.password());
				out.writeBoolean(value.active());
				// enum – store its ordinal
				out.writeInt(value.role().ordinal());
				out.writeUTF(value.fullName());

				/* notifications : List<String> */
				List<String> n = value.notifications();
				out.writeInt(n.size());                       // length prefix
				for (String s : n) {
					out.writeUTF(s);
				}

				/* bookRequests : Map<BookRequest, BookRequest.Data> */
				Map<BookRequest, BookRequest.Data> m = value.bookRequests();
				out.writeInt(m.size());
				for (Map.Entry<BookRequest, BookRequest.Data> e : m.entrySet()) {
					bookRequestSerializer.serialize(out, e.getKey());
					bookRequestDataSerializer.serialize(out, e.getValue());
				}
			}

			@Override
			public Data deserialize(@NotNull DataInput2 input, int available) throws IOException {
				String pwd = input.readUTF();
				boolean act = input.readBoolean();
				Role r = Role.values()[input.readInt()];
				String fn = input.readUTF();

				/* notifications */
				int nSize = input.readInt();
				List<String> notifs = new ArrayList<>(nSize);
				for (int i = 0; i < nSize; i++) {
					notifs.add(input.readUTF());
				}

				/* bookRequests */
				int mSize = input.readInt();
				Map<BookRequest, BookRequest.Data> reqs = new HashMap<>(mSize);
				for (int i = 0; i < mSize; i++) {
					BookRequest key = bookRequestSerializer.deserialize(input, available);
					BookRequest.Data val = bookRequestDataSerializer.deserialize(input, available);
					reqs.put(key, val);
				}

				return new Data(pwd, act, r, fn, notifs, reqs);
			}
		}
	}

	public record S() implements Serializer<User> {
		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull User value) throws IOException {
			out.writeUTF(value.username());
		}

		@Override
		public User deserialize(@NotNull DataInput2 input, int available) throws IOException {
			String uname = input.readUTF();
			return new User(uname);
		}
	}
}
