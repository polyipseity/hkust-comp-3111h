package library.models.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GutendexResponseTest {
	private GutendexResponse response;

	@BeforeEach
	void setUp() {
		// Sample book data for testing
		List<GutendexResponse.Person> authors = Arrays.asList(
				new GutendexResponse.Person(1900, null, "Author A"),
				new GutendexResponse.Person(1850, null, "Author B")
		);

		List<String> subjects = Arrays.asList("Fiction", "Adventure");
		List<String> bookshelves = Arrays.asList("Fiction", "Classic");
		List<String> summaries = Arrays.asList("This is a great story.", "It's full of adventure.");

		GutendexResponse.Book book = new GutendexResponse.Book(
				1L,
				"The Adventure of the Blue Moon",
				subjects,
				authors,
				summaries,
				Collections.emptyList(),
				Collections.emptyList(),
				bookshelves,
				List.of("en"),
				null,
				"eBook",
				Collections.singletonMap("application/pdf", "https://example.com/pdf"),
				1234L
		);

		response = new GutendexResponse(10L, null, null, List.of(book, new GutendexResponse.Book(
				0L,
				"",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				null,
				"",
				Collections.emptyMap(),
				0L
		)));
	}

	@Test
	void testAuthorString() {
		GutendexResponse.Book book = response.results().getFirst();
		assertEquals("Author A; Author B", book.authorString());
	}

	@Test
	void testBookshelvesString() {
		GutendexResponse.Book book = response.results().getFirst();
		assertEquals("Fiction; Classic", book.bookshelvesString());
	}

	@Test
	void testSummariesString() {
		GutendexResponse.Book book = response.results().getFirst();
		assertEquals("This is a great story.\n\nIt's full of adventure.", book.summariesString());
	}

	@Test
	void testAuthorString_empty() {
		GutendexResponse.Book book = response.results().getLast();
		assertEquals("(no authors)", book.authorString());
	}

	@Test
	void testBookshelvesString_empty() {
		GutendexResponse.Book book = response.results().getLast();
		assertEquals("(no bookshelves)", book.bookshelvesString());
	}

	@Test
	void testSummariesString_empty() {
		GutendexResponse.Book book = response.results().getLast();
		assertEquals("(no summary)", book.summariesString());
	}
}
