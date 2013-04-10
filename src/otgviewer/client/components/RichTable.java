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
	protected int extraCols = 0;
	protected int dataColumns = 0;
 	
	
	public RichTable() {
		hideableColumns = initHideableColumns();
		grid = new DataGrid<T>();
		initWidget(grid);
		grid.setWidth("100%");
		grid.setRowStyles(new RowHighligher());
		AsyncHandler colSortHandler = new AsyncHandler(grid);
		grid.addColumnSortHandler(colSortHandler);
	}
	
	protected void setupColumns() {
		// todo: explicitly set the width of each column
		TextCell tc = new TextCell();

		int count = grid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			grid.removeColumn(0);
		}
		grid.getColumnSortList().clear();
		
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
	
	protected Cell<String> toolCell() {
		return new TextCell();
	}
	
	abstract protected Column<T, String> toolColumn(Cell<String> cell);
	
	protected void addColWithTooltip(Column<T, ?> c, String title, String tooltip) {
		grid.addColumn(c, SafeHtmlUtils.fromSafeConstant("<span title=\"" + 
				tooltip + "\">" + title + "</span>"));
	}
	
	protected void insertColWithTooltip(Column<T, ?> c, int at, String title, String tooltip) {
		grid.insertColumn(at, c, SafeHtmlUtils.fromSafeConstant("<span title=\"" + 
				tooltip + "\">" + title + "</span>"));
	}
	
	/**
	 * Remove a column without altering the sort order, if possible
	 * @param c
	 */
	protected void removeColumn(Column<T, ?> c) {
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

	
	protected void addDataColumn(Column<T, ?> col, String title, String tooltip) {
		col.setSortable(true);		
		addColWithTooltip(col, title, tooltip);		
		col.setCellStyleNames("dataColumn");		
	}
	
	/**
	 * An "extra" column is a column that is not a data column.
	 * @param col
	 * @param name
	 */
	protected void addExtraColumn(Column<T, ?> col, String name) {
		col.setCellStyleNames("extraColumn");
		insertColWithTooltip(col, extraCols, name, name);		
		extraCols += 1;
	}
	
	protected void removeExtraColumn(Column<T, ?> col) {
		grid.removeColumn(col);
		extraCols -= 1;
	}
	
	abstract protected List<HideableColumn> initHideableColumns();
	
	
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
	
	protected class RowHighligher<T> implements RowStyles<T> {
		
		public RowHighligher() {}

		@Override
		public String getStyleNames(T row, int rowIndex) {
			if (highlightedRow != -1 && rowIndex == highlightedRow + grid.getVisibleRange().getStart()) {
				return "highlightedRow";
			} else {
				return "";
			}
		}		
	}
	

	
}
