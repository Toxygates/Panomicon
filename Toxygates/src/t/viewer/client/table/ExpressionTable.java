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

package t.viewer.client.table;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;

import otg.viewer.client.charts.ChartParameters;
import otg.viewer.client.charts.MatrixCharts;
import otg.viewer.client.components.OTGScreen;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.shared.ManagedMatrixInfo;
import t.viewer.shared.SortKey;

/**
 * The main data display table. This class has many different functionalities. (maybe still too
 * many)
 * 
 * It requests microarray expression data dynamically, displays it, as well as displaying additional
 * dynamic data. It also provides functionality for chart popups. It also has an interface for
 * adding and removing t-tests and u-tests, which can be hidden and displayed on demand.
 * 
 * Data is managed by an ETMatrixManager, which also includes an AsyncDataProvider for the table,
 * providing data for whatever rows the user navigates to.
 * 
 * Hideable columns and clickable icons are handled by the RichTable superclass. Dynamic
 * (association) columns are handled by the AssociationManager helper class.
 */
public class ExpressionTable extends RichTable<ExpressionRow>
    implements
      ETMatrixManager.Delegate,
      ETColumns.Delegate,
      ETHeaderBuilder.Delegate,
      NavigationTools.Delegate,
      AssociationManager.TableDelegate<ExpressionRow> {

  private final String COLUMN_WIDTH = "10em";

  private OTGScreen screen;
  private AssociationManager<ExpressionRow> associations;
  private ETMatrixManager matrix;
  private ETColumns columns;
  private Delegate delegate;

  protected final int initPageSize;
  private final Logger logger = SharedUtils.getLogger("expressionTable");

  private NavigationTools navigationTools;
  private AnalysisTools analysisTools;

  private MatrixCharts charts;
  private ChartDialog lastChartDialog;

  protected boolean displayPColumns = true;
  protected SortKey sortKey;
  protected boolean sortAsc;
  private boolean withPValueOption;

  protected List<Group> chosenColumns = new ArrayList<Group>();
  protected SampleClass chosenSampleClass;
  protected String[] chosenProbes = new String[0];

  protected AbstractSelectionModel<ExpressionRow> selectionModel;

  public interface Delegate {
    void onGettingExpressionFailed(ExpressionTable table);

    void onApplyColumnFilter();

    void afterGetRows(ExpressionTable table);
  }

  public ExpressionTable(OTGScreen _screen, TableFlags flags, TableStyle style,
      ETMatrixManager.Loader loader, Delegate delegate,
      AssociationManager.ViewDelegate<ExpressionRow> viewDelegate) {
    super(_screen, style, flags);
    screen = _screen;
    this.matrix = new ETMatrixManager(_screen, flags, this, loader, grid);
    this.associations = new AssociationManager<ExpressionRow>(screen, this, this, viewDelegate);
    this.delegate = delegate;
    this.withPValueOption = flags.withPValueOption;
    this.initPageSize = flags.initPageSize;

    grid.setHeaderBuilder(new ETHeaderBuilder(grid, this, schema));
    grid.addStyleName("exprGrid");

    if (!flags.allowHighlight) {
      selectionModel = new NoSelectionModel<ExpressionRow>();
      grid.setSelectionModel(selectionModel);
    } else {
      selectionModel = new SingleSelectionModel<ExpressionRow>();
      grid.setSelectionModel(selectionModel);
    }
    grid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    navigationTools = new NavigationTools(grid, withPValueOption, this);

    analysisTools = new AnalysisTools(this);

    hideableColumns = createHideableColumns(schema);

    setEnabled(false);
  }

  @Override
  protected ETColumns makeColumnHelper(OTGScreen screen) {
    this.columns = new ETColumns(this, screen.manager().resources(), COLUMN_WIDTH);
    return columns;
  }

  public AbstractSelectionModel<ExpressionRow> selectionModel() {
    return selectionModel;
  }

  public void setStyle(TableStyle style) {
    this.style = style;
  }

  public ValueType getValueType() {
    return navigationTools.getValueType();
  }

  public Widget tools() {
    return this.navigationTools;
  }

  public void setDisplayPColumns(boolean displayPColumns) {
    if (withPValueOption) {
      this.displayPColumns = displayPColumns;
      navigationTools.setPValueState(displayPColumns);
    }
  }

  public Widget analysisTools() {
    return analysisTools;
  }

  public void downloadCSV(boolean individualSamples) {
    if (individualSamples && matrix.isMergeMode()) {
      Window.alert("Individual samples cannot be downloaded in orthologous mode.\n"
          + "Please inspect one group at a time.");
    } else {
      matrix.downloadCSV(individualSamples);
    }
  }

  private String oldSortColumnHint = null;
  private Group oldSortColumnGroup = null;
  private boolean oldSortAscending = false;

  @Override
  public void setupColumns() {
    super.setupColumns();
    ManagedMatrixInfo matrixInfo = matrix.info();
    columns.addDataColumns(matrixInfo, displayPColumns);
    ensureSection("synthetic");
    columns.addSynthColumns(matrixInfo);
    restoreSort();
  }

  private void storeSortInfo() {
    ColumnSortList csl = grid.getColumnSortList();
    if (csl.size() > 0) {
      Column<?, ?> column = csl.get(0).getColumn();
      if (column instanceof ExpressionColumn) {
        int index = ((ExpressionColumn) column).matrixColumn();
        oldSortColumnHint = matrix.info().columnHint(index);
        oldSortColumnGroup = matrix.info().columnGroup(index);
        oldSortAscending = csl.get(0).isAscending();
      }
    } else {
      oldSortColumnHint = null;
      oldSortColumnGroup = null;
    }
  }

  private void restoreSort() {
    ColumnSortInfo recreatedSortInfo = columns.recreateSortInfo(matrix.info(), grid,
        oldSortColumnHint, oldSortColumnGroup, oldSortAscending);
    if (recreatedSortInfo != null) {
      grid.getColumnSortList().push(recreatedSortInfo);
    } else if (grid.getColumnSortList().size() == 0) {
      Column<ExpressionRow, ?> sortColumn = sectionColumnAtIndex("data", 0);
      grid.getColumnSortList().push(new ColumnSortInfo(sortColumn, false));
    }
  }

  @Override
  protected boolean interceptGridClick(String target, int x, int y) {
    /*
     * To prevent unwanted interactions between the sorting system and the filtering system, we have
     * to intercept click events at this high level and choose whether to pass them on (non-filter
     * clicks) or not (filter clicks).
     */
    // logger.info("Click target: " + target);
    boolean shouldFilterClick = target.equals(FilterCell.CLICK_ID);
    if (shouldFilterClick && matrix.info() != null) {
      // Identify the column that was filtered.
      int col = columnAt(x);
      Column<ExpressionRow, ?> clickedCol = grid.getColumn(col);
      if (clickedCol instanceof ExpressionColumn) {
        ExpressionColumn ec = (ExpressionColumn) clickedCol;
        matrix.editColumnFilter(ec.matrixColumn());
      } else if (clickedCol instanceof AssociationColumn) {
        @SuppressWarnings("unchecked")
        AssociationColumn<ExpressionRow> ac = (AssociationColumn<ExpressionRow>) clickedCol;
        associations.displayColumnSummary(ac);
      }
    }
    // If we return true, the click will not be passed on to the other widgets
    return shouldFilterClick;
  }

  @Override
  protected List<HideableColumn<ExpressionRow, ?>> createHideableColumns(DataSchema schema) {
    List<HideableColumn<ExpressionRow, ?>> hideableColumns = columns.createHideableColumns(schema);
    // We want gene sym, probe title etc. to be before the association
    // columns going left to right
    hideableColumns.addAll(associations.createHideableColumns(schema));
    return hideableColumns;
  }

  public List<ExpressionRow> getDisplayedRows() {
    return grid.getVisibleItems();
  }

  public void columnsChanged(List<Group> columns) {
    HashSet<Group> oldColumns = new HashSet<Group>(chosenColumns);
    HashSet<Group> newColumns = new HashSet<Group>(columns);
    if (newColumns.equals(oldColumns) && newColumns.size() > 0) {
      matrix.logInfo("ExpressionTable ignoring column change signal: no actual change in columns");
      return;
    }

    chosenColumns = columns;

    // We set chosenSampleClass to the intersection of all the samples
    // in the groups here. Needed later for e.g. the associations() call.
    // Note: we might want to move/factor out this
    List<SampleClass> allCs = new LinkedList<SampleClass>();
    for (Group g : columns) {
      allCs.addAll(SampleClassUtils.classes(Arrays.asList(g.getSamples())));
    }

    chosenSampleClass = SampleClass.intersection(allCs);
    logger.info("Set SC to: " + chosenSampleClass.toString());

    analysisTools.columnsChanged(columns);

    charts = new MatrixCharts(screen, columns);

    matrix.clear();
    matrix.lastColumnFilters().clear();
    grid.getColumnSortList().clear();

    matrix.logInfo("Columns changed (" + columns.size() + ")");
  }

  /**
   * Refetch the first page of rows as they are currently represented on the server side.
   * @param forcePageSize The number of rows to fetch, or null to use
   * the pager's default page size. 
   */
  public void refetchRows(@Nullable Integer forcePageSize) {
    int ps = navigationTools.pageSize();
      int count = (forcePageSize == null) ? ps : forcePageSize;
    
    grid.setVisibleRangeAndClearData(new Range(0, count), true);    
  }

  protected Set<String> indicatedRows = new HashSet<String>();

  public void setIndicatedProbes(Set<String> highlighted, boolean redraw) {
    logger.info(highlighted.size() + " rows are indicated");
    Set<String> oldIndicated = indicatedRows;
    indicatedRows = highlighted;

    String[] displayedProbes = matrix.displayedProbes();
    if (redraw) {
      for (int i = 0; i < displayedProbes.length; ++i) {
        if (highlighted.contains(displayedProbes[i]) || oldIndicated.contains(displayedProbes[i])) {
          grid.redrawRow(i);
        }
      }
    }
  }

  @Override
  protected boolean isIndicated(ExpressionRow row) {
    return indicatedRows.contains(row.getProbe());
  }

  public void getExpressions(boolean preserveFilters) {
    matrix.logInfo("Begin loading data for " + chosenColumns.size() + " columns and "
        + chosenProbes.length + " probes");
    matrix.getExpressions(preserveFilters, navigationTools.getValueType());
  }

  private class ChartDialog extends Utils.LocationTrackedDialog {
    private int chartRow; // The row for which this dialog is displaying charts
    private Element mouseDownChartElement = null; // The chart ImageClickCell DOM element that a
                                                  // mousedown event occurred on
    private boolean startedOpeningNewCharts; // True if a new ChartDialog (not this one) has been
                                             // opened

    public ChartDialog() {
      super();
      chartRow = highlightedRow;
      addCloseHandler(new ChartCloseHandler());
    }

    /*
     * If a user clicks on an ImageClickCell, but the mousedown associated with that click causes
     * any row in the DataGrid to be redrawn, then that click won't be recognized. To work around
     * this, this method implements custom logic for the following two cases: 1. mousedown events on
     * a chart ImageClickCell 2. mouseup events associated with a mousedown event that was on a
     * chart ImageClickCell, but occurs somewhere other than that chart ImageClickCell In a third
     * case - if a mouseup event occurs on the same chart ImageClickCell as a mousedown event,
     * that's just a click, which will cause a *new* ChartDialog to be shown. That new ChartDialog
     * will, in its show() method, hide the old ChartDialog.
     */
    @Override
    protected void onPreviewNativeEvent(NativePreviewEvent event) {
      // Get some information about the browser event
      NativeEvent ev = event.getNativeEvent();
      EventTarget et = ev.getEventTarget();
      String parentId = "";
      Element parentElement = null;
      if (Element.is(et)) {
        Element e = et.cast();
        parentId = e.getParentElement().getId(); // The ID of the parent DOM element where the event
                                                 // occurred
        parentElement = e.getParentElement(); // The actual parent DOM element
      }

      if (ev.getType() == "mousedown" && parentId == "charts") {
        /*
         * We cancel mousedown events on a chart ImageClickCell, so they don't cause the dialog box
         * to be redrawn.
         */
        mouseDownChartElement = parentElement;
        event.cancel();
      } else if ((ev.getType() == "mouseup") && mouseDownChartElement != null
          && parentElement != mouseDownChartElement) {
        /*
         * For a mouseup event which doesn't occur on the some chart ImageClickCell as the last
         * mousedown event, we hide the dialog, since it wasn't hidden before. If a mouseup event
         * does occur on the same chart ImageClickCell as the last mousedown event, that will be
         * recognized as a click, and be dealt with in the new ChartDialog's show() method.
         */
        hide(true);
      }
      super.onPreviewNativeEvent(event);
    }

    public void show(Widget widget) {
      Utils.displayInPopup(this, "Charts", widget, true, DialogPosition.Side);
      if (lastChartDialog != null) {
        lastChartDialog.hide();
      }
      lastChartDialog = this;
    }

    private class ChartCloseHandler implements CloseHandler<PopupPanel> {
      @Override
      public void onClose(CloseEvent<PopupPanel> event) {
        if (!startedOpeningNewCharts) {
          // If we've started opening a new ChartDialog, then it's already set highlightedRow to
          // some desired
          // value, which we don't want to overwrite.
          highlightedRow = -1;
          lastChartDialog = null;
        }
        grid.redrawRow(chartRow + grid.getPageStart());
      }
    }

    public void startedOpeningNewCharts() {
      startedOpeningNewCharts = true;
    }
  }

  private void displayCharts() {
    ExpressionRow dispRow = grid.getVisibleItem(highlightedRow);
    final String[] probes = dispRow.getAtomicProbes();
    final String title =
        SharedUtils.mkString(probes, "/") + ":" + SharedUtils.mkString(dispRow.getGeneSyms(), "/");
    ChartParameters params = charts.parameters(navigationTools.getValueType(), title);

    charts.make(params, probes, ag -> (new ChartDialog()).show(ag));
    Analytics.trackEvent(Analytics.CATEGORY_VISUALIZATION, Analytics.ACTION_DISPLAY_CHARTS);
  }

  public AssociationManager<ExpressionRow> associations() {
    return associations;
  }

  public void probesChanged(String[] probes) {
    chosenProbes = probes;
  }

  public ETMatrixManager matrix() {
    return matrix;
  }

  // ETMatrixManager.Delegate methods
  @Override
  public void onGettingExpressionFailed() {
    Window.alert("No data was available for the saved gene set (" + chosenProbes.length
        + " probes).\nThe view will switch to default selection. (Wrong species?)");
    delegate.onGettingExpressionFailed(this);
  }

  @Override
  public List<Group> chosenColumns() {
    return chosenColumns;
  }

  /**
   * Enable or disable the GUI
   */
  @Override
  public void setEnabled(boolean enabled) {
    navigationTools.setEnabled(enabled);
    analysisTools.setEnabled(navigationTools.getValueType(), enabled);
  }

  @Override
  public void getExpressions() {
    getExpressions(false);    
  }

  @Override
  public ETMatrixManager.SortOrder computeSortParams() {
    ColumnSortList csl = grid.getColumnSortList();
    sortAsc = false;
    sortKey = new SortKey.MatrixColumn(0);
    if (csl.size() > 0) {
      Column<?, ?> col = csl.get(0).getColumn();
      if (col instanceof MatrixSortable) {
        MatrixSortable ec = (MatrixSortable) col;
        return new ETMatrixManager.SortOrder(ec.sortKey(), csl.get(0).isAscending());
      } else {
        Window.alert("Sorting for this column is not implemented yet.");
      }
    }
    // Default sort order, sort descending by first column
    return new ETMatrixManager.SortOrder(new SortKey.MatrixColumn(0), false);
  }

  @Override
  public void onGetRows() {
    highlightedRow = -1;
    associations.getAllAssociations();
    delegate.afterGetRows(ExpressionTable.this);
    storeSortInfo();
  }

  @Override
  public void onSetRowCount(int visibleRows) {
    logger.info("Set visible row count: " + visibleRows);
    grid.setVisibleRangeAndClearData(new Range(0, visibleRows), true);
  }

  @Override
  public void onApplyColumnFilter() {
    delegate.onApplyColumnFilter();
  }

  // ETColumns.Delegate methods
  @Override
  public TableStyle style() {
    return style;
  }

  @Override
  public void onToolCellClickedForProbe(String probe) {
    highlightedRow = SharedUtils.indexOf(matrix.displayedProbes(), probe);
    if (lastChartDialog != null) {
      lastChartDialog.startedOpeningNewCharts();
    }
    grid.redrawRow(highlightedRow + grid.getPageStart());
    Utils.ensureVisualisationAndThen(() -> displayCharts());
  }

  // ETHeaderBuilder.Delegate methods
  @Override
  public List<String> columnSections() {
    return columnSections;
  }

  @Override
  public int columnCountForSection(String sectionName) {
    return sectionColumnCount.get(sectionName);
  }

  @Override
  public boolean displayPColumns() {
    return displayPColumns;
  }

  @Override
  public ManagedMatrixInfo matrixInfo() {
    return matrix.info();
  }

  // NavigationTools delegate methods
  @Override
  public void setPValueDisplay(boolean newState) {
    if (newState) {
      if (matrix.hasPValueColumns()) {
        setDisplayPColumns(newState);
        setupColumns();
      } else {
        Window.alert("Precomputed p-values are only available for sample groups "
            + " in fold-change mode, consisting of a single time and dose.\n"
            + "If you wish to compare two columns, use "
            + "\"Compare two sample groups\" in the tools menu.");
        setDisplayPColumns(false);
      }
    } else {
      // Generate list of p-value columns that are currently being filtered on
      List<ExpressionColumn> pValueFilteredColumns = new ArrayList<ExpressionColumn>();
      for (int i = 0; i < grid.getColumnCount(); i++) {
        Column<ExpressionRow, ?> column = grid.getColumn(i);
        if (column instanceof ExpressionColumn) {
          ExpressionColumn eColumn = (ExpressionColumn) column;
          if (matrixInfo().isPValueColumn(eColumn.matrixColumn())
              && eColumn.columnInfo().filterActive()) {
            pValueFilteredColumns.add(eColumn);
          }
        }
      }
      if (pValueFilteredColumns.size() == 0) {
        // If we're not filtering on any p-value columns, proceed normally
        setDisplayPColumns(newState);
        setupColumns();
      } else {
        // If we are filtering on p-value columns, get user confirmation before clearing
        // filters based on p-value columns
        if (Window.confirm(
            "Hiding p-value columns will undo all filtering based on " + "p-value columns.")) {
          int[] columnIndices =
              pValueFilteredColumns.stream().mapToInt(col -> col.matrixColumn()).toArray();
          matrix.clearColumnFilters(columnIndices);
          setDisplayPColumns(newState);
        } else {
          navigationTools.pValueCheck.setValue(true);
        }
      }
    }
  }

  @Override
  public void navigationToolsValueTypeChanged() {
    matrix().removeTests();
    getExpressions();
  }

  // AssociationManager.TableDelegate methods
  @Override
  public SampleClass chosenSampleClass() {
    return chosenSampleClass;
  }

  @Override
  public String[] displayedAtomicProbes() {
    return matrix.displayedAtomicProbes();
  }

  @Override
  public String[] atomicProbesForRow(ExpressionRow row) {
    return row.getAtomicProbes();
  }

  @Override
  public String[] geneIdsForRow(ExpressionRow row) {
    return row.getGeneIds();
  }
}
