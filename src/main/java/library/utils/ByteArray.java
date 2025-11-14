package library.utils;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public final class ByteArray {
	private final byte @NotNull [] data;
}
