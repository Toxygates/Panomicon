package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

/**
 * A cell table that displays data and includes a column with checkboxes. By using the checkboxes,
 * the user can select some set of rows.
 * @author johan
 *
 * @param <T>
 */
abstract public class SelectionTable<T> extends Composite implements SetEditor<T> {
	private CellTable<T> table;
	private Column<T, Boolean> selectColumn;
	private Set<T> selected = new HashSet<T>();
	private ListDataProvider<T> provider = new ListDataProvider<T>();
	
	public SelectionTable(final String selectColTitle, boolean fixedLayout) {
		super();
		table = new CellTable<T>();
		initWidget(table);
		
		selectColumn = new Column<T, Boolean>(new CheckboxCell()) {
			@Override
			public Boolean getValue(T object) {
				return selected.contains(object);				
			}
		};
		selectColumn.setFieldUpdater(new FieldUpdater<T, Boolean>() {
			@Override
			public void update(int index, T object, Boolean value) {
				if (value) {					
					selected.add(object);
				} else {
					selected.remove(object);
				}				
				selectionChanged(selected);				
			}
		});
		
		if (fixedLayout) {
			//Fixed width lets us control column widths explicitly
			table.setWidth("100%", true);
		}
		table.addColumn(selectColumn, selectColTitle);
		if (fixedLayout) {
			table.setColumnWidth(selectColumn, "3.5em");
		}
		table.setSelectionModel(new NoSelectionModel<T>());
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		provider.addDataDisplay(table);		
		initTable(table);
	}
	
	abstract protected void initTable(CellTable<T> table);
	
	protected void selectionChanged(Set<T> selected) { }
	
//	public ListDataProvider<T> provider() { return this.provider; }
	public CellTable<T> table() { return this.table; }	
	public Set<T> selection() { return selected; }
	public Set<T> inverseSelection() {
		Set<T> r = new HashSet<T>(provider.getList());
		r.removeAll(selection());
		return r;
	}
	
	public void selectAll(Collection<T> selection) {
		selected.addAll(selection);
		setSelection(new HashSet<T>(selected));
	}
	
	public void unselectAll(Collection<T> selection) {
		selected.removeAll(selection);
		setSelection(new HashSet<T>(selected));
	}
	
	public void setSelection(Collection<T> selection) {
		clearSelection();
		selected = new HashSet<T>(selection);
		table.redraw();
	}
	
	public Set<T> getSelection() {
		return new HashSet<T>(selected);
	}
	
	public void setSelected(T t) {
		selected.add(t);
	}
	
	public void addItem(T t) {
		provider.getList().add(t);		
	}
	
	public void removeItem(T t) {
		provider.getList().remove(t);
		selected.remove(t);
	}
		
	/**
	 * Get an item that was selected by highlighting a row (not by ticking a check box)
	 * @return
	 */
	public T highlightedRow() {
		for (T t : provider.getList()) {
			if (table.getSelectionModel().isSelected(t)) {
				return t;
			}
		}
		return null;		
	}
	
	public void clearSelection() {
		selected = new HashSet<T>();		
		//reset any edits the user might have done
		for (T item: provider.getList()) {
			((CheckboxCell) selectColumn.getCell()).clearViewData(provider.getKey(item));
		}
	}
	
	public void setItems(List<T> data) {
		setItems(data, true);
	}
	
	/**
	 * TODO: retire this method
	 * @param data
	 * @param clearSelection
	 */
	public void setItems(List<T> data, boolean clearSelection) {
		provider.setList(new ArrayList<T>(data));
		table.setVisibleRange(0, data.size());		
		if (clearSelection) {
			clearSelection();
		} else {
			Set<T> toRemove = new HashSet<T>();
			for (T t: selected) {
				if (!data.contains(t)) {
					toRemove.add(t);
				}
			}
			selected.removeAll(toRemove);
		}
	}
	
	public T get(int index) {
		return provider.getList().get(index);
	}
}
