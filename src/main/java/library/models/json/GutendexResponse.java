package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public record GutendexResponse(
		@JsonProperty("count") long count,
		@JsonProperty("next") @Nullable String next,
		@JsonProperty("previous") @Nullable String previous,
		@JsonProperty("results") List<Book> results
) {
	public record Book(
			@JsonProperty("id") long id,
			@JsonProperty("title") String title,
			@JsonProperty("subjects") List<String> subjects,
			@JsonProperty("authors") List<Person> authors,
			@JsonProperty("summaries") List<String> summaries,
			@JsonProperty("editors") List<Person> editors,
			@JsonProperty("translators") List<Person> translators,
			@JsonProperty("bookshelves") List<String> bookshelves,
			@JsonProperty("languages") List<String> languages,
			@JsonProperty("copyright") @Nullable Boolean copyright,
			@JsonProperty("media_type") String mediaType,
			@JsonProperty("formats") Map<String, String> formats,
			// <string of MIME-type>: <string of URL>
			@JsonProperty("download_count") long downloadCount
	) {
		public String authorString() {
			final var ret = authors.stream().map(Person::name).collect(Collectors.joining("; "));
			return ret.isBlank() ? "(no authors)" : ret;
		}

		public String bookshelvesString() {
			final var ret = String.join("; ", bookshelves);
			return ret.isBlank() ? "(no bookshelves)" : ret;
		}

		public String summariesString() {
			final var ret = String.join("\n\n", summaries);
			return ret.isBlank() ? "(no summary)" : ret;
		}
	}

	public record Person(
			@JsonProperty("birth_year") @Nullable Integer birthYear,
			@JsonProperty("death_year") @Nullable Integer deathYear,
			@JsonProperty("name") String name
	) {
	}
}
