package library.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import library.models.Author;
import library.models.Book;
import library.models.json.GutendexErrorResponse;
import library.models.json.GutendexResponse;
import library.utils.TimeUtil;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public record BookDownloadControl(
		@NotNull HttpClient client,
		@NotNull ObjectMapper objectMapper
) implements Closeable {
	@NotNull
	public static final String GUTENBERG_BOOK_API = "https://gutendex.com/books/";

	/**
	 * Creates a new BookDownloadControl instance with a pre-initialized HttpClient.
	 */
	public BookDownloadControl() {
		this(HttpClient.newHttpClient(), new ObjectMapper());
	}

	/**
	 * Searches for books from Project Gutenberg using the Gutendex API asynchronously.
	 *
	 * @param query The search query (title or author). Case-insensitive.
	 * @return A CompletableFuture containing a list of matching books.
	 */
	@NotNull
	public CompletableFuture<@NotNull List<GutendexResponse.@NotNull Book>> searchProjectGutenberg(@NotNull String query) {
		return CompletableFuture.supplyAsync(() -> {
					// Build the search URL with query parameters
					final var params = Map.of(
							"search", query
					);
					// Construct query string
					final var uri = URI.create("%s?%s".formatted(
							GUTENBERG_BOOK_API,
							params.entrySet().stream()
									.map(e -> "%s=%s".formatted(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8), URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)))
									.collect(Collectors.joining("&"))
					));
					return HttpRequest.newBuilder()
							.uri(uri)
							.GET()
							.header("Accept", "application/json")
							.build();
				})
				// Use the pre-initialized HttpClient to call the API asynchronously
				.thenCompose(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
				.thenApply(HttpResponse::body)
				.thenApply(body -> {
					try {
						return objectMapper.readValue(body, GutendexResponse.class);
					} catch (JsonProcessingException e) {
						try {
							throw new CompletionException(objectMapper.readValue(body, GutendexErrorResponse.class).detail(), e);
						} catch (JsonProcessingException e2) {
							final var e3 = new CompletionException(e2);
							e3.addSuppressed(e);
							throw e3;
						}
					}
				})
				.thenApply(GutendexResponse::results);
	}

	@NotNull
	public CompletableFuture<@NotNull String> downloadProjectGutenberg(@NotNull GutendexResponse.Book book) {
		return CompletableFuture.supplyAsync(() -> {
					// Get the URL for the text/plain format
					final var uri = URI.create(book.formats().entrySet().stream().filter(entry -> entry.getKey().startsWith("text/plain"))
							.findFirst().orElseThrow(() -> new NoSuchElementException("No plaintext format")).getValue());
					return HttpRequest.newBuilder()
							.uri(uri)
							.GET()
							.header("Accept", "text/plain")
							.build();
				})
				.thenCompose(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
				.thenApply(HttpResponse::body);
	}

	/**
	 * Combines a GutendexResponse.Book and its content to create a Book.
	 *
	 * @param book    The GutendexResponse.Book to convert.
	 * @param content The content of the book in string form.
	 * @return A Book with the combined data.
	 */
	@NotNull
	public Tuple2<Book, Book.Data> newBook(@NotNull GutendexResponse.Book book, @NotNull String content) {
		// Extract title and author from GutendexResponse.Book
		final var title = book.title();
		final var author = new Author.ByName(book.authorString());

		// Create a Book.Data object with content and approval status
		final var data = new Book.Data(
				book.summariesString(),
				content,
				library.models.Book.ApprovalStatus.APPROVED,
				TimeUtil.nowZoned(),
				null,
				0
		);

		// Create a new Book with title, author, and data
		return new Tuple2<>(new Book(title, author), data);
	}

	/**
	 * Closes this stream and releases any system resources associated
	 * with it. If the stream is already closed then invoking this
	 * method has no effect.
	 *
	 * <p> As noted in {@link AutoCloseable#close()}, cases where the
	 * close may fail require careful attention. It is strongly advised
	 * to relinquish the underlying resources and to internally
	 * <em>mark</em> the {@code Closeable} as closed, prior to throwing
	 * the {@code IOException}.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void close() throws IOException {
		client.close();
	}
}
