package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the error response from the Gutendex API.
 * This record encapsulates the details of an error response,
 * specifically a message explaining the reason for the error.
 */
public record GutendexErrorResponse(
		@JsonProperty("detail") String detail
) {
}
