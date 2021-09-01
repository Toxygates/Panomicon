/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.viewer.client.table;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

import t.gwt.viewer.client.screen.Screen;
import t.gwt.common.client.Utils;
import t.shared.common.DataSchema;
import t.gwt.viewer.client.Analytics;
import t.gwt.viewer.client.components.TickMenuItem;

/**
 * A data grid with functionality for hiding columns and displaying clickable icons in the leftmost
 * columns. It also manages a list of named column sections. Columns in a given section are adjacent
 * to each other.
 */
abstract public class RichTable<T> extends Composite implements RequiresResize {
  protected Screen screen;
  protected PublicHeaderDataGrid<T> grid;
  protected ColumnHelper<T> columnHelper;
  protected Label titleLabel = new Label();

  protected final DataSchema schema;
  protected List<HideableColumn<T, ?>> hideableColumns = new ArrayList<HideableColumn<T, ?>>();
  protected TableStyle style;

  // Track the order of sections
  protected List<String> columnSections = new ArrayList<String>();
  // Track the number of columns in each section
  protected Map<String, Integer> sectionColumnCount = new HashMap<String, Integer>();

  protected int highlightedRow = -1;
  protected boolean shouldComputeTableWidth = true;
  protected final boolean keepSortOnReload;

  Logger logger;
  
  public interface ColumnHelper<T> {
    Column<T, String> toolColumn(Cell<String> cell);
    Cell<String> toolCell();    
    Header<SafeHtml> getColumnHeader(ColumnInfo info);
  }

  public interface Resources extends DataGrid.Resources {
    @Override
    @Source("t/gwt/viewer/client/table/Tables.gss")
    DataGrid.Style dataGridStyle();
  }

