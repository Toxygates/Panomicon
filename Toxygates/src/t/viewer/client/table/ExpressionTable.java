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

import javax.annotation.Nullable;

import com.google.gwt.cell.client.*;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;

import otgviewer.client.charts.AdjustableGrid;
import otgviewer.client.charts.Charts;
import otgviewer.client.charts.Charts.AChartAcceptor;
import otgviewer.client.components.Screen;
import t.common.client.ImageClickCell;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.shared.*;

/**
 * The main data display table. This class has many different functionalities. (too many, should be
 * refactored into something like an MVC architecture, almost certainly)
 * 
 * It requests microarray expression data dynamically, displays it, as well as displaying additional
 * dynamic data. It also provides functionality for chart popups. It also has an interface for
 * adding and removing t-tests and u-tests, which can be hidden and displayed on demand.
 * 
 * Hideable columns and clickable icons are handled by the RichTable superclass. Dynamic
 * (association) columns are handled by the AssociationManager helper class.
 * 
 * @author johan
 *
 */
public class ExpressionTable extends RichTable<ExpressionRow>
    implements ETMatrixManager.Delegate, ETHeaderBuilder.Delegate, NavigationTools.Delegate,
    AssociationManager.TableDelegate<ExpressionRow> {

  private final String COLUMN_WIDTH = "10em";

  private Screen screen;
  private KCAsyncProvider asyncProvider = new KCAsyncProvider();
  private AssociationManager<ExpressionRow> associations;
  private ETMatrixManager matrix;
  private Delegate delegate;

  protected MatrixLoader loader;

  private final t.common.client.Resources resources;
  protected final int initPageSize;
  private final Logger logger = SharedUtils.getLogger("expressionTable");
  
  private NavigationTools tools;
  private AnalysisTools analysisTools;

  private boolean matrixDirty = false;
  protected boolean displayPColumns = true;
  protected SortKey sortKey;
  protected boolean sortAsc;
  private boolean withPValueOption;

  protected List<Group> chosenColumns = new ArrayList<Group>();
  protected SampleClass chosenSampleClass;
  protected String[] chosenProbes = new String[0];

  /**
   * Names of the probes currently displayed
   */
  private String[] displayedAtomicProbes = new String[0];
  /**
   * Names of the (potentially merged) probes being displayed
   */
  private String[] displayedProbes = new String[0];

  protected ValueType chosenValueType;
  private Sample[] chartBarcodes = null;
  private List<ColumnFilter> lastColumnFilters = new ArrayList<ColumnFilter>();
  protected AbstractSelectionModel<ExpressionRow> selectionModel;

  interface MatrixLoader {
    /**
     * Perform an initial matrix load.
     * The method should load data and then call setInitialMatrix.
     */
    void loadInitialMatrix(ValueType valueType, List<ColumnFilter> initFilters);
  }

  public interface Delegate {
    void onGettingExpressionFailed(ExpressionTable table);
    void afterGetRows(ExpressionTable table);
  }

  public ExpressionTable(Screen _screen, TableFlags flags, TableStyle style, MatrixLoader loader, 
      Delegate delegate, AssociationManager.ViewDelegate<ExpressionRow> viewDelegate) {
    super(_screen, style, flags);
    screen = _screen;
    this.associations = new AssociationManager<ExpressionRow>(screen, this, this, viewDelegate);
    this.delegate = delegate;
    this.withPValueOption = flags.withPValueOption;
    this.resources = _screen.manager().resources();
    this.initPageSize = flags.initPageSize;
    this.loader = loader;


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
    asyncProvider.addDataDisplay(grid);

    // TODO use flags.withPager
    tools = new NavigationTools(this, grid, withPValueOption, this);
    chosenValueType = tools.getValueType();

    analysisTools = new AnalysisTools(this);

    hideableColumns = createHideableColumns(schema);

    setEnabled(false);
  }
  
  private boolean matrixReady() {
    return !matrixDirty && matrix.loadedData();
  }

  public AbstractSelectionModel<ExpressionRow> selectionModel() {
    return selectionModel;
  }

  public void setStyle(TableStyle style) {
    this.style = style;    
  }

  protected boolean isMergeMode() {
    if (displayedProbes == null || displayedAtomicProbes == null) {
      return false;
    }
    return displayedProbes.length != displayedAtomicProbes.length;
  }
  
  public ValueType getValueType() {
    return tools.getValueType();    
  }

  public Widget tools() {
    return this.tools;
  }
  
  /**
   * Enable or disable the GUI
   */
  @Override
  public void setEnabled(boolean enabled) {
    tools.setEnabled(enabled);
    analysisTools.setEnabled(chosenValueType, enabled);     
  }

  public void setDisplayPColumns(boolean displayPColumns) {
    if (withPValueOption) {
      this.displayPColumns = displayPColumns;
      tools.setPValueState(displayPColumns);      
    }    
  }

  public Widget analysisTools() {
    return analysisTools;
  }

  public void downloadCSV(boolean individualSamples) {
    if (individualSamples && isMergeMode()) {
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
      ExpressionColumn synCol = addSynthColumn(matrixInfo, i, borderStyle);
      if (i == oldSortIndex) {
        newSort = new ColumnSortInfo(synCol, oldSortInfo.isAscending());
      }
    }

    if (newSort != null && keepSortOnReload) {
      grid.getColumnSortList().push(newSort);
    }
    
  }

  @Override
  protected Column<ExpressionRow, String> toolColumn(Cell<String> cell) {
    return new Column<ExpressionRow, String>(cell) {
      @Override
      public String getValue(ExpressionRow er) {
        if (er != null) {
          return er.getProbe();
        } else {
          return "";
        }
      }
    };
  }

  @Override
  protected Cell<String> toolCell() {
    return new ToolCell();
  }

  private ExpressionColumn addSynthColumn(ManagedMatrixInfo matrixInfo, int column, String borderStyle) {
    TextCell tc = new TextCell();    
    ExpressionColumn synCol = new ExpressionColumn(tc, column);
    
    ColumnInfo info = new ColumnInfo(matrixInfo.shortColumnName(column), 
      matrixInfo.columnHint(column), 
      true, false, COLUMN_WIDTH,
        "extraColumn " + borderStyle, false, true, 
        matrixInfo.columnFilter(column).active());
    info.setHeaderStyleNames(borderStyle);
    info.setDefaultSortAsc(true);
    addColumn(synCol, "synthetic", info);
    return synCol;
  }

  @Override
  protected Header<SafeHtml> getColumnHeader(ColumnInfo info) {
    Header<SafeHtml> superHeader = super.getColumnHeader(info);
    if (info.filterable()) {
      FilteringHeader header = new FilteringHeader(superHeader.getValue(), info.filterActive());
      header.setHeaderStyleNames(info.headerStyleNames());
      return header;
    } else {
      return superHeader;
    }
  }

  private void computeSortParams() {
    ColumnSortList csl = grid.getColumnSortList();
    sortAsc = false;
    sortKey = new SortKey.MatrixColumn(0);
    if (csl.size() > 0) {
      Column<?, ?> col = csl.get(0).getColumn();
      if (col instanceof MatrixSortable) {
        MatrixSortable ec = (MatrixSortable) csl.get(0).getColumn();
        sortKey = ec.sortKey();
        sortAsc = csl.get(0).isAscending();
      } else {
        Window.alert("Sorting for this column is not implemented yet.");
      }
    }
  }

  @Override
  protected boolean interceptGridClick(String target, int x, int y) {
    /**
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

  protected @Nullable String probeLink(String identifier) {
    return null;
  }

  private String mkAssociationList(String[] values) {
    return SharedUtils.mkString("<div class=\"associationValue\">", values, "</div> ");
  }

  @Override
  protected List<HideableColumn<ExpressionRow, ?>> createHideableColumns(DataSchema schema) {
    SafeHtmlCell htmlCell = new SafeHtmlCell();
    List<HideableColumn<ExpressionRow, ?>> columns = new ArrayList<HideableColumn<ExpressionRow, ?>>();

    columns.add(new LinkingColumn<ExpressionRow>(htmlCell, "Gene ID", 
        StandardColumns.GeneID, style) {        
      @Override
      protected String formLink(String value) {
        return AType.formGeneLink(value);
      }

      @Override
      protected Collection<AssociationValue> getLinkableValues(ExpressionRow er) {
        String[] geneIds = er.getGeneIds(); // basis for the URL
        String[] labels = er.getGeneIdLabels();
        List<AssociationValue> r = new ArrayList<AssociationValue>();
        for (int i = 0; i < geneIds.length; i++) {
          r.add(new AssociationValue(labels[i], geneIds[i], null));
        }
        return r;
      }
    });

    columns.add(new HTMLHideableColumn<ExpressionRow>(htmlCell, "Gene Symbol",
        StandardColumns.GeneSym, style) {        
      @Override
      protected String getHtml(ExpressionRow er) {
        return mkAssociationList(er.getGeneSyms());
      }

    });

    columns.add(new HTMLHideableColumn<ExpressionRow>(htmlCell, "Probe Title",
        StandardColumns.ProbeTitle, style) {        
      @Override
      protected String getHtml(ExpressionRow er) {
        return mkAssociationList(er.getAtomicProbeTitles());
      }
    });

    columns.add(new LinkingColumn<ExpressionRow>(htmlCell, "Probe", 
        StandardColumns.Probe, style) {        

      @Override
      protected String formLink(String value) {
        return probeLink(value);
      }

      @Override
      protected Collection<AssociationValue> getLinkableValues(ExpressionRow er) {
        List<AssociationValue> r = new LinkedList<AssociationValue>();
        for (String probe: er.getAtomicProbes()) {
          r.add(new AssociationValue(probe, probe, null));
        }        
        return r;
      }

    });

    // We want gene sym, probe title etc. to be before the association
    // columns going left to right
    columns.addAll(associations.createHideableColumns(schema));

    return columns;
  }

  /**
   * This class fetches data on demand when the user requests a different page. Data must first be
   * loaded with getExpressions.
   * 
   * @author johan
   *
   */
  class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
    private Range range;    
    AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
      
      private String errMsg() {
        String appName = screen.appInfo().applicationName();
        return "Unable to obtain data. If you have not used " + appName + " in a while, try reloading the page.";
      }
      @Override
      public void onFailure(Throwable caught) {        
        matrixDirty = true;
        Window.alert(errMsg());
      }

      @Override
      public void onSuccess(List<ExpressionRow> result) {
        if (result.size() > 0) {
          updateRowData(range.getStart(), result);
          displayedAtomicProbes = result.stream().
              flatMap(r -> Arrays.stream(r.getAtomicProbes())).
              toArray(String[]::new);
          displayedProbes = result.stream().map(r -> r.getProbe()).
              toArray(String[]::new);          
          highlightedRow = -1;
          associations.getAssociations();
          delegate.afterGetRows(ExpressionTable.this);
        } 
      }
    };

    @Override
    protected void onRangeChanged(HasData<ExpressionRow> display) {
      if (matrixReady()) {
        range = display.getVisibleRange();
        computeSortParams();
        if (range.getLength() > 0) {
          matrixService.matrixRows(matrix.id(), range.getStart(), range.getLength(), 
              sortKey, sortAsc, rowCallback);
        }
      }
    }
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

    // we set chosenSampleClass to the intersection of all the samples
    // in the groups here. Needed later for e.g. the associations() call.
    // TODO: this may need to be moved.
    // TODO: efficiency of this operation for 100's of samples
    List<SampleClass> allCs = new LinkedList<SampleClass>();
    for (Group g : columns) {
      allCs.addAll(SampleClassUtils.classes(Arrays.asList(g.getSamples())));
    }

    sampleClassChanged(SampleClass.intersection(allCs));
    logger.info("Set SC to: " + chosenSampleClass.toString());

    analysisTools.columnsChanged(columns);
    
    chartBarcodes = null;
    matrixDirty = true;
    lastColumnFilters.clear();
    grid.getColumnSortList().clear();
    
    matrix.logInfo("Columns changed (" + columns.size() + ")");
  }

  /**
   * Filter data that has already been loaded
   */
  public void refilterData() {
    if (!matrixReady()) {
      matrix.logInfo("Request to refilter but data was not loaded");
      return;
    }
    asyncProvider.updateRowCount(0, false);
    setEnabled(false);
    matrix.refilterData(chosenProbes);
  }
  
  public void clearMatrix() {
    matrix.clear();
    asyncProvider.updateRowCount(0, true);
    setEnabled(false);
  }

  @Override
  public void setRows(int numRows) {
    lastColumnFilters = matrix.columnFilters();
    asyncProvider.updateRowCount(numRows, true);
    int initSize = NavigationTools.INIT_PAGE_SIZE;
    int displayRows = (numRows > initSize) ? initSize : numRows;
    grid.setVisibleRangeAndClearData(new Range(0, displayRows), true);
    setEnabled(true);
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
  
  public void onSelectionChanged(String selectedProbe) {
    
  }
  
  public void setIndicatedProbes(Set<String> highlighted, boolean redraw) {
    logger.info(highlighted.size() + " rows are indicated");
    Set<String> oldIndicated = indicatedRows;
    indicatedRows = highlighted;
    
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

  public void getExpressions() {
    getExpressions(false);
  }
  
  /**
   * Load data (when there is nothing stored in our server side session)
   */
  public void getExpressions(boolean preserveFilters) {
    setEnabled(false);
    List<ColumnFilter> initFilters = preserveFilters ? lastColumnFilters : 
      new ArrayList<ColumnFilter>();    
    asyncProvider.updateRowCount(0, false);

    matrix.logInfo("Begin loading data for " + chosenColumns.size() + " columns and "
        + chosenProbes.length + " probes");
    loader.loadInitialMatrix(chosenValueType, initFilters);    
  }
  
  @Override
  public void onGettingExpressionFailed() {
    Window.alert("No data was available for the saved gene set (" + chosenProbes.length
        + " probes).\nThe view will switch to default selection. (Wrong species?)");
    delegate.onGettingExpressionFailed(this);
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
  
  /**
   * This cell displays an image that can be clicked to display charts.
   */
  class ToolCell extends ImageClickCell.StringImageClickCell {

    public ToolCell() {
      super(resources.chart(), "charts", false);
    }

    @Override
    public void onClick(final String value) {
      int oldHighlightedRow = highlightedRow;
      highlightedRow = SharedUtils.indexOf(displayedProbes, value);
      grid.redrawRow(oldHighlightedRow);
      grid.redrawRow(highlightedRow);
      Utils.ensureVisualisationAndThen(new Runnable() {
        @Override
        public void run() {
          displayCharts();
        }
      });
    }
  }

  public AssociationManager<ExpressionRow> associations() {
    return associations;
  }

  @Override
  public List<Group> chosenColumns() {
    return chosenColumns;
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
    return matrix.matrixInfo;
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
    return displayedAtomicProbes;
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
