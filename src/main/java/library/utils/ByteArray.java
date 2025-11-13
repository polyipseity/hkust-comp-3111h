package library.utils;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class ByteArray {
	private final byte @NotNull [] data;
}