  public RichTable(Screen screen, TableStyle style, TableFlags flags) {
    this.screen = screen;
    this.schema = screen.manager().schema();
    this.style = style;
    this.columnHelper = makeColumnHelper(screen);
    this.keepSortOnReload = flags.keepSortOnReload;
    
    String title = flags.title;
    logger = screen.getLogger();

    Resources resources = GWT.create(Resources.class);
    grid = new PublicHeaderDataGrid<T>(50, resources) {
      @Override
      protected void onBrowserEvent2(Event event) {
        String eventId = Utils.clickParentId(event);
        if (eventId != null &&
          interceptGridClick(eventId, event.getClientX(), event.getClientY())) {
            return;
        }
        super.onBrowserEvent2(event);        
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
    
    grid.setWidth("100%");
    grid.setRowStyles(new RowHighlighter());
    AsyncHandler colSortHandler = new AsyncHandler(grid);
    grid.addColumnSortHandler(colSortHandler);
  }
  
  /*
   * We use an abstract method to get a ColumnHelper, rather than make it a
   * constructor argument, because constructing a ColumnHelper might need some
   * logic from a subclass (of RichTable).
   */
  protected abstract ColumnHelper<T> makeColumnHelper(Screen screen);

  public void setTitleHeader(String title) {
    titleLabel.setText(title);
  }

  /**
   * Should we intercept a grid click at the given coordinates?
   * @return true if the click event should be intercepted and not propagated further.
   */
  protected boolean interceptGridClick(String target, int x, int y) {
    return false;
  }
  
  /**
   * Create columns for the DataGrid, adding all the columns that should be
   * displayed based on the current application state. Some of these come from the
   * hideableColumns list and are created in the setupHideableColumns method.
   * 
   * Subclasses that want to display additional columns, outside of the
   * hideableColumns mechanism, should override this method.
   */
  protected void setupColumns() {
    int count = grid.getColumnCount();
    for (int i = 0; i < count; ++i) {
      grid.removeColumn(0);
    }
    grid.getColumnSortList().clear();
    
    columnSections = new ArrayList<String>();
    sectionColumnCount = new HashMap<String, Integer>();

    ensureSection("default");
    ensureSection("extra");
    ensureSection("data");

    // Add column of icons that can be clicked to show charts for the probe
    addColumn(toolColumn(toolCell()), "default",
        new ColumnInfo("", "", false, false, "2.5em", "clickCell", false, false, false));

    setupHideableColumns();
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
   * Obtain the index of the column at the given x-position.
   */
  protected int columnAt(int x) {
    int prev = 0;
    for (int i = 0; i < grid.getColumnCount() - 1; ++i) {
      // We use the second row of table headers, since the first row will not
      // have distinct cells for every table column.
      int next = grid.getTableHeadElement().getRows().getItem(1).getCells().getItem(i + 1).getAbsoluteLeft();
      if (prev <= x && next > x) {
        return i;
      }
      prev = next;
    }
    return grid.getColumnCount() - 1;
  }

  protected Cell<String> toolCell() {
    return columnHelper.toolCell();
  }

  protected Column<T, String> toolColumn(Cell<String> cell) {
    return columnHelper.toolColumn(cell);
  }

  public final static int COL_TITLE_MAX_LEN = 8;

  /**
   * Sets a Column's properties according to a ColumnInfo
   */
  private void setupColumn(Column<T, ?> column, ColumnInfo info) {
    grid.setColumnWidth(column, info.width());
    if (info.cellStyleNames() != null) {
      column.setCellStyleNames(info.cellStyleNames());
    }
    column.setSortable(info.sortable());
    column.setDefaultSortAscending(info.defaultSortAsc());
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

  protected Header<SafeHtml> getColumnHeader(ColumnInfo info) {
    return columnHelper.getColumnHeader(info);
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

  public void addColumn(Column<T, ?> column, String section, ColumnInfo info) {
    int index = nextColumnIndex(section);
    increaseSectionColumnCount(section);
    grid.insertColumn(index, column, getColumnHeader(info));
    setupColumn(column, info);
    computeTableWidth();
  }

  protected void removeColumn(Column<T, ?> column) {
    int index = grid.getColumnIndex(column);
    if (index == -1) {
      return;
    }
    decreaseSectionColumnCount(index);
    grid.removeColumn(column);
    computeTableWidth();
  }

  abstract protected List<HideableColumn<T, ?>> createHideableColumns(DataSchema schema);

  public void setStyleAndApply(TableStyle style) {
    this.style = style;
    for (HideableColumn<T, ?> column : hideableColumns) {
      reapplyStyle(style, column);
    }
  }
  
  //Only toggles visibility flag in the column.
  protected void reapplyStyle(TableStyle style, HideableColumn<T, ?> column) {
    StandardColumns standard = column.standard;
    if (standard != null) {
      grid.setColumnWidth(column, style.initWidth(standard));
      column.setVisibility(style.initVisibility(standard));
    }    
  }
  
  public List<HideableColumn<T, ?>> getHideableColumns() {
    return hideableColumns;
  }

  /**
   * External users should use this to set a column's visibility.
   */
  public void setVisible(HideableColumn<T, ?> column, boolean newState) {
    column.setVisibility(newState);

    // We need to set up all the columns each time in order to style borders correctly
    setupHideableColumns();
  }

  protected class RowHighlighter implements RowStyles<T> {
    public RowHighlighter() {}

    @Override
    public String getStyleNames(T row, int rowIndex) {
      if (highlightedRow != -1 && rowIndex == highlightedRow + grid.getVisibleRange().getStart()) {
        return "highlightedRow";
      } else if (row != null && isIndicated(row)) {
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

  public void redrawGrid() {
    grid.redraw();
  }

  public List<T> visibleItems() {
    return grid.getVisibleItems();
  }

  public Column<T, ?> sectionColumnAtIndex(String desiredSection, int sectionIndex) {
    int totalIndex = 0;
    for (String section : columnSections) {
      if (section == desiredSection) {
        if (sectionIndex < sectionColumnCount.get(section)) {
          return grid.getColumn(totalIndex + sectionIndex);
        } else {
          return null;
        }
      } else {
        totalIndex += sectionColumnCount.get(section);
      }
    }
    return null;
  }
  
  public MenuBar createColumnVisibilityMenu() {
    Set<String> visibleColumns = new HashSet<String>(screen.getStorage().columnStateStorage.getIgnoringException());
    MenuBar menuBar = new MenuBar(true);
    // Task: store the TickMenuItem in HideableColumn so that the state can be synchronised
    for (final HideableColumn<T, ?> column : getHideableColumns()) {
        final String title = column.columnInfo().title();
   
      //Automatically added to the menuBar
      new TickMenuItem(menuBar, title, column.visible()) {
        @Override
        public void stateChange(boolean newState) {
          setVisible(column, newState);
          persistColumnState();
          if (newState) {
              Analytics.trackEvent(Analytics.CATEGORY_TABLE, 
                      Analytics.ACTION_DISPLAY_OPTIONAL_COLUMN, title);
          }
        }
      }.setState(visibleColumns.contains(title));
    }
    return menuBar;
  }
  
  public void loadColumnVisibility() {
    Set<String> visibleColumns = new HashSet<String>(screen.getStorage().columnStateStorage.getIgnoringException()); 
    if (visibleColumns != null) {
      for (HideableColumn<T, ?> c : hideableColumns) {
        // Task: we currently ignore persisted state for standard columns - for now this is to
        //play nicely with dual data screen
        if (c.standard == null) {
          ColumnInfo info = c.columnInfo();
          boolean visible = visibleColumns.contains(info.title());
          c.setVisibility(visible);
        }
      }
    }
  }

  public void persistColumnState() {
    Set<String> visibleColumns = hideableColumns.stream().filter(c -> c.visible()).
        map(c -> c.columnInfo().title()).collect(Collectors.toSet());
    screen.getStorage().columnStateStorage.store(new ArrayList<String>(visibleColumns));
  }
}
