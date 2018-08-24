/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.Screen;
import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.viewer.client.PersistedState;

/**
 * A data grid with functionality for hiding columns and displaying clickable icons in the leftmost
 * columns. It also manages a list of named column sections. Columns in a given section are adjacent
 * to each other.
 */
abstract public class RichTable<T> extends Composite implements RequiresResize {
  protected DataGrid<T> grid;
  protected List<HideableColumn<T, ?>> hideableColumns = new ArrayList<HideableColumn<T, ?>>();
  protected int highlightedRow = -1;
  protected boolean shouldComputeTableWidth = true;
  protected final boolean keepSortOnReload;

  protected final DataSchema schema;
  protected List<ColumnInfo> columnInfos = new ArrayList<ColumnInfo>();

  // Track the order of sections
  protected List<String> columnSections = new ArrayList<String>();
  // Track the number of columns in each section
  protected Map<String, Integer> sectionColumnCount = new HashMap<String, Integer>();

  Logger logger;

  protected SampleClass chosenSampleClass;
  protected String[] chosenProbes = new String[0];
  protected List<Group> chosenColumns = new ArrayList<Group>();

  public interface Resources extends DataGrid.Resources {
    @Override
    @Source("t/viewer/client/table/Tables.gss")
    DataGrid.Style dataGridStyle();
  }

  protected TableStyle style;

  protected Screen screen;
  protected Label titleLabel = new Label();
 
  public RichTable(Screen screen, TableStyle style, TableFlags flags) {
    this.screen = screen;
    this.schema = screen.manager().schema();
    this.style = style;
    this.keepSortOnReload = flags.keepSortOnReload;
    
    String title = flags.title;
    logger = screen.getLogger();

    hideableColumns = initHideableColumns(schema);
    Resources resources = GWT.create(Resources.class);
    grid = new DataGrid<T>(50, resources) {
      @Override
      protected void onBrowserEvent2(Event event) {
        if ("click".equals(event.getType())) {
          EventTarget et = event.getEventTarget();          
          if (Element.is(et)) {
            Element e = et.cast();            
            String targetId = e.getParentElement().getId();            
            if (interceptGridClick(targetId, event.getClientX(), event.getClientY())) {
              return;
            }
          }
          super.onBrowserEvent2(event);
        }
      }
    };

    LayoutPanel lp = new LayoutPanel();
    
    lp.add(grid);
    if (title != null) {
      titleLabel.setText(title);

      titleLabel.addStyleName("exprGrid-title");
      lp.add(titleLabel);
      lp.setWidgetTopHeight(titleLabel, 0, Unit.PX, 30, Unit.PX);
      lp.setWidgetLeftRight(grid, 0, Unit.PX, 0, Unit.PX);
      lp.setWidgetTopBottom(grid, 30, Unit.PX, 0, Unit.PX);
    }
    
    initWidget(lp);
    
//    initWidget(grid);
    grid.setWidth("100%");
    grid.setRowStyles(new RowHighlighter());
    AsyncHandler colSortHandler = new AsyncHandler(grid);
    grid.addColumnSortHandler(colSortHandler);
  }
  
