package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the error response from an OpenAI API call.
 * This record encapsulates information about the HTTP status code,
 * an optional error message, and an optional activity identifier for troubleshooting.
 */
public record OpenAIErrorResponse(
		@JsonProperty("statusCode") int statusCode,
		@JsonProperty("message") @Nullable String message,
		@JsonProperty("activityId") @Nullable String activityId
) {
}
