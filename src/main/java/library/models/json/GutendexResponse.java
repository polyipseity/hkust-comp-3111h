package library.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GutendexResponse(
		@JsonProperty("count") long count,
		@JsonProperty("next") @Nullable String next,
		@JsonProperty("previous") @Nullable String previous,
		@JsonProperty("results") @NotNull List<@NotNull Book> results
) {
	public record Book(
			@JsonProperty("id") long id,
			@JsonProperty("title") @NotNull String title,
			@JsonProperty("subjects") @NotNull List<@NotNull String> subjects,
			@JsonProperty("authors") @NotNull List<@NotNull Person> authors,
			@JsonProperty("summaries") @NotNull List<@NotNull String> summaries,
			@JsonProperty("editors") @NotNull List<@NotNull Person> editors,
			@JsonProperty("translators") @NotNull List<@NotNull Person> translators,
			@JsonProperty("bookshelves") @NotNull List<@NotNull String> bookshelves,
			@JsonProperty("languages") @NotNull List<@NotNull String> languages,
			@JsonProperty("copyright") @Nullable Boolean copyright,
			@JsonProperty("media_type") @NotNull String mediaType,
			@JsonProperty("formats") @NotNull Map<@NotNull String, @NotNull String> formats,
			// <string of MIME-type>: <string of URL>
			@JsonProperty("download_count") long downloadCount
	) {
		public @NotNull String authorString() {
			return authors.stream().map(GutendexResponse.Person::name).collect(Collectors.joining("; "));
		}

		public @NotNull String bookshelvesString() {
			return String.join("; ", bookshelves);
		}

		public @NotNull String summariesString() {
			return String.join("\n\n", summaries);
		}
	}

	public record Person(
			@JsonProperty("birth_year") @Nullable Integer birthYear,
			@JsonProperty("death_year") @Nullable Integer deathYear,
			@JsonProperty("name") @NotNull String name
	) {
	}
}
