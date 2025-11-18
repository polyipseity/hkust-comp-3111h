package library.utils;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public enum TimeUtil {
	;
	@NotNull
	public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(ZoneOffset.UTC);
	}

	@NotNull
	public static String toStringZonedLocal(@NotNull ZonedDateTime dateTime) {
		return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(dateTime.withZoneSameInstant(ZoneId.systemDefault()));
	}

	@NotNull
	public static String toStringDuration(@NotNull Duration duration) {
		return "%dd %d:%02d:%02d".formatted(duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
	}
}
