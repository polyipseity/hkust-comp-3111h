package library.controllers.common;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.FakeContext;
import library.Main;
import library.PlatformTestUtil;
import library.controllers.common.DynamicTableController.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;

class DynamicTableControllerTest {
	private static boolean unsupported = false;
	private TableView<Function<String, DynamicTableController.Data>> table;
	private DynamicTableController<@NotNull String, @NotNull Function<String, DynamicTableController.Data>> tableController;
	private SequencedMap<String, TableColumn<Function<String, Data>, Function<String, Data>>> keys;
	private Collection<Function<String, Data>> data;

	@BeforeAll
	static void setUpAll() {
		if (!PlatformTestUtil.startup()) {
			abort();
		}
		final var context = new FakeContext(false);
		Main.setContext(context);
	}

	@AfterAll
	static void tearDownAll() {
		if (unsupported) {
			return;
		}
		Main.getContext().close();
		Main.setContext(null);
	}

	@Test
	public void testSetKeysRemovesObsoleteColumnsAndAddsNewColumns() {
		table = new TableView<>();

		final var col1 = new TableColumn<Function<String, Data>, Function<String, Data>>();
		final var col2 = new TableColumn<Function<String, Data>, Function<String, Data>>();
		final var col3 = new TableColumn<Function<String, Data>, Function<String, Data>>();

		// Arrange
		keys = new LinkedHashMap<>();
		keys.put("key1", col1);
		keys.put("key2", col2);

		tableController = new DynamicTableController<>(table, new LinkedHashMap<>(Map.of(
				"key1", col1,
				"key3", col3
		)));
		data = List.of();

		// Act
		tableController.setKeys(keys);

		// Assert
		assertEquals(2, table.getColumns().size());
		assertTrue(table.getColumns().contains(col1));
		assertTrue(table.getColumns().contains(col2));
		assertFalse(table.getColumns().contains(col3));
	}

	@Test
	public void testSetDataRemovesObsoleteItemsAndAddsNewItems() {
		table = new TableView<>();

		final var col1 = new TableColumn<Function<String, Data>, Function<String, Data>>();
		final var col2 = new TableColumn<Function<String, Data>, Function<String, Data>>();

		// Arrange
		keys = new LinkedHashMap<>();
		keys.put("key1", col1);
		keys.put("key2", col2);

		tableController = new DynamicTableController<>(table);
		table.getItems().setAll(List.of(new StringWrapper("item1"), new StringWrapper("item2")));
		data = List.of(new StringWrapper("item1"), new StringWrapper("item3"), new StringWrapper("item4"));

		// Act
		boolean result = tableController.setData(data);

		// Assert
		assertTrue(result);
		assertEquals(3, table.getItems().size());
		assertTrue(table.getItems().contains(new StringWrapper("item1")));
		assertTrue(table.getItems().contains(new StringWrapper("item3")));
		assertTrue(table.getItems().contains(new StringWrapper("item4")));
		assertFalse(table.getItems().contains(new StringWrapper("item2")));
	}

	@Test
	public void testAddDatumAddsDatumAndSorts() {
		table = new TableView<>();

		final var col1 = new TableColumn<Function<String, Data>, Function<String, Data>>();
		final var col2 = new TableColumn<Function<String, Data>, Function<String, Data>>();

		// Arrange
		keys = new LinkedHashMap<>();
		keys.put("key1", col1);
		keys.put("key2", col2);

		tableController = new DynamicTableController<>(table);
		table.getItems().setAll(List.of(new StringWrapper("item1")));

		// Act
		boolean result = tableController.addDatum(new StringWrapper("item2"));

		// Assert
		assertTrue(result);
		assertEquals(2, table.getItems().size());
		assertTrue(table.getItems().contains(new StringWrapper("item1")));
		assertTrue(table.getItems().contains(new StringWrapper("item2")));
	}

	@Test

	public void testRemoveDatumRemovesDatum() {
		table = new TableView<>();

		final var col1 = new TableColumn<Function<String, Data>, Function<String, Data>>();
		final var col2 = new TableColumn<Function<String, Data>, Function<String, Data>>();

		// Arrange
		keys = new LinkedHashMap<>();
		keys.put("key1", col1);
		keys.put("key2", col2);

		tableController = new DynamicTableController<>(table);
		table.getItems().setAll(List.of(new StringWrapper("item1"), new StringWrapper("item2")));

		// Act
		boolean result = tableController.removeDatum(new StringWrapper("item1"));

		// Assert
		assertTrue(result);
		assertEquals(1, table.getItems().size());
		assertFalse(table.getItems().contains(new StringWrapper("item1")));
		assertTrue(table.getItems().contains(new StringWrapper("item2")));
	}

