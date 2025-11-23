package library.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.models.json.OpenAIChatCompletionBody;
import library.models.json.OpenAIChatCompletionResponse;
import library.models.json.OpenAIErrorResponse;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The type Ai service control.
 */
public record AIServiceControl(
        /**
         *
         *//**
 * The HTTP client used for making asynchronous HTTP requests.
 * <p>
 * This variable is part of the {@link AIServiceControl} class and is used
 * to send HTTP requests, specifically to interact with external APIs such as OpenAI.
 * It is initialized as an instance of {@link HttpClient} with configurations
 * for redirect handling.
 */HttpClient client,
        ObjectMapper objectMapper
) implements Closeable {
    /**
     * The constant OPENAI_API_URL.
     */
    public static final String OPENAI_API_URL = "https://hkust.azure-api.net/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-10-21";
    /**
     * The constant OPENAI_API_KEY.
     */
    public static final String OPENAI_API_KEY = "d7d5f6c01f3c493292402ec61cafe248";

    /**
     * Instantiates a new Ai service control.
     */
    public AIServiceControl() {
		this(
				HttpClient.newBuilder()
						.followRedirects(HttpClient.Redirect.ALWAYS)
						.build(),
				new ObjectMapper()
		);
	}

    /**
     * Chat complete completable future.
     *
     * @param prompt the prompt
     * @return the completable future
     */
    public CompletableFuture<OpenAIChatCompletionResponse> chatComplete(OpenAIChatCompletionBody prompt) {
		return CompletableFuture.supplyAsync(() -> {
					try {
						System.out.println(objectMapper.writeValueAsString(prompt));
						return HttpRequest.newBuilder()
								.uri(URI.create(OPENAI_API_URL))
								.header("Content-Type", "application/json")
								.header("api-key", OPENAI_API_KEY)
								.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(prompt)))
								.build();
					} catch (JsonProcessingException e) {
						throw new CompletionException(e);
					}
				})
				// Use the pre-initialized HttpClient to call the API asynchronously
				.thenCompose(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
				.thenApply(HttpResponse::body)
				.thenApply(body -> {
					System.out.println(body);
					try {
						return objectMapper.readValue(body, OpenAIChatCompletionResponse.class);
					} catch (JsonProcessingException e) {
						try {
							throw new CompletionException(objectMapper.readValue(body, OpenAIErrorResponse.class).message(), e);
						} catch (JsonProcessingException e2) {
							e2.addSuppressed(e);
							throw new CompletionException(e2);
						}
					}
				});
	}

	@Override
	public void close() {
		client.close();
	}
}
