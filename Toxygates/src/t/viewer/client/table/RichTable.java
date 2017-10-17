/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.table;

import java.util.*;

import otgviewer.client.StandardColumns;
import otgviewer.client.components.DataListenerWidget;
import t.common.shared.DataSchema;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.client.Event;

/**
 * A data grid with functionality for hiding columns and displaying clickable icons in the leftmost
 * columns. It also manages a list of named column sections. Columns in a given section are adjacent
 * to each other.
 */
abstract public class RichTable<T> extends DataListenerWidget {
  protected DataGrid<T> grid;
  protected List<HideableColumn<T, ?>> hideableColumns = new ArrayList<HideableColumn<T, ?>>();
  protected int highlightedRow = -1;
  protected boolean shouldComputeTableWidth = true;

  protected final DataSchema schema;
  protected List<ColumnInfo> columnInfos = new ArrayList<ColumnInfo>();

  // Track the order of sections
  private List<String> columnSections = new ArrayList<String>();
  // Track the number of columns in each section
  private Map<String, Integer> sectionColumnCount = new HashMap<String, Integer>();

  protected TableStyle style;
  
  public interface Resources extends DataGrid.Resources {
    @Override
    @Source("t/viewer/client/table/RichTable.css")
    DataGrid.Style dataGridStyle();
  }

  public RichTable(DataSchema schema, TableStyle style) {
    this.schema = schema;
    this.style = style;
    hideableColumns = initHideableColumns(schema);
    Resources resources = GWT.create(Resources.class);
    grid = new DataGrid<T>(50, resources) {
      @Override
      protected void onBrowserEvent2(Event event) {
        if ("click".equals(event.getType())) {
          EventTarget et = event.getEventTarget();
          if (Element.is(et)) {
            Element e = et.cast();
            String target = e.getString();
            if (interceptGridClick(target, event.getClientX(), event.getClientY())) {
              return;
            }
          }
          super.onBrowserEvent2(event);
        }
      }
    };

    initWidget(grid);
    grid.setWidth("100%");
    grid.setRowStyles(new RowHighlighter<T>());
    AsyncHandler colSortHandler = new AsyncHandler(grid);
    grid.addColumnSortHandler(colSortHandler);
  }

  /**
   * TODO clean this mechanism up as much as possible
   * 
   * @param target
   * @return true if the click event should be intercepted and not propagated further.
   */
  protected boolean interceptGridClick(String target, int x, int y) {
    return false;
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
    increaseSectionColumnCount("default");
    // This object will never be used - mainly to keep indexes consistent
    columnInfos.add(new ColumnInfo("", "", false, false, false, false));

    tcl.setCellStyleNames("clickCell");
    grid.setColumnWidth(tcl, "2.5em");

    for (HideableColumn<T, ?> c : hideableColumns) {
      if (c.visible()) {
        ColumnInfo info = c.columnInfo();
        info.setCellStyleNames("extraColumn");
        addColumn(c, "extra", info);
      }
    }
  }

  /**
   * Obtain the index of the column at the given x-position. Only works if there is at least one row
   * in the table. (!!)
   * 
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

  protected Cell<String> toolCell() {
    return new TextCell();
  }

  abstract protected Column<T, String> toolColumn(Cell<String> cell);

  private final static int COL_TITLE_MAX_LEN = 12;

  /**
   * Configure a column
   * 
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
      grid.getColumnSortList().push(c); // initial sort
    }
  }

  protected void ensureSection(String section) {
    if (!columnSections.contains(section)) {
      columnSections.add(section);
      sectionColumnCount.put(section, 0);
    }
  }

  private void increaseSectionColumnCount(String section) {
    int old = sectionColumnCount.get(section);
    sectionColumnCount.put(section, old + 1);
  }

  private int nextColumnIndex(String section) {
    ensureSection(section);
    int c = 0;
    for (String s : columnSections) {
      c += sectionColumnCount.get(s);
      if (s.equals(section)) {
        return c;
      }
    }
    // Should not get here but...
    return c;
  }

  private void decreaseSectionCount(int at) {
    int c = 0;
    for (String s : columnSections) {
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

  /**
   * Computes the width that the table should have, by summing the widths of each table column.
   * Requires that all columns have widths specified in ems.
   * @return the width of the table in ems, as long as every column has a width specified in ems.
   *         Otherwise, null.
   */
  protected Double totalColumnWidth() {
    double totalWidth = 0;
    for (int i = 0; i < grid.getColumnCount(); i++) {
      String widthString = grid.getColumnWidth(grid.getColumn(i));
      if (widthString.endsWith("em")) {
        try {
          totalWidth += Double.parseDouble(widthString.substring(0, widthString.length() - 2));
        } catch (NumberFormatException e) {
          return null;
        }
      } else {
        return null;
      }
    }
    return totalWidth;
  }

