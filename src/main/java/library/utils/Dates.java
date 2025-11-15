package library.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum Dates {
	;
	public static final ZoneId UTC = ZoneId.of("UTC");

	public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(UTC);
	}
}
