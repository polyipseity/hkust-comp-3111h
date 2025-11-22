package library.utils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public enum TimeUtil {
	;

	public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(ZoneOffset.UTC);
	}

	public static String toStringZonedLocal(ZonedDateTime dateTime) {
		final var localDateTime = dateTime.withZoneSameInstant(ZoneId.systemDefault());
		return "%04d-%02d-%02d %02d:%02d:%02d".formatted(localDateTime.getYear(),
				localDateTime.getMonthValue(),
				localDateTime.getDayOfMonth(),
				localDateTime.getHour(),
				localDateTime.getMinute(),
				localDateTime.getSecond());
	}

	public static String toStringDuration(Duration duration) {
		return "%dd %02d:%02d:%02d".formatted(duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
	}
}
