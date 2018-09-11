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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;

import otgviewer.client.charts.AdjustableGrid;
import otgviewer.client.charts.Charts;
import otgviewer.client.charts.Charts.AChartAcceptor;
import otgviewer.client.components.Screen;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.shared.ManagedMatrixInfo;
import t.viewer.shared.SortKey;

/**
 * The main data display table. This class has many different functionalities.
 * (maybe still too many)
 * 
 * It requests microarray expression data dynamically, displays it, as well as
 * displaying additional dynamic data. It also provides functionality for chart
 * popups. It also has an interface for adding and removing t-tests and u-tests,
 * which can be hidden and displayed on demand.
 * 
 * Data is managed by an ETMatrixManager, which also includes an
 * AsyncDataProvider for the table, providing data for whatever rows the user
 * navigates to.
 * 
 * Hideable columns and clickable icons are handled by the RichTable superclass.
 * Dynamic (association) columns are handled by the AssociationManager helper
 * class.
 */
public class ExpressionTable extends RichTable<ExpressionRow>
    implements ETMatrixManager.Delegate, ETColumns.Delegate, ETHeaderBuilder.Delegate, 
    NavigationTools.Delegate, AssociationManager.TableDelegate<ExpressionRow> {

  private final String COLUMN_WIDTH = "10em";

  private Screen screen;
  private AssociationManager<ExpressionRow> associations;
  private ETMatrixManager matrix;
  private ETColumns columns;
  private Delegate delegate;

  protected final int initPageSize;
  private final Logger logger = SharedUtils.getLogger("expressionTable");
  
  private NavigationTools navigationTools;
  private AnalysisTools analysisTools;

  protected boolean displayPColumns = true;
  protected SortKey sortKey;
  protected boolean sortAsc;
  private boolean withPValueOption;

  protected List<Group> chosenColumns = new ArrayList<Group>();
  protected SampleClass chosenSampleClass;
  protected String[] chosenProbes = new String[0];

  protected ValueType chosenValueType;
  private Sample[] chartBarcodes = null;
  protected AbstractSelectionModel<ExpressionRow> selectionModel;

  public interface Delegate {
    void onGettingExpressionFailed(ExpressionTable table);
    void afterGetRows(ExpressionTable table);
  }

  public ExpressionTable(Screen _screen, TableFlags flags, TableStyle style, ETMatrixManager.Loader loader,
      Delegate delegate, AssociationManager.ViewDelegate<ExpressionRow> viewDelegate) {
    super(_screen, style, flags);
    screen = _screen;
    this.matrix = new ETMatrixManager(_screen, flags, this, loader, grid);
    this.associations = new AssociationManager<ExpressionRow>(screen, this, this, viewDelegate);
    this.columns = new ETColumns(this, _screen.manager().resources(), COLUMN_WIDTH);
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
      
      //To avoid confusion, we can avoid simultaneous selection and indication in the same table.
//      selectionModel.addSelectionChangeHandler(e ->
//        setIndicatedProbes(new String[] {}));     
    }
    grid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    // TODO use flags.withPager
    navigationTools = new NavigationTools(this, grid, withPValueOption, this);
    chosenValueType = navigationTools.getValueType();

    analysisTools = new AnalysisTools(this);

    hideableColumns = createHideableColumns(schema);

    setEnabled(false);
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
      Window.alert("Individual samples cannot be downloaded in orthologous mode.\n" +
          "Please inspect one group at a time.");
    } else {
      matrix.downloadCSV(individualSamples);
    }
  }
  
  @Override
  public void setupColumns() {
    super.setupColumns();
    TextCell tc = new TextCell();

    int oldSortIndex = (oldSortInfo != null ? 
        ((ExpressionColumn) oldSortInfo.getColumn()).matrixColumn() : -1);

    ManagedMatrixInfo matrixInfo = matrix.info();
    ColumnSortInfo newSort = null;
    Group previousGroup = null;
    for (int i = 0; i < matrixInfo.numDataColumns(); ++i) {
      if (displayPColumns || !matrixInfo.isPValueColumn(i)) {
        Column<ExpressionRow, String> valueCol = new ExpressionColumn(tc, i);
        if (i == oldSortIndex) {
          newSort = new ColumnSortInfo(valueCol, oldSortInfo.isAscending());
        }
        Group group = matrixInfo.columnGroup(i);
        String groupStyle = group == null ? "dataColumn" : group.getStyleName();
        String borderStyle = (group != previousGroup) ? "darkBorderLeft" : "lightBorderLeft";
        String style = groupStyle + " " + borderStyle;

        logger.info(matrixInfo.shortColumnName(i) + " " + 
            matrixInfo.columnFilter(i).threshold + " " + matrixInfo.columnFilter(i).active());
        ColumnInfo ci =
            new ColumnInfo(matrixInfo.shortColumnName(i), matrixInfo.columnHint(i), true, false,
                COLUMN_WIDTH, style, false, true, matrixInfo.columnFilter(i).active());
        ci.setHeaderStyleNames(style);

        previousGroup = group;
        addColumn(valueCol, "data", ci);
      }
    }
    
    ensureSection("synthetic");
    boolean first = true;
    for (int i = matrixInfo.numDataColumns(); i < matrixInfo.numColumns(); i++) {
      String borderStyle = first ? "darkBorderLeft" : "lightBorderLeft";
      first = false;
      ExpressionColumn synCol = columns.addSynthColumn(matrixInfo, i, borderStyle);
      if (i == oldSortIndex) {
        newSort = new ColumnSortInfo(synCol, oldSortInfo.isAscending());
      }
    }

    if (newSort != null && keepSortOnReload) {
      grid.getColumnSortList().push(newSort);
    }
  }

  // TODO: modify RichTable to directly call these methods from an interface to be 
  // implemented by ETColumns so we can get rid of this boilerplate
  @Override
  protected Column<ExpressionRow, String> toolColumn(Cell<String> cell) {
    return columns.toolColumn(cell);
  }

  @Override
  protected Cell<String> toolCell() {
    return columns.toolCell();
  }

  @Override
  protected Header<SafeHtml> getColumnHeader(ColumnInfo info) {
    return columns.getColumnHeader(info);
  }

  @Override
  protected boolean interceptGridClick(String target, int x, int y) {
    /*
     * To prevent unwanted interactions between the sorting system and the filtering system, we have
     * to intercept click events at this high level and choose whether to pass them on (non-filter
     * clicks) or not (filter clicks).
     */
    logger.info("Click target: " + target);
    boolean shouldFilterClick = target.equals(FilterCell.CLICK_ID);
    if (shouldFilterClick && matrix.info() != null && matrix.info().numRows() > 0) {
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
      matrix.logInfo("Ignoring column change signal");
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

    sampleClassChanged(SampleClass.intersection(allCs));
    logger.info("Set SC to: " + chosenSampleClass.toString());

    analysisTools.columnsChanged(columns);
    
    chartBarcodes = null;
    matrix.clear();
    matrix.lastColumnFilters().clear();
    grid.getColumnSortList().clear();
    
    matrix.logInfo("Columns changed (" + columns.size() + ")");
  }

  /**
   * Refetch rows as they are currently represented on the server side.
   * TODO: this should respect page size changes
   */
  public void refetchRows() {
    int initSize = NavigationTools.INIT_PAGE_SIZE;
    grid.setVisibleRangeAndClearData(new Range(0, initSize), true);
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

  /**
   * Load data (when there is nothing stored in our server side session)
   */
  public void getExpressions(boolean preserveFilters) {
    matrix.logInfo("Begin loading data for " + chosenColumns.size() + " columns and "
        + chosenProbes.length + " probes");
    matrix.getExpressions(preserveFilters, chosenValueType);
  }

  private void displayCharts() {
    final Charts cgf = new Charts(screen, chosenColumns);
    ExpressionRow dispRow = grid.getVisibleItem(highlightedRow);
    final String[] probes = dispRow.getAtomicProbes();
    cgf.makeRowCharts(screen, chartBarcodes, chosenValueType, probes, new AChartAcceptor() {
      @Override
      public void acceptCharts(final AdjustableGrid<?, ?> cg) {
        Utils.displayInPopup("Charts", cg, true, DialogPosition.Side);
      }

      @Override
      public void acceptBarcodes(Sample[] bcs) {
        chartBarcodes = bcs;
      }
    });
    Analytics.trackEvent(Analytics.CATEGORY_VISUALIZATION, Analytics.ACTION_DISPLAY_CHARTS);
  }
  
  public AssociationManager<ExpressionRow> associations() {
    return associations;
  }

  public void sampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;
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
    analysisTools.setEnabled(chosenValueType, enabled);
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
        MatrixSortable ec = (MatrixSortable) csl.get(0).getColumn();
        return new ETMatrixManager.SortOrder(ec.sortKey(), csl.get(0).isAscending());
      } else {
        Window.alert("Sorting for this column is not implemented yet.");
      }
    }
    return null;
  }

  @Override
  public void onGetRows() {
    highlightedRow = -1;
    associations.getAssociations();
    delegate.afterGetRows(ExpressionTable.this);
  }

  @Override
  public void onSetRowCount(int numRows) {
    grid.setVisibleRangeAndClearData(new Range(0, numRows), true);
  }

  // ETColumns.Delegate methods
  @Override
  public TableStyle style() {
    return style;
  }

  @Override
  public void onToolCellClickedForProbe(String probe) {
    int oldHighlightedRow = highlightedRow;
    highlightedRow = SharedUtils.indexOf(matrix.displayedProbes(), probe);
    if (oldHighlightedRow > 0) {
      grid.redrawRow(oldHighlightedRow);
    }
    grid.redrawRow(highlightedRow);
    Utils.ensureVisualisationAndThen(new Runnable() {
      @Override
      public void run() {
        displayCharts();
      }
    });
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

  // NavigationTools delegate method
  @Override
  public void setPValueDisplay(boolean newState) {
    if (newState && !matrix.hasPValueColumns()) {
      Window.alert("Precomputed p-values are only available for sample groups "
          + " in fold-change mode, consisting of a single time and dose.\n"
          + "If you wish to compare two columns, use "
          + "\"Compare two sample groups\" in the tools menu.");
      setDisplayPColumns(false);
    } else {
      setDisplayPColumns(newState);
      setupColumns();
    }
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
