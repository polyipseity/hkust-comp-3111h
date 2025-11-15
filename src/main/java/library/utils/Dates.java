package library.utils;

import org.jetbrains.annotations.NotNull;

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
