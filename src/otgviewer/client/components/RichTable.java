package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import t.common.shared.DataSchema;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * A data grid with functionality for hiding columns and displaying 
 * clickable icons in the leftmost columns.
 * It also has the concepts of data columns and extra columns.
 */
abstract public class RichTable<T> extends DataListenerWidget {
	protected DataGrid<T> grid;
	protected List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
	protected int highlightedRow = -1;
	private int extraCols = 0;
	protected int dataColumns = 0;
 	protected final DataSchema schema;
	
	
	public RichTable(DataSchema schema) {
		this.schema = schema;
		hideableColumns = initHideableColumns(schema);
		grid = new DataGrid<T>() {
			@Override
			protected void onBrowserEvent2(Event event) {
				if ("click".equals(event.getType())) {
					EventTarget et = event.getEventTarget();
					if (Element.is(et)) {
						Element e = et.cast();
						String target = e.getString();
						if (!interceptGridClick(target, event.getClientX(), event.getClientY())) {
							return;
						}
					}
					super.onBrowserEvent2(event);
				}
			}
		};
		
		initWidget(grid);
		grid.setWidth("100%");
		grid.setRowStyles(new RowHighligher<T>());
		AsyncHandler colSortHandler = new AsyncHandler(grid);
		grid.addColumnSortHandler(colSortHandler);
	}
	
	/**
	 * TODO clean this mechanism up as much as possible 
	 * @param target
	 * @return true if the click event should be propagated further.
	 */
	protected boolean interceptGridClick(String target, int x, int y) {
		return true;
	}
	
	protected void setupColumns() {
		// TODO: explicitly set the width of each column

		int count = grid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			grid.removeColumn(0);
		}
		grid.getColumnSortList().clear();
		
		dataColumns = 0;
		extraCols = 0;
		Column<T, String> tcl = toolColumn(toolCell());
		
		grid.addColumn(tcl, "");
		extraCols += 1;
		tcl.setCellStyleNames("clickCell");
		grid.setColumnWidth(tcl, "40px");		
		
		for (HideableColumn c: hideableColumns) {
			if (c.visible()) {
				Column<T, ?> cc = (Column<T, ?>) c;
				addExtraColumn(cc, c.name());												
			}
		}		
	}
	
	/**
	 * Obtain the index of the column at the given x-position. Only works
	 * if there is at least one row in the table.
	 * (!!)
	 * @param x
	 * @return
	 */
	protected int columnAt(int x) {
		int prev = 0;
		for (int i = 0; i < grid.getColumnCount() - 1; ++i) {
			int next = grid.getRowElement(0).getCells().getItem(i + 1).getAbsoluteLeft();
			if (prev <= x && next > x) {
				return i;
			}
			prev = next;
		}
		return grid.getColumnCount() - 1;
	}
	
	protected Cell<String> toolCell() { return new TextCell(); }
	
	abstract protected Column<T, String> toolColumn(Cell<String> cell);
	
	protected SafeHtml headerHtml(String title, String tooltip) {
		 return SafeHtmlUtils.fromSafeConstant("<span title=\"" + tooltip + "\">" + title + "</span>");
	}
	
	protected void addColWithTooltip(Column<T, ?> c, String title, String tooltip) {		
		grid.addColumn(c, getColumnHeader(grid.getColumnCount(), headerHtml(title, tooltip)));
	}
	
	protected void insertColWithTooltip(Column<T, ?> c, int at, String title, String tooltip) {
		grid.insertColumn(at, c, getColumnHeader(at, headerHtml(title, tooltip)));
	}
	
	protected Header<SafeHtml> getColumnHeader(int column, SafeHtml safeHtml) {
		return new SafeHtmlHeader(safeHtml);
	}
	
	public void addDataColumn(Column<T, ?> col, String title, String tooltip) {
		col.setSortable(true);	
		addColWithTooltip(col, title, tooltip);		
		col.setCellStyleNames("dataColumn");		
		if (dataColumns == 0 && grid.getColumnSortList().size() == 0) {
			grid.getColumnSortList().push(col); //initial sort
		}
		dataColumns += 1;
	}
	
	/**
	 * Remove a column without altering the sort order, if possible
	 * @param c
	 */
	public void removeDataColumn(Column<T, ?> c) {
		ColumnSortList csl = grid.getColumnSortList();
		
		for (int i = 0; i < csl.size(); ++i) {
			ColumnSortInfo csi = grid.getColumnSortList().get(i);
			if (csi.getColumn() == c) {
				csl.remove(csi);
				break;
			}
		}		
		grid.removeColumn(c);
		dataColumns -= 1;
	}

	private int sortCol;
	private boolean sortAsc;
	public void computeSortParams() {
		ColumnSortList csl = grid.getColumnSortList();
		sortAsc = false;
		sortCol = 0;
		if (csl.size() > 0) {
			sortCol = grid.getColumnIndex(
					(Column<T, ?>) csl.get(0).getColumn())
					- extraCols;
			sortAsc = csl.get(0).isAscending();
		}
	}
	
	/**
	 * The offset of the data column being sorted (within the data columns only)
	 */
	public int sortDataColumnIdx() { 		
		return sortCol; 
	}
		
	public boolean sortAscending() {		
		return sortAsc; 
	}
	
	/**
	 * An "extra" column is a column that is not a data column.
	 * @param col
	 * @param name
	 */
	private void addExtraColumn(Column<T, ?> col, String name) {
		col.setCellStyleNames("extraColumn");
		extraCols += 1;
		insertColWithTooltip(col, extraCols - 1, name, name);		
	}
	
	private void removeExtraColumn(Column<T, ?> col) {
		grid.removeColumn(col);
		extraCols -= 1;
	}
	
	/**
	 * Obtain the number of leading columns before the main data columns.
	 * @return
	 */
	protected int numExtraColumns() {
		return extraCols;
	}
	
	abstract protected List<HideableColumn> initHideableColumns(DataSchema schema);
	
	public List<HideableColumn> getHideableColumns() {
		return hideableColumns;
	}
	
	/**
	 * External users should use this to set a column's visibility,
	 * rather than the hc.setVisibility method.
	 * @param hc
	 */
	public void setVisible(HideableColumn hc, boolean newState) {
		hc.setVisibility(newState);	
		if (newState) {
			addExtraColumn(((Column<T, ?>) hc), hc.name());					
		} else {
			removeExtraColumn((Column<T, ?>) hc);
		}				
	}
	
	public interface HideableColumn {
		String name();
		boolean visible();
		
		// TODO consider not exposing this
		void setVisibility(boolean v);		
	}
	
	/*
	 * The default hideable column
	 */
	protected abstract static class DefHideableColumn<T> extends SafeTextColumn<T> implements HideableColumn {
		private boolean visible;
		public DefHideableColumn(String name, boolean initState) {
			super();
			visible = initState;
			_name = name;
		}
		
		private String _name;
		public String name() { return _name; }
		public boolean visible() { return this.visible; }				
		public void setVisibility(boolean v) { visible = v; }		
	}
	
	protected class RowHighligher<U> implements RowStyles<U> {		
		public RowHighligher() {}

		@Override
		public String getStyleNames(U row, int rowIndex) {
			if (highlightedRow != -1 && rowIndex == highlightedRow + grid.getVisibleRange().getStart()) {
				return "highlightedRow";
			} else {
				return "";
			}
		}		
	}
}