	@Test
	public void testReplaceDatumReplacesDatumAndSorts() {
		table = new TableView<>();

		final var col1 = new TableColumn<Function<String, Data>, Function<String, Data>>();
		final var col2 = new TableColumn<Function<String, Data>, Function<String, Data>>();

		// Arrange
		keys = new LinkedHashMap<>();
		keys.put("key1", col1);
		keys.put("key2", col2);

		tableController = new DynamicTableController<>(table);
		table.getItems().setAll(List.of(new StringWrapper("item1"), new StringWrapper("item2")));

		// Act
		boolean result = tableController.replaceDatum(new StringWrapper("item1"), new StringWrapper("newItem"));
		boolean result2 = tableController.replaceDatum(new StringWrapper("itemNotFound"), new StringWrapper("newItem"));

		// Assert
		assertTrue(result);
		assertFalse(result2);
		assertEquals(2, table.getItems().size());
		assertFalse(table.getItems().contains(new StringWrapper("item1")));
		assertTrue(table.getItems().contains(new StringWrapper("newItem")));
	}

	public record StringWrapper(String value) implements Function<String, DynamicTableController.Data> {
		/**
		 * Applies this function to the given argument.
		 *
		 * @param s the function argument
		 * @return the function result
		 */
		@Override
		public Data apply(String s) {
			return new Data.Text(value);
		}
	}

	@Nested
	class DataTest {
		@Test
		void testTextCompareTo_TextEqual() {
			Data text1 = new Data.Text("hello");
			Data text2 = new Data.Text("hello");
			assertEquals(0, text1.compareTo(text2));
		}

		@Test
		void testTextCompareTo_TextDifferentComparable() {
			Data text1 = new Data.Text("hello");
			Data text2 = new Data.Text("hello", "hello");
			assertEquals(-1, text1.compareTo(text2));
		}

		@Test
		void testTextCompareTo_TextDifferent() {
			Data text1 = new Data.Text("hello");
			Data text2 = new Data.Text("world");
			assertEquals("hello".compareTo("world"), text1.compareTo(text2));
		}

		@Test
		void testTextCompareTo_ObservableText() {
			Data text = new Data.Text("hello");
			Data observableText = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			assertEquals(0, text.compareTo(observableText));
		}

		@Test
		void testTextCompareTo_Graphic() {
			Data text = new Data.Text("hello");
			@SuppressWarnings("DataFlowIssue") Data graphic = new Data.Graphic(() -> null);
			assertEquals(Byte.compare(text.getTag(), graphic.getTag()), text.compareTo(graphic));
		}

		@Test
		void testObservableTextCompareTo_ObservableTextEqual() {
			Data observableText1 = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			Data observableText2 = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			assertEquals(0, observableText1.compareTo(observableText2));
		}

		@Test
		void testObservableTextCompareTo_ObservableTextDifferentComparable() {
			Data observableText1 = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			Data observableText2 = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> "hello");
			assertEquals(-1, observableText1.compareTo(observableText2));
		}

		@Test
		void testObservableTextCompareTo_ObservableTextDifferent() {
			Data observableText1 = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			Data observableText2 = new Data.ObservableText(new ReadOnlyStringWrapper("world"), () -> null);
			assertEquals("hello".compareTo("world"), observableText1.compareTo(observableText2));
		}

		@Test
		void testObservableTextCompareTo_Text() {
			Data observableText = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			Data text = new Data.Text("hello");
			assertEquals(0, observableText.compareTo(text));
		}

		@Test
		void testObservableTextCompareTo_Graphic() {
			Data observableText = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			@SuppressWarnings("DataFlowIssue") Data graphic = new Data.Graphic(() -> null);
			assertEquals(Byte.compare(observableText.getTag(), graphic.getTag()), observableText.compareTo(graphic));
		}

		@Test
		void testGraphicCompareTo_Graphic() {
			@SuppressWarnings("DataFlowIssue") Data graphic1 = new Data.Graphic(() -> null);
			@SuppressWarnings("DataFlowIssue") Data graphic2 = new Data.Graphic(() -> null);
			assertEquals(0, graphic1.compareTo(graphic2));
		}

		@Test
		void testGraphicCompareTo_GraphicDifferentComparable() {
			@SuppressWarnings("DataFlowIssue") Data graphic1 = new Data.Graphic(() -> null);
			@SuppressWarnings("DataFlowIssue") Data graphic2 = new Data.Graphic(() -> null, "g2");
			assertEquals(-1, graphic1.compareTo(graphic2));
		}

		@Test
		void testGraphicCompareTo_Text() {
			@SuppressWarnings("DataFlowIssue") Data graphic = new Data.Graphic(() -> null);
			Data text = new Data.Text("hello");
			assertEquals(Byte.compare(graphic.getTag(), text.getTag()), graphic.compareTo(text));
		}

		@Test
		void testGraphicCompareTo_ObservableText() {
			@SuppressWarnings("DataFlowIssue") Data graphic = new Data.Graphic(() -> null);
			Data observableText = new Data.ObservableText(new ReadOnlyStringWrapper("hello"), () -> null);
			assertEquals(Byte.compare(graphic.getTag(), observableText.getTag()), graphic.compareTo(observableText));
		}

		@Test
		void testComparableNullCases() {
			Data text = new Data.Text("hello");
			final var _ = assertThrows(NullPointerException.class, () -> {
				//noinspection DataFlowIssue
				text.compareTo(null);
			});
		}

		@Test
		void testCompareComparable_Nulls() {
			// Verify compareComparable handles nulls correctly
			assertEquals(0, Data.compareComparable(null, null));
			assertEquals(-1, Data.compareComparable(null, "a"));
			assertEquals(1, Data.compareComparable("a", null));
		}
	}
}
