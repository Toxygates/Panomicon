package t.viewer.client.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.components.DataListenerWidget;
import t.common.shared.DataSchema;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
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
 * It also manages a list of named column sections. 
 * Columns in a given section are adjacent to each other.
 */
abstract public class RichTable<T> extends DataListenerWidget {
	protected DataGrid<T> grid;
	protected List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
	protected int highlightedRow = -1;
	
 	protected final DataSchema schema; 	
 	protected List<ColumnInfo> columnInfos = new ArrayList<ColumnInfo>();
 	
 	//Track the order of sections
	private List<String> columnSections = new ArrayList<String>();
	//Track the number of columns in each section
	private Map<String, Integer> sectionColumnCount = new HashMap<String, Integer>();
 	
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
		int count = grid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			grid.removeColumn(0);
		}
		grid.getColumnSortList().clear();
		columnInfos = new ArrayList<ColumnInfo>();
		columnSections = new ArrayList<String>();
		sectionColumnCount = new HashMap<String, Integer>();
		
		ensureSection("default");
		ensureSection("extra");
		ensureSection("data");
		
		Column<T, String> tcl = toolColumn(toolCell());
		
		grid.addColumn(tcl, "");
		increaseSectionCount("default");
		//This object will never be used - mainly to keep indexes consistent
		columnInfos.add(new ColumnInfo("", "", false, false, false));
				
		tcl.setCellStyleNames("clickCell");
		grid.setColumnWidth(tcl, "40px");		
		
		for (HideableColumn c: hideableColumns) {
			if (c.visible()) {
				Column<T, ?> cc = (Column<T, ?>) c;
				ColumnInfo info = new ColumnInfo(c, false);
				info.setCellStyleNames("extraColumn");
				addColumn(cc, "extra", info);												
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
	
	private final static int COL_TITLE_MAX_LEN = 12;

	/**
	 * Configure a column
	 * @param c
	 * @param info
	 */
	private void setup(Column<T, ?> c, ColumnInfo info) {
		grid.setColumnWidth(c, info.width());
		if (info.cellStyleNames() != null) {
			c.setCellStyleNames(info.cellStyleNames());
		}
		c.setSortable(info.sortable());
		c.setDefaultSortAscending(info.defaultSortAsc());

		if (info.sortable() && grid.getColumnSortList().size() == 0) {		
			grid.getColumnSortList().push(c); //initial sort
		}
	}
	
	protected void ensureSection(String section) {
		if (!columnSections.contains(section)) {
			columnSections.add(section);
			sectionColumnCount.put(section, 0);
		}
	}
	
	private void increaseSectionCount(String section) {
		int old = sectionColumnCount.get(section);
		sectionColumnCount.put(section, old + 1);
	}
	
	private int nextColumnIndex(String section) {
		ensureSection(section);
		int c = 0;
		for (String s: columnSections) {
			c += sectionColumnCount.get(s);
			if (s.equals(section)) {
				return c;
			}
		}
		//Should not get here but...
		return c;
	}
	
	private void decreaseSectionCount(int at) {
		int c = 0;
		for (String s: columnSections) {
			c += sectionColumnCount.get(s);
			if (c >= at && c > 0) {
				int old = sectionColumnCount.get(s);
				sectionColumnCount.put(s, old - 1);
				return;
			}			
		}
	}
	
	protected int sectionCount(String section) {
		return sectionColumnCount.get(section);
	}
	
	protected Header<SafeHtml> getColumnHeader(ColumnInfo info) {		
		ColumnInfo i = info.trimTitle(COL_TITLE_MAX_LEN);	
		return new SafeHtmlHeader(i.headerHtml());
	}
	
	protected void addColumn(Column<T, ?> col, String section, ColumnInfo info) {		
		int at = nextColumnIndex(section);
		increaseSectionCount(section);
		grid.insertColumn(at, col, getColumnHeader(info));
		setup(col, info);		
		columnInfos.add(at, info);
	}
	
	protected void removeColumn(Column<T, ?> col) {
		int idx = grid.getColumnIndex(col);
		decreaseSectionCount(idx);
		ColumnInfo info = columnInfos.get(idx);
		
		if (info.sortable()) {
			//Try to keep the original sort order
			ColumnSortList csl = grid.getColumnSortList();			
			for (int i = 0; i < csl.size(); ++i) {
				ColumnSortInfo csi = grid.getColumnSortList().get(i);
				if (csi.getColumn() == col) {
					csl.remove(csi);
					break;
				}
			}		
		}
		columnInfos.remove(idx);
		grid.removeColumn(col);		
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
			ColumnInfo info = new ColumnInfo(hc, false);
			info.setCellStyleNames("extraColumn");
			addColumn(((Column<T, ?>) hc), "extra", info);			
		} else {
			removeColumn((Column<T, ?>) hc);
		}				
	}
	
	public interface HideableColumn {
		String name();
		boolean visible();
		
		// TODO consider not exposing this
		void setVisibility(boolean v);		
		String width();
	}

	/**
	 * A hideable column that displays SafeHtml
	 * TODO: remove DefHideableColumn or reduce code duplication
	 * @author johan
	 *
	 * @param <T>
	 */
	protected abstract static class HTMLHideableColumn<T> extends Column<T, SafeHtml> implements HideableColumn {
		protected boolean _visible;
		protected String _width;
		protected String _name;
		protected SafeHtmlCell _c;
		
		public HTMLHideableColumn(SafeHtmlCell c, String name, boolean initState, String width) {
			super(c);
			this._c = c;
			_visible = initState;
			_name = name;
			_width = width;
		}
		
		public SafeHtml getValue(T er) {			
			SafeHtmlBuilder build = new SafeHtmlBuilder();			
			build.appendHtmlConstant(getHtml(er));
			return build.toSafeHtml();
		}
		
		protected abstract String getHtml(T er);
		
		public String name() { return _name; }
		public boolean visible() { return _visible; }				
		public void setVisibility(boolean v) { _visible = v; }		
		public String width() { return _width; }
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
