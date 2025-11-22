package library.models;

import library.utils.DurationSerializer;
import library.utils.TimeUtil;
import library.utils.ZonedDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;

@With
public record Borrow(
		ZonedDateTime borrowDate,
		Duration duration,
		String pdfPath
) {
	public ZonedDateTime due() {
		return borrowDate.plus(duration);
	}

	/**
	 * How much time is left until the book is due, relative to {@code reference}.
	 *
	 * @param reference the point in time from which we want to measure the remaining period
	 * @return a {@link Duration} that is zero if the due date has already passed,
	 * otherwise the duration between {@code reference} and the due date
	 */
	public Duration durationLeft(ZonedDateTime reference) {
		final var due = due();
		return reference.isAfter(due)
				? Duration.ZERO
				: Duration.between(reference, due);
	}

	/**
	 * Convenience overload that uses the current instant in UTC.
	 *
	 * @return the remaining duration until the book is due (or zero if overdue)
	 */
	public Duration durationLeft() {
		return durationLeft(TimeUtil.nowZoned());
	}

	public boolean expired() {
		return durationLeft().isZero();
	}

	@RequiredArgsConstructor
	public static final class S extends GroupSerializerObjectArray<Borrow> {
		/**
		 * Serializes the content of the given value into the given
		 * {@link DataOutput2}.
		 *
		 * @param out   DataOutput2 to save object into
		 * @param value Object to serialize
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public void serialize(DataOutput2 out, Borrow value) throws IOException {
			ZonedDateTimeSerializer.INSTANCE.serialize(out, value.borrowDate());
			DurationSerializer.INSTANCE.serialize(out, value.duration());
			out.writeUTF(value.pdfPath);
		}

		/**
		 * Deserializes and returns the content of the given {@link DataInput2}.
		 *
		 * @param input     DataInput2 to de-serialize data from
		 * @param available how many bytes that are available in the DataInput2 for
		 *                  reading, may be -1 (in streams) or 0 (null).
		 * @return the de-serialized content of the given {@link DataInput2}
		 * @throws IOException in case of an I/O error
		 */
		@Override
		public Borrow deserialize(DataInput2 input, int available) throws IOException {
			final var borrowDate = ZonedDateTimeSerializer.INSTANCE.deserialize(input, available);
			final var duration = DurationSerializer.INSTANCE.deserialize(input, available);
			final var pdfPath = input.readUTF();
			return new Borrow(borrowDate, duration, pdfPath);
		}
	}
}
