package library.controllers.common;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import library.utils.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DynamicTableController<Key, Value extends Function<@NotNull Key, DynamicTableController.@NotNull Data>> implements Initializable {
	protected final Map<Key, TableColumn<@NotNull Value, @NotNull Value>> columnMap = new HashMap<>();
	@FXML
	protected TableView<@NotNull Value> table;

	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		loadTable();
	}

	protected abstract @NotNull SequencedMap<Key, TableColumn<@NotNull Value, @NotNull Value>> getKeys();

	protected abstract @NotNull Collection<@NotNull Value> getData();

	public void loadTable() {
		final var keys = getKeys();

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
				columns.add(col);
				return col;
			});
		}

		final var items = table.getItems();
		items.clear();
		items.addAll(getData());
	}

	protected @NotNull TableColumn<@NotNull Value, @NotNull Value> configureColumn(@NotNull Key key, @NotNull TableColumn<@NotNull Value, @NotNull Value> column) {
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
			@NotNull
			public ObservableValue<@NotNull Value> call(@NotNull TableColumn.CellDataFeatures<@NotNull Value, @NotNull Value> param) {
				return new SimpleObjectProperty<>(param.getValue());
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
			@NotNull
			public TableCell<@NotNull Value, @Nullable Value> call(TableColumn<@NotNull Value, @Nullable Value> param) {
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
						if (empty || item == null) {
							setText(null);
							setGraphic(null);
							return;
						}
						switch (item.apply(key)) {
							case Data.Value(final var val) -> {
								setText(val);
								setGraphic(null);
							}
							case Data.Buttons(final var vals) -> {
								setText(null);
								final var buttonBox = new HBox(5);
								for (final var action : vals) {
									final var b = new Button(action._1());
									b.setOnAction(event -> action._2().accept(event));
									buttonBox.getChildren().add(b);
								}
								setGraphic(buttonBox);
							}
						}
					}
				};
			}
		});
		return column;
	}

	public sealed interface Data permits Data.Buttons, Data.Value {
		record Value(@NotNull String value) implements Data {
		}

		record Buttons(@NotNull List<Tuple2<@NotNull String, @NotNull Consumer<ActionEvent>>> values)
				implements Data {
		}
	}
}
