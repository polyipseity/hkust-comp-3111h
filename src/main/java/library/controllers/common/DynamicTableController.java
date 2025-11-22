package library.controllers.common;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DynamicTableController<Key, Value extends Function<Key, DynamicTableController.Data>> {
	public final TableView<@Nullable Value> table;
	protected final Map<Key, TableColumn<Value, @Nullable Value>> columnMap = new HashMap<>();

	public DynamicTableController(TableView<@Nullable Value> table, SequencedMap<Key, TableColumn<Value, @Nullable Value>> keys) {
		this(table);
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
		items.clear();
		return items.addAll(data);
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
					 * {@link javafx.scene.control.ListView} control) to call this method. However,
					 * the purpose of having the updateItem method is so that developers, when
					 * specifying custom cell factories (again, like the ListView
					 * {@link javafx.scene.control.ListView#cellFactoryProperty() cell factory}),
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
							case Data.Graphic(final var val) -> setGraphic(val.get());
							case Data.ObservableText(final var val) -> textProperty().bind(val);
						}
					}
				};
			}
		});
		return column;
	}

	public sealed interface Data permits Data.Graphic, Data.ObservableText, Data.Text {
		record Text(String value) implements Data {
		}

		record Graphic(Supplier<? extends Node> value)
				implements Data {
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
		}

		record ObservableText(ObservableStringValue value) implements Data {
		}
	}
}
