package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.MenuBar;

/**
 * A data grid with functionality for hiding columns and displaying clickable icons in the leftmost columns.
 * It also has the concepts of data columns and extra columns.
 * @author johan
 *
 */
abstract public class RichTable<T> extends DataListenerWidget {
	protected DataGrid<T> grid;
	protected List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
	protected int highlightedRow = -1;
	private int extraCols = 0;
	protected int dataColumns = 0;
 	
	public RichTable() {
		hideableColumns = initHideableColumns();
		grid = new DataGrid<T>();
		initWidget(grid);
		grid.setWidth("100%");
		grid.setRowStyles(new RowHighligher<T>());
		AsyncHandler colSortHandler = new AsyncHandler(grid);
		grid.addColumnSortHandler(colSortHandler);
	}
	
	protected void setupColumns() {
		// todo: explicitly set the width of each column

		int count = grid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			grid.removeColumn(0);
		}
		grid.getColumnSortList().clear();
		
		dataColumns = 0;
		extraCols = 0;
		Column<T, String> tcl = toolColumn(toolCell());
		
		grid.addColumn(tcl, "");
		tcl.setCellStyleNames("clickCell");
		grid.setColumnWidth(tcl, "40px");		
		extraCols += 1;
		
		for (HideableColumn c: hideableColumns) {
			if (c.visible()) {
				Column<T, ?> cc = (Column<T, ?>) c;
				addExtraColumn(cc, c.name());												
			}
		}		
	}
	
	protected Cell<String> toolCell() { return new TextCell(); }
	
	abstract protected Column<T, String> toolColumn(Cell<String> cell);
	
	protected void addColWithTooltip(Column<T, ?> c, String title, String tooltip) {
		grid.addColumn(c, SafeHtmlUtils.fromSafeConstant("<span title=\"" + 
				tooltip + "\">" + title + "</span>"));
	}
	
	protected void insertColWithTooltip(Column<T, ?> c, int at, String title, String tooltip) {
		grid.insertColumn(at, c, SafeHtmlUtils.fromSafeConstant("<span title=\"" + 
				tooltip + "\">" + title + "</span>"));
	}
	
	public void addDataColumn(Column<T, ?> col, String title, String tooltip) {
		col.setSortable(true);	
		col.setDefaultSortAscending(false);
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
			sortCol = grid
					.getColumnIndex((Column<T, ?>) csl.get(
							0).getColumn())
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
		insertColWithTooltip(col, extraCols, name, name);		
		extraCols += 1;
	}
	
	private void removeExtraColumn(Column<T, ?> col) {
		grid.removeColumn(col);
		extraCols -= 1;
	}
	
	abstract protected List<HideableColumn> initHideableColumns();
	
	/**
	 * Create tick menu items corresponding to the hideable columns.
	 * @param mb
	 */
	protected void setupMenuItems(MenuBar mb) {
		for (final HideableColumn c: hideableColumns) {
			new TickMenuItem(mb, c.name(), c.visible()) {
				@Override
				public void stateChange(boolean newState) {
					c.setVisibility(newState);	
					if (newState) {
						addExtraColumn(((Column<T, ?>) c), c.name());					
					} else {
						removeExtraColumn((Column<T, ?>) c);
					}				

				}				
			};
		}
	}
	
	protected interface HideableColumn {
		String name();
		boolean visible();
		void setVisibility(boolean v);		
	}
	
	/*
	 * The default hideable column
	 */
	protected abstract static class DefHideableColumn<T> extends TextColumn<T> implements HideableColumn {
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
