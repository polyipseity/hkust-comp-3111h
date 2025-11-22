package library.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAIChatCompletionBody(
		@JsonProperty("model") String model,
		@JsonProperty("messages") List<Message> messages,
		@JsonProperty("temperature") @Nullable Double temperature,
		@JsonProperty("max_tokens") @Nullable Integer maxTokens,
		@JsonProperty("stream") @Nullable Boolean stream
		// Many other properties...
) {
	public record Message(
			@JsonProperty("role") String role,
			@JsonProperty("content") String content
	) {
	}
}
