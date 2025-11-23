package library.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the response from the OpenAI Chat Completion API.
 * This record encapsulates the details of the AI-generated chat responses, including the identifier,
 * object type, timestamp, model used, list of choices, and resource usage information.
 */
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAIChatCompletionResponse(
		@JsonProperty("id") String id,
		@JsonProperty("object") String object,
		@JsonProperty("created") long created,
		@JsonProperty("model") String model,
		@JsonProperty("choices") List<Choice> choices,
		@JsonProperty("usage") Usage usage
		// Many other properties...
) {
    /**
     * The type Choice.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
	public record Choice(
			@JsonProperty("index") int index,
			@JsonProperty("message") Message message,
			@JsonProperty("logprobs") @Nullable Object logprobs,
			@JsonProperty("finish_reason") String finishReason
			// Many other properties...
	) {
	}

    /**
     * The type Message.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
	public record Message(
			@JsonProperty("role") String role,
			@JsonProperty("content") String content
			// Many other properties...
	) {
	}

    /**
     * The type Usage.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
	public record Usage(
			@JsonProperty("prompt_tokens") int promptTokens,
			@JsonProperty("completion_tokens") int completionTokens,
			@JsonProperty("total_tokens") int totalTokens
			// Many other properties...
	) {
	}
}
