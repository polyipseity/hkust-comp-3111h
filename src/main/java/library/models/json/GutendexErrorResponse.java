package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public record GutendexErrorResponse(
		@JsonProperty("detail") @NotNull String detail
) {
}