  /**
   * Sets the table width based on the total width of columns, if shouldComputeTableWidth = true.
   * Should be called after every operation that causes a change in column widths.
   */
  protected void computeTableWidth() {
    if (shouldComputeTableWidth) {
      Double width = totalColumnWidth();
      if (width != null) {
        grid.setTableWidth(width, Unit.EM);
      }
    }
  }

  protected void addColumn(Column<T, ?> col, String section, ColumnInfo info) {
    int at = nextColumnIndex(section);
    increaseSectionColumnCount(section);
    grid.insertColumn(at, col, getColumnHeader(info));
    setup(col, info);
    columnInfos.add(at, info);
    computeTableWidth();
  }

  protected void removeColumn(Column<T, ?> col) {
    int idx = grid.getColumnIndex(col);
    decreaseSectionCount(idx);
    ColumnInfo info = columnInfos.get(idx);

    if (info.sortable()) {
      // Try to keep the original sort order
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
    computeTableWidth();
  }

  abstract protected List<HideableColumn<T, ?>> initHideableColumns(DataSchema schema);

  public List<HideableColumn<T, ?>> getHideableColumns() {
    return hideableColumns;
  }

  /**
   * External users should use this to set a column's visibility.
   * 
   * @param hc
   */
  public void setVisible(HideableColumn<T, ?> hc, boolean newState) {
    hc.setVisibility(newState);
    if (newState) {
      ColumnInfo info = hc.columnInfo();
      info.setCellStyleNames("extraColumn");
      addColumn(hc, "extra", info);
    } else {
      removeColumn(hc);
    }
  }

  public abstract static class HideableColumn<T, C> extends Column<T, C> {
    public HideableColumn(Cell<C> cell, boolean initState) {
      super(cell);
      _visible = initState;
    }

    protected boolean _visible;
    protected ColumnInfo _columnInfo;

    public ColumnInfo columnInfo() {
      return _columnInfo;
    }

    public boolean visible() {
      return _visible;
    }

    void setVisibility(boolean v) {
      _visible = v;
    }
  }

  /**
   * A hideable column that displays SafeHtml
   */
  protected abstract static class HTMLHideableColumn<T> extends HideableColumn<T, SafeHtml> {

    protected String _width;
    protected String _name;
    protected SafeHtmlCell _c;

    public HTMLHideableColumn(SafeHtmlCell c, String name, boolean initState, String width) {
      super(c, initState);
      this._c = c;
      _name = name;
      _width = width;
      _columnInfo = new ColumnInfo(name, width, false);
    }
    
    public HTMLHideableColumn(SafeHtmlCell c, String name, 
        StandardColumns col, TableStyle style) {
      this(c, name, style.initVisibility(col), style.initWidth(col));
    }

    @Override
    public SafeHtml getValue(T er) {
      SafeHtmlBuilder build = new SafeHtmlBuilder();
      build.appendHtmlConstant(getHtml(er));
      return build.toSafeHtml();
    }

    protected abstract String getHtml(T er);
  }

  protected class RowHighlighter<U> implements RowStyles<U> {
    public RowHighlighter() {}

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
