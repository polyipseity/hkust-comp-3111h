package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GutendexErrorResponse(
		@JsonProperty("detail") String detail
) {
}
