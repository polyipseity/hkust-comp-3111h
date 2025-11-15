package library.utils;

import library.models.Author;
import library.models.Book;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum Dates {
	;
	@NotNull
	public static final ZoneId UTC = ZoneId.of("UTC");

	@NotNull
	public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(UTC);
	}
}
