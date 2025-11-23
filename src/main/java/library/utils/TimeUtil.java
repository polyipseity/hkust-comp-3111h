package library.utils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Utility class providing time-related functions such as fetching the
 * current UTC time, formatting `ZonedDateTime` objects, and converting
 * durations to string representations.
 */
public enum TimeUtil {
	;

    /**
     * Retrieves the current date and time in the UTC time zone.
     *
     * @return the current date and time as a {@link ZonedDateTime} in the UTC time zone
     */
    public static ZonedDateTime nowZoned() {
		return ZonedDateTime.now(ZoneOffset.UTC);
	}

    /**
     * Converts the given {@link ZonedDateTime} to a local date-time string formatted as
     * "yyyy-MM-dd HH:mm:ss", using the system's default time zone.
     *
     * @param dateTime the {@link ZonedDateTime} to be converted
     * @return a string representation of the date-time in the format "yyyy-MM-dd HH:mm:ss"
     */
    public static String toStringZonedLocal(ZonedDateTime dateTime) {
		final var localDateTime = dateTime.withZoneSameInstant(ZoneId.systemDefault());
		return "%04d-%02d-%02d %02d:%02d:%02d".formatted(localDateTime.getYear(),
				localDateTime.getMonthValue(),
				localDateTime.getDayOfMonth(),
				localDateTime.getHour(),
				localDateTime.getMinute(),
				localDateTime.getSecond());
	}

    /**
     * Converts the given {@link Duration} into a human-readable string representation
     * in the format "dd HH:mm:ss", where `dd` represents days, `HH` represents hours,
     * `mm` represents minutes, and `ss` represents seconds.
     *
     * @param duration the {@link Duration} to be formatted
     * @return a formatted string representing the duration in the format "dd HH:mm:ss"
     */
    public static String toStringDuration(Duration duration) {
		return "%dd %02d:%02d:%02d".formatted(duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
	}
}
