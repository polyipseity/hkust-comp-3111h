package library.utils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public enum TimeUtil {
	;

	public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(ZoneOffset.UTC);
	}

	public static String toStringZonedLocal(ZonedDateTime dateTime) {
		return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(dateTime.withZoneSameInstant(ZoneId.systemDefault()));
	}

	public static String toStringDuration(Duration duration) {
		return "%dd %02d:%02d:%02d".formatted(duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
	}
}
