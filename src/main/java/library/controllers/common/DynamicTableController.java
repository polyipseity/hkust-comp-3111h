package library.controllers.common;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DynamicTableController<Key, Value extends Function<Key, DynamicTableController.Data>> {
	public final TableView<@Nullable Value> table;
	protected final Map<Key, TableColumn<Value, @Nullable Value>> columnMap = new HashMap<>();

	public DynamicTableController(TableView<@Nullable Value> table, SequencedMap<Key, TableColumn<Value, @Nullable Value>> keys) {
		this(table);
		table.getItems().clear(); // Odd that it contains `TableColumn`s...
		setKeys(keys);
	}

	public DynamicTableController(TableView<@Nullable Value> table) {
		this.table = table;
	}

	public void setKeys(SequencedMap<Key, TableColumn<Value, @Nullable Value>> keys) {
		setData(List.of());

		// Remove obsolete keys
		final var columns = table.getColumns();
		columnMap.entrySet().removeIf(entry -> {
			if (keys.containsKey(entry.getKey())) return false;
			columns.remove(entry.getValue());
			return true;
		});
		// Add new keys
		for (final var keyEntry : keys.entrySet()) {
			columnMap.computeIfAbsent(keyEntry.getKey(), key -> {
				final var col = configureColumn(key, keyEntry.getValue());
				if (!columns.contains(col)) columns.add(col);
				return col;
			});
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean setData(Collection<Value> data) {
		final var items = table.getItems();
		return items.removeIf(Predicate.not(data::contains)) // Remove obsolete items
				|| items.addAll(data.stream().filter(Predicate.not(items::contains)).toList()); // Add new items
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean addDatum(@NotNull Value datum) {
		return table.getItems().add(datum);
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean removeDatum(@NotNull Value datum) {
		return table.getItems().remove(datum);
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean replaceDatum(@NotNull Value oldDatum, @NotNull Value datum) {
		final var items = table.getItems();
		return switch (Integer.valueOf(items.indexOf(oldDatum))) {
			case -1 -> false;
			case Integer idx -> {
				items.set(idx, datum);
				yield true;
			}
		};
	}

	protected TableColumn<Value, @Nullable Value> configureColumn(@NotNull Key key, TableColumn<Value, @Nullable Value> column) {
		column.setComparator(Comparator.comparing(datum -> Objects.requireNonNull(datum).apply(key)));
		column.setCellValueFactory(new Callback<>() {
			/**
			 * The <code>call</code> method is called when required, and is given a
			 * single argument of type P, with a requirement that an object of type R
			 * is returned.
			 *
			 * @param param The single argument upon which the returned value should be
			 *      determined.
			 * @return An object of type R that may be determined based on the provided
			 *      parameter value.
			 */
			@Override
			public ObservableValue<Value> call(TableColumn.CellDataFeatures<Value, Value> param) {
				return new ReadOnlyObjectWrapper<>(param.getValue());
			}
		});
		column.setCellFactory(new Callback<>() {
			/**
			 * The <code>call</code> method is called when required, and is given a
			 * single argument of type P, with a requirement that an object of type R
			 * is returned.
			 *
			 * @param param The single argument upon which the returned value should be
			 *      determined.
			 * @return An object of type R that may be determined based on the provided
			 *      parameter value.
			 */
			@Override
			public TableCell<Value, @Nullable Value> call(TableColumn<Value, @Nullable Value> param) {
				return new TableCell<>() {
					/**
					 * The updateItem method should not be called by developers, but it is the
					 * best method for developers to override to allow for them to customise the
					 * visuals of the cell. To clarify, developers should never call this method
					 * in their code (they should leave it up to the UI control, such as the
					 * {@link ListView} control) to call this method. However,
					 * the purpose of having the updateItem method is so that developers, when
					 * specifying custom cell factories (again, like the ListView
					 * {@link ListView#cellFactoryProperty() cell factory}),
					 * the updateItem method can be overridden to allow for complete customisation
					 * of the cell.
					 *
					 * <p>It is <strong>very important</strong> that subclasses
					 * of Cell override the updateItem method properly, as failure to do so will
					 * lead to issues such as blank cells or cells with unexpected content
					 * appearing within them. Here is an example of how to properly override the
					 * updateItem method:
					 *
					 * <pre>
					 * protected void updateItem(T item, boolean empty) {
					 *     super.updateItem(item, empty);
					 *
					 *     if (empty || item == null) {
					 *         setText(null);
					 *         setGraphic(null);
					 *     } else {
					 *         setText(item.toString());
					 *     }
					 * }
					 * </pre>
					 *
					 * <p>Note in this code sample two important points:
					 * <ol>
					 *     <li>We call the super.updateItem(T, boolean) method. If this is not
					 *     done, the item and empty properties are not correctly set, and you are
					 *     likely to end up with graphical issues.</li>
					 *     <li>We test for the <code>empty</code> condition, and if true, we
					 *     set the text and graphic properties to null. If we do not do this,
					 *     it is almost guaranteed that end users will see graphical artifacts
					 *     in cells unexpectedly.</li>
					 * </ol>
					 *
					 * @param item The new item for the cell.
					 * @param empty whether this cell represents data from the list. If it
					 *        is empty, then it does not represent any domain data, but is a cell
					 *        being used to render an "empty" row.
					 */
					@Override
					protected void updateItem(@Nullable Value item, boolean empty) {
						if (item == getItem()) return;
						super.updateItem(item, empty);
						textProperty().unbind();
						setText(null);
						setGraphic(null);
						if (empty || item == null) {
							return;
						}
						switch (item.apply(key)) {
							case Data.Text(final var val) -> setText(val);
							case Data.ObservableText(final var val) -> textProperty().bind(val);
							case Data.Graphic(final var val) -> setGraphic(val.get());
						}
					}
				};
			}
		});
		return column;
	}

	public sealed interface Data extends Comparable<Data> permits Data.Graphic, Data.ObservableText, Data.Text {
		byte getTag();

		record Text(String value) implements Data {
			public static final byte TAG = 0;

			@Override
			public byte getTag() {
				return TAG;
			}

			/**
			 * Compares this object with the specified object for order.  Returns a
			 * negative integer, zero, or a positive integer as this object is less
			 * than, equal to, or greater than the specified object.
			 *
			 * <p>The implementor must ensure {@link Integer#signum
			 * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
			 * all {@code x} and {@code y}.  (This implies that {@code
			 * x.compareTo(y)} must throw an exception if and only if {@code
			 * y.compareTo(x)} throws an exception.)
			 *
			 * <p>The implementor must also ensure that the relation is transitive:
			 * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
			 * {@code x.compareTo(z) > 0}.
			 *
			 * <p>Finally, the implementor must ensure that {@code
			 * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
			 * == signum(y.compareTo(z))}, for all {@code z}.
			 *
			 * @param o the object to be compared.
			 * @return a negative integer, zero, or a positive integer as this object
			 * is less than, equal to, or greater than the specified object.
			 * @throws NullPointerException if the specified object is null
			 * @throws ClassCastException   if the specified object's type prevents it
			 *                              from being compared to this object.
			 * @apiNote It is strongly recommended, but <i>not</i> strictly required that
			 * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
			 * class that implements the {@code Comparable} interface and violates
			 * this condition should clearly indicate this fact.  The recommended
			 * language is "Note: this class has a natural ordering that is
			 * inconsistent with equals."
			 */
			@Override
			public int compareTo(Data o) {
				return switch (o) {
					case Graphic _ -> Byte.compare(getTag(), o.getTag());
					case ObservableText(final var val) -> value.compareToIgnoreCase(val.get());
					case Text(final var val) -> value.compareToIgnoreCase(val);
				};
			}
		}

		record ObservableText(ObservableStringValue value) implements Data {
			public static final byte TAG = 1;

			@Override
			public byte getTag() {
				return TAG;
			}

			/**
			 * Compares this object with the specified object for order.  Returns a
			 * negative integer, zero, or a positive integer as this object is less
			 * than, equal to, or greater than the specified object.
			 *
			 * <p>The implementor must ensure {@link Integer#signum
			 * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
			 * all {@code x} and {@code y}.  (This implies that {@code
			 * x.compareTo(y)} must throw an exception if and only if {@code
			 * y.compareTo(x)} throws an exception.)
			 *
			 * <p>The implementor must also ensure that the relation is transitive:
			 * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
			 * {@code x.compareTo(z) > 0}.
			 *
			 * <p>Finally, the implementor must ensure that {@code
			 * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
			 * == signum(y.compareTo(z))}, for all {@code z}.
			 *
			 * @param o the object to be compared.
			 * @return a negative integer, zero, or a positive integer as this object
			 * is less than, equal to, or greater than the specified object.
			 * @throws NullPointerException if the specified object is null
			 * @throws ClassCastException   if the specified object's type prevents it
			 *                              from being compared to this object.
			 * @apiNote It is strongly recommended, but <i>not</i> strictly required that
			 * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
			 * class that implements the {@code Comparable} interface and violates
			 * this condition should clearly indicate this fact.  The recommended
			 * language is "Note: this class has a natural ordering that is
			 * inconsistent with equals."
			 */
			@Override
			public int compareTo(Data o) {
				return switch (o) {
					case Graphic _ -> Byte.compare(getTag(), o.getTag());
					case ObservableText(final var val) -> value.get().compareToIgnoreCase(val.get());
					case Text(final var val) -> value.get().compareToIgnoreCase(val);
				};
			}
		}

		record Graphic(Supplier<? extends Node> value)
				implements Data {
			public static final byte TAG = 2;
			public static final double BUTTON_SPACING = 5;

			@SafeVarargs
			public static Graphic ofButtons(Tuple2<? extends ObservableValue<? extends String>, ? extends BiConsumer<? super Button, ? super ActionEvent>>... buttonData) {
				return new Graphic(() -> {
					final var buttonBox = new HBox(BUTTON_SPACING);
					for (final var buttonDatum : buttonData) {
						final var button = new Button();
						button.textProperty().bind(buttonDatum._1());
						button.setOnAction(event -> buttonDatum._2().accept(button, event));
						buttonBox.getChildren().add(button);
					}
					return buttonBox;
				});
			}

			@Override
			public byte getTag() {
				return TAG;
			}

			/**
			 * Compares this object with the specified object for order.  Returns a
			 * negative integer, zero, or a positive integer as this object is less
			 * than, equal to, or greater than the specified object.
			 *
			 * <p>The implementor must ensure {@link Integer#signum
			 * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
			 * all {@code x} and {@code y}.  (This implies that {@code
			 * x.compareTo(y)} must throw an exception if and only if {@code
			 * y.compareTo(x)} throws an exception.)
			 *
			 * <p>The implementor must also ensure that the relation is transitive:
			 * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
			 * {@code x.compareTo(z) > 0}.
			 *
			 * <p>Finally, the implementor must ensure that {@code
			 * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
			 * == signum(y.compareTo(z))}, for all {@code z}.
			 *
			 * @param o the object to be compared.
			 * @return a negative integer, zero, or a positive integer as this object
			 * is less than, equal to, or greater than the specified object.
			 * @throws NullPointerException if the specified object is null
			 * @throws ClassCastException   if the specified object's type prevents it
			 *                              from being compared to this object.
			 * @apiNote It is strongly recommended, but <i>not</i> strictly required that
			 * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
			 * class that implements the {@code Comparable} interface and violates
			 * this condition should clearly indicate this fact.  The recommended
			 * language is "Note: this class has a natural ordering that is
			 * inconsistent with equals."
			 */
			@Override
			public int compareTo(Data o) {
				return switch (o) {
					case Graphic _ -> 0;
					case ObservableText _, Text _ -> Byte.compare(getTag(), o.getTag());
				};
			}
		}
	}
}
