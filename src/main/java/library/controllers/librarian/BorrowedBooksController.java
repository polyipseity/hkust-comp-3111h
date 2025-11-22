package library.controllers.librarian;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import library.Main;
import library.controllers.common.DynamicTableController;
import library.controllers.common.LoadsData;
import library.controllers.common.RequiresLoggedIn;
import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.utils.TimeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

public final class BorrowedBooksController implements RequiresLoggedIn, Initializable, LoadsData {
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableView<@Nullable Data> table;
	@UnknownNullability
	@SuppressWarnings("unused")
	public DynamicTableController<Keys, Data> tableController;
	@UnknownNullability
	@SuppressWarnings("unused")
	public TableColumn<Data, @Nullable Data> titleCol, borrowerCol, borrowDateCol, durationLeftCol;


	@Override
	public void initialize(@Nullable URL location, @Nullable ResourceBundle resources) {
		final var keys = new LinkedHashMap<Keys, TableColumn<Data, @Nullable Data>>();
		keys.put(Keys.TITLE, titleCol);
		keys.put(Keys.BORROWER, borrowerCol);
		keys.put(Keys.BORROW_DATE, borrowDateCol);
		keys.put(Keys.DURATION_LEFT, durationLeftCol);
		tableController = new DynamicTableController<>(table, keys);
		table.getSortOrder().add(durationLeftCol);

		LoadsData.super.initialize(location, resources);
	}

	@Override
	public void loadData() {
		tableController.setData(Main.getContext().getRepository().borrowOps.read().entrySet().stream()
				.map(entry -> new Data(
						this,
						entry.getKey()._1(),
						entry.getKey()._2(),
						entry.getValue()))
				.toList());
	}

	public enum Keys {
		TITLE,
		BORROWER,
		BORROW_DATE,
		DURATION_LEFT
	}

	public record Data(BorrowedBooksController controller,
	                   User user,
	                   Book book,
	                   Borrow borrow)
			implements Function<Keys, DynamicTableController.Data> {

		/**
		 * Applies this function to the given argument.
		 *
		 * @param key the function argument
		 * @return the function result
		 */
		@Override
		public DynamicTableController.Data apply(Keys key) {
			return switch (key) {
				case TITLE -> new DynamicTableController.Data.Text(book.title());
				case BORROWER -> new DynamicTableController.Data.Text(user.username());
				case BORROW_DATE -> new DynamicTableController.Data.Text(
						TimeUtil.toStringZonedLocal(borrow.borrowDate()));
				case DURATION_LEFT -> {
					final var borrow = this.borrow; // Do not reference `this` in lambda.
					final var prop = new SimpleStringProperty(TimeUtil.toStringDuration(borrow.durationLeft()));
					final var ret = new DynamicTableController.Data.ObservableText(prop);
					final var weakThis = new WeakReference<>(this);
					Main.getContext().addSecondTimelineListener(ret, _ -> {
						prop.setValue(TimeUtil.toStringDuration(borrow.durationLeft()));
						if (borrow.expired()) {
							final var this2 = weakThis.get();
							if (this2 == null) return;
							this2.controller.tableController.removeDatum(this2);
						}
					});
					yield ret;
				}
			};
		}
	}
}
