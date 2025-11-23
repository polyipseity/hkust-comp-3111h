package library.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the body of a request sent to the OpenAI Chat Completion API.
 * This record encapsulates information about the model, a list of messages for the prompt,
 * temperature for randomness, a limit on maximum tokens, and whether to stream the result.
 */
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
    /**
     * The type Message.
     */
    public record Message(
			@JsonProperty("role") String role,
			@JsonProperty("content") String content
	) {
	}
}
