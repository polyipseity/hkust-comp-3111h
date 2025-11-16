package library.utils;

import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public enum Dates {
	;
	@NotNull
	public static final ZoneId UTC = ZoneId.of("UTC");

	@NotNull
	public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(UTC);
	}

	@NotNull
	public static String zonedLocalToString(@NotNull ZonedDateTime dateTime) {
		return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(dateTime.withZoneSameInstant(ZoneId.systemDefault()));
	}
}
