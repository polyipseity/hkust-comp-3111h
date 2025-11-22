package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

public record OpenAIErrorResponse(
		@JsonProperty("statusCode") int statusCode,
		@JsonProperty("message") @Nullable String message,
		@JsonProperty("activityId") @Nullable String activityId
) {
}