  public void setTitleHeader(String title) {
    titleLabel.setText(title);
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

  protected ColumnSortInfo oldSortInfo;
  
  protected void setupColumns() {
    int count = grid.getColumnCount();
    for (int i = 0; i < count; ++i) {
      grid.removeColumn(0);
    }
    
    ColumnSortList csl = grid.getColumnSortList();
    if (csl.size() > 0) {
      oldSortInfo = csl.get(0);
    } else {
      oldSortInfo = null;
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

    setupHideableColumns();
  }
  
  public void loadColumnVisibility() {
    columnState.load(screen.getParser());
    Set<String> preferredColumns = columnState.getValue();
    if (preferredColumns != null) {
      for (HideableColumn<T, ?> c : hideableColumns) {
        //TODO: we ignore persisted state for standard columns - for now this is to
        //play nicely with dual data screen
        if (c.col == null) {
          ColumnInfo info = c.columnInfo();
          boolean visible = preferredColumns.contains(info.title());
          c.setVisibility(visible);
        }
      }
    }
  }

  protected void setupHideableColumns() {
    boolean first = true;

    for (HideableColumn<T, ?> column : hideableColumns) {
      if (grid.getColumnIndex(column) >= 0) {
        removeColumn(column);
      }      
      if (column.visible()) {
        ColumnInfo info = column.columnInfo();
        String borderStyle = first ? "darkBorderLeft" : "lightBorderLeft";
        first = false;
        info.setCellStyleNames("extraColumn " + borderStyle);
        info.setHeaderStyleNames(borderStyle);
        addColumn(column, "extra", info);
      }
    }    
  }

  /**
   * Obtain the index of the column at the given x-position. Only works if there is at least one row
   * in the table. (!!)
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

  private final static int COL_TITLE_MAX_LEN = 8;

  /**
   * Sets a Column's properties according to a ColumnInfo
   */
  private void setupColumn(Column<T, ?> c, ColumnInfo info) {
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
    int index = 0;
    for (String s : columnSections) {
      index += sectionColumnCount.get(s);
      if (s.equals(section)) {
        return index;
      }
    }
    // Should not get here but...
    return index;
  }

  private void decreaseSectionColumnCount(int at) {
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
    SafeHtmlHeader header = new SafeHtmlHeader(i.headerHtml());
    header.setHeaderStyleNames(info.headerStyleNames());
    return header;
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
    setupColumn(col, info);
    columnInfos.add(at, info);
    computeTableWidth();
  }

  protected void removeColumn(Column<T, ?> col) {
    int idx = grid.getColumnIndex(col);
    if (idx == -1) {
      return;
    }
    decreaseSectionColumnCount(idx);
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

  public void setStyleAndApply(TableStyle style) {
    this.style = style;
    for (HideableColumn<T, ?> col: hideableColumns) {
      reapplyStyle(style, col);
    }
  }
  
  //Only toggles visibility flag in the column.
  protected void reapplyStyle(TableStyle style, HideableColumn<T, ?> col) {
    StandardColumns c = col.col;
    if (c != null) {
      grid.setColumnWidth(col, style.initWidth(c));
      col.setVisibility(style.initVisibility(c));
    }    
  }
  
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

    // We need to set up all the columns each time in order to style borders correctly
    setupHideableColumns();
  }

  protected class RowHighlighter implements RowStyles<T> {
    public RowHighlighter() {}

    @Override
    public String getStyleNames(T row, int rowIndex) {
      if (highlightedRow != -1 && rowIndex == highlightedRow + grid.getVisibleRange().getStart()) {
        return "highlightedRow";
      } else if (isIndicated(row)) {
        return "indicatedRow";
      } else {
        return "";
      }
    }
  }
  
  protected boolean isIndicated(T row) {
    return false;
  }

  @Override
  public void onResize() {
    grid.onResize();
  }
  
  public void persistColumnState() {
    Set<String> vs = hideableColumns.stream().filter(c -> c.visible()).
        map(c -> c.columnInfo().title()).collect(Collectors.toSet());
    columnState.changeAndPersist(screen.manager().getParser(), vs);
  }
  
  public boolean persistedVisibility(String columnId, boolean default_) {
    if (columnState.getValue() == null) {
      return default_;
    }
    return columnState.getValue().contains(columnId);
  }
  
  protected PersistedState<Set<String>> columnState = new PersistedState<Set<String>>(
      "Hideable columns", "hideableColumns") {

    @Override
    protected String doPack(Set<String> state) {      
      return SharedUtils.packList(state, ":::");
    }

    @Override
    protected Set<String> doUnpack(String state) {
      return new HashSet<String>(Arrays.asList(state.split(":::")));
    }
  };

  public void columnsChanged(List<Group> columns) {
    chosenColumns = columns;
  }
  
  public List<Group> chosenColumns() {
    return chosenColumns;
  }

  public void sampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;
  }

  public void probesChanged(String[] probes) {
    chosenProbes = probes;
  }
}
