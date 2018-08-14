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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.*;
import com.google.gwt.dom.builder.shared.SpanBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;

import otgviewer.client.charts.AdjustableGrid;
import otgviewer.client.charts.Charts;
import otgviewer.client.charts.Charts.AChartAcceptor;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.client.ImageClickCell;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.FilterEditor;
import t.viewer.client.rpc.MatrixServiceAsync;
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
 * (association) columns are handled by the AssociationTable superclass.
 * 
 * @author johan
 *
 */
public class ExpressionTable extends AssociationTable<ExpressionRow> {

  private final String COLUMN_WIDTH = "10em";

  private Screen screen;
  private KCAsyncProvider asyncProvider = new KCAsyncProvider();
  
  private NavigationTools tools;
  private AnalysisTools analysisTools;

  private final MatrixServiceAsync matrixService;
  private final t.common.client.Resources resources;

  protected boolean displayPColumns = true;
  protected SortKey sortKey;
  protected boolean sortAsc;

  private boolean withPValueOption;

  // For Analytics: we count every matrix load other than the first as a gene set change
  private boolean firstMatrixLoad = true;


  /**
   * Names of the probes currently displayed
   */
  private String[] displayedAtomicProbes = new String[0];
  /**
   * Names of the (potentially merged) probes being displayed
   */
  private String[] displayedProbes = new String[0];

  private boolean loadedData = false;
  public ManagedMatrixInfo matrixInfo = null;
  private List<ColumnFilter> lastColumnFilters = new ArrayList<ColumnFilter>();
  
  private Sample[] chartBarcodes = null;

  private DialogBox filterDialog = null;

  private final Logger logger = SharedUtils.getLogger("expressionTable");

  protected ValueType chosenValueType;
  private String matrixId;

  interface MatrixLoader {
    /**
     * Perform an initial matrix load.
     * The method should load data and then call setInitialMatrix.
     * @param valueType
     * @param initFilters
     */
    void loadInitialMatrix(ValueType valueType, List<ColumnFilter> initFilters);
  }
  
  protected class HeaderBuilder extends DefaultHeaderOrFooterBuilder<ExpressionRow> {
    AbstractCellTable.Style style;

    public HeaderBuilder(AbstractCellTable<ExpressionRow> table) {
      super(table, false);
      style = getTable().getResources().style();
    }

    private void buildGroupHeader(TableRowBuilder rowBuilder, Group group, int columnCount,
        String styleNames) {
      SpanBuilder spanBuilder = rowBuilder.startTH().colSpan(columnCount)
          .className(style.header() + " majorHeader " + styleNames).startSpan();
      DataSchema schema = screen.manager().schema();
      spanBuilder.title(group.tooltipText(schema)).text(group.getName()).endSpan();
      rowBuilder.endTH();
    }

    private void buildBlankHeader(TableRowBuilder rowBuilder, int columnCount) {
      rowBuilder.startTH().colSpan(columnCount).endTH();
    }

    @Override
    protected boolean buildHeaderOrFooterImpl() {
      if (columnSections.size() > 0) {
        TableRowBuilder rowBuilder = startRow();
        for (int i = 0; i < columnSections.size(); i++) {
          String sectionName = columnSections.get(i);
          int numSectionColumns = sectionColumnCount.get(sectionName);
          if (numSectionColumns > 0 && matrixInfo != null) {
            if (sectionName == "data") {
              int groupColumnCount = 1;
              Group group = matrixInfo.columnGroup(0);
              // Iterate through data columns, and build a group header whenever
              // we switch groups, and also at the end of the iteration.
              boolean first = true;
              for (int j = 1; j < matrixInfo.numDataColumns(); j++) {
                if (displayPColumns || !matrixInfo.isPValueColumn(j)) {
                  Group nextGroup = matrixInfo.columnGroup(j);
                  if (group != nextGroup) {
                    String borderStyle = first ? "darkBorderLeft" : "whiteBorderLeft";
                    String groupStyle = group.getStyleName() + "-background";
                    buildGroupHeader(rowBuilder, group, groupColumnCount,
                        borderStyle + " " + groupStyle);
                    first = false;
                    groupColumnCount = 0;
                  }
                  groupColumnCount++;
                  group = nextGroup;
                }
              }
              String groupStyle = group.getStyleName() + "-background";
              buildGroupHeader(rowBuilder, group, groupColumnCount,
                  "whiteBorderLeft " + groupStyle);
            } else {
              buildBlankHeader(rowBuilder, numSectionColumns);
            }
          }
        }
      }
      return super.buildHeaderOrFooterImpl();
    }
  }

  protected AbstractSelectionModel<ExpressionRow> selectionModel;
  
  protected final int initPageSize;
  protected MatrixLoader loader;
  
  public ExpressionTable(Screen _screen, TableFlags flags,
      TableStyle style,
      MatrixLoader loader) {
    super(_screen, style, flags);
    this.withPValueOption = flags.withPValueOption;
    this.matrixService = _screen.manager().matrixService();
    this.resources = _screen.manager().resources();
    this.matrixId = flags.matrixId;
    this.initPageSize = flags.initPageSize;
    this.loader = loader;
    screen = _screen;

    grid.setHeaderBuilder(new HeaderBuilder(grid));
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

    tools = makeTools(flags.withPager);
    analysisTools = new AnalysisTools(this);

    setEnabled(false);
  }
  
  public AbstractSelectionModel<ExpressionRow> selectionModel() {
    return selectionModel;
  }

  private void logMatrixInfo(String msg) {
    logger.info("Matrix " + matrixId + ":" + msg);
  }
  
  private void logMatrix(Level level, String msg, Throwable throwable) {
    logger.log(level, "Matrix " + matrixId + ":" + msg, throwable);
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
  
  public ManagedMatrixInfo currentMatrixInfo() {
    return matrixInfo;
  }

  /**
   * Enable or disable the GUI
   * 
   * @param enabled
   */
  private void setEnabled(boolean enabled) {
    tools.setEnabled(enabled);
    analysisTools.setEnabled(chosenValueType, enabled);     
  }

  /**
   * The main (navigation) tool panel
   */
  private NavigationTools makeTools(boolean withPager) {
    //TODO use withPager
    NavigationTools r = new NavigationTools(this, grid, withPValueOption) {
      @Override
      void onPValueChange(boolean newState) {
        if (newState && ! hasPValueColumns()) {
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
    };
    chosenValueType = r.getValueType();
    return r;
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

  void removeTests() {    
    matrixService.removeSyntheticColumns(matrixId, new PendingAsyncCallback<ManagedMatrixInfo>(screen, 
        "There was an error removing the test columns.") {

      @Override
      public void handleSuccess(ManagedMatrixInfo result) {
        matrixInfo = result; // no need to do the full setMatrix
        setupColumns();
      }
    });    
  }

  void addTwoGroupSynthetic(final Synthetic.TwoGroupSynthetic synth, final String name) {
    final Group g1 = GroupUtils.findGroup(chosenColumns, analysisTools.selectedGroup1()).get();
    final Group g2 = GroupUtils.findGroup(chosenColumns, analysisTools.selectedGroup2()).get();
    synth.setGroups(g1, g2);

    matrixService.addSyntheticColumn(matrixId, synth,
        new PendingAsyncCallback<ManagedMatrixInfo>(screen, "Adding test column failed") {
          @Override
          public void handleSuccess(ManagedMatrixInfo r) {
            setMatrix(r);
            setupColumns();
          }
        });
  }

  public void downloadCSV(boolean individualSamples) {
    if (individualSamples && isMergeMode()) {
      Window.alert("Individual samples cannot be downloaded in orthologous mode.\n" +
          "Please inspect one group at a time.");
      return;
    }
    
    matrixService.prepareCSVDownload(matrixId, individualSamples, 
        new PendingAsyncCallback<String>(screen,
        "Unable to prepare the requested data for download.") {

      @Override
      public void handleSuccess(String url) {
        Utils.displayURL("Your download is ready.", "Download", url);
      }
    });
  }
  
  protected boolean hasPValueColumns() {
    for (int i = 0; i < matrixInfo.numDataColumns(); ++i) {
      if (matrixInfo.isPValueColumn(i)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void setupColumns() {
    super.setupColumns();
    TextCell tc = new TextCell();

    int oldSortIndex = (oldSortInfo != null ? 
        ((ExpressionColumn) oldSortInfo.getColumn()).matrixColumn() : -1);

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
    if (shouldFilterClick && matrixInfo != null && matrixInfo.numRows() > 0) {
      // Identify the column that was filtered.
      int col = columnAt(x);
      Column<ExpressionRow, ?> clickedCol = grid.getColumn(col);
      if (clickedCol instanceof ExpressionColumn) {
        ExpressionColumn ec = (ExpressionColumn) clickedCol;
        editColumnFilter(ec.matrixColumn());
      } else if (clickedCol instanceof AssociationTable.AssociationColumn) {
        @SuppressWarnings("unchecked")
        AssociationColumn ac = (AssociationColumn) clickedCol;
        displayColumnSummary(ac);
      }
    }
    // If we return true, the click will not be passed on to the other widgets
    return shouldFilterClick;
  }
  
  // /**
  // * Display a summary of a column.
  // */
  // private void columnSummary(AssociationTable<ExpressionRow>.AssociationColumn col) {
  // AssociationSummary<ExpressionRow> summary =
  // new AssociationSummary<ExpressionRow>(col, grid.getVisibleItems());
  // StringArrayTable.displayDialog(summary.getTable(), col.getAssociation().title() + " summary",
  // 500, 500);
  // }
  
  protected void editColumnFilter(int column) {
    ColumnFilter filt = matrixInfo.columnFilter(column);    
    FilterEditor fe =
        new FilterEditor(matrixInfo.columnName(column), column, filt) {

          @Override
          protected void onChange(ColumnFilter newVal) {
            applyColumnFilter(editColumn, newVal);
          }
        };
    filterDialog = Utils.displayInPopup("Edit filter", fe, DialogPosition.Center);
  }

  protected void applyColumnFilter(final int column, 
      final @Nullable ColumnFilter filter) {
    setEnabled(false);
    matrixService.setColumnFilter(matrixId,
        column, filter, new AsyncCallback<ManagedMatrixInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("An error occurred when the column filter was changed.");
        filterDialog.setVisible(false);
        setEnabled(true);
      }

      @Override
      public void onSuccess(ManagedMatrixInfo result) {
        if (result.numRows() == 0 && filter.active()) {
          Window.alert("No rows match the selected filter. The filter will be reset.");
          applyColumnFilter(column, filter.asInactive());
        } else {
          setMatrix(result);
          setupColumns();
          filterDialog.setVisible(false);
        }
      }
    });
  }

  protected @Nullable String probeLink(String identifier) {
    return null;
  }

  private String mkAssociationList(String[] values) {
    return SharedUtils.mkString("<div class=\"associationValue\">", values, "</div> ");
  }

  @Override
  protected List<HideableColumn<ExpressionRow, ?>> initHideableColumns(DataSchema schema) {
    SafeHtmlCell htmlCell = new SafeHtmlCell();
    List<HideableColumn<ExpressionRow, ?>> r = new ArrayList<HideableColumn<ExpressionRow, ?>>();

    r.add(new LinkingColumn<ExpressionRow>(htmlCell, "Gene ID", 
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

    r.add(new HTMLHideableColumn<ExpressionRow>(htmlCell, "Gene Symbol",
        StandardColumns.GeneSym, style) {        
      @Override
      protected String getHtml(ExpressionRow er) {
        return mkAssociationList(er.getGeneSyms());
      }

    });

    r.add(new HTMLHideableColumn<ExpressionRow>(htmlCell, "Probe Title",
        StandardColumns.ProbeTitle, style) {        
      @Override
      protected String getHtml(ExpressionRow er) {
        return mkAssociationList(er.getAtomicProbeTitles());
      }
    });

    r.add(new LinkingColumn<ExpressionRow>(htmlCell, "Probe", 
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
    r.addAll(super.initHideableColumns(schema));

    return r;
  }

  /**
   * The list of atomic probes currently on screen.
   */
  @Override
  public String[] displayedAtomicProbes() {
    return displayedAtomicProbes;
  }

  @Override
  protected String probeForRow(ExpressionRow row) {
    return row.getProbe();
  }

  @Override
  protected String[] atomicProbesForRow(ExpressionRow row) {
    return row.getAtomicProbes();
  }

  @Override
  protected String[] geneIdsForRow(ExpressionRow row) {
    return row.getGeneIds();
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
        loadedData = false;
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
          getAssociations();
          afterGetRows();
        } 
      }
    };

    @Override
    protected void onRangeChanged(HasData<ExpressionRow> display) {
      if (loadedData) {
        range = display.getVisibleRange();
        computeSortParams();
        if (range.getLength() > 0) {
          matrixService.matrixRows(matrixId, range.getStart(), range.getLength(), 
              sortKey, sortAsc, rowCallback);
        }
      }
    }
  }
  
  protected void afterGetRows() { }
  
  public List<ExpressionRow> getDisplayedRows() {
    return grid.getVisibleItems();
  }
  

  @Override
  public void columnsChanged(List<Group> columns) {
    HashSet<Group> oldColumns = new HashSet<Group>(chosenColumns);
    HashSet<Group> newColumns = new HashSet<Group>(columns);
    if (newColumns.equals(oldColumns) && newColumns.size() > 0) {
      logMatrixInfo("Ignoring column change signal");
      return;
    }

    super.columnsChanged(columns);

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
    loadedData = false;
    lastColumnFilters.clear();
    grid.getColumnSortList().clear();
    
    logMatrixInfo("Columns changed (" + columns.size() + ")");
  }

  /**
   * Filter data that has already been loaded
   */
  public void refilterData() {
    if (!loadedData) {
      logMatrixInfo("Request to refilter but data was not loaded");
      return;
    }
    setEnabled(false);
    asyncProvider.updateRowCount(0, false);
    // grid.setRowCount(0, false);
    logMatrixInfo("Refilter for " + chosenProbes.length + " probes");
    matrixService.selectProbes(matrixId, chosenProbes, dataUpdateCallback());
  }

  private AsyncCallback<ManagedMatrixInfo> dataUpdateCallback() {
    return new AsyncCallback<ManagedMatrixInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        logMatrix(Level.WARNING, "Exception in data update callback", caught);
        getExpressions(); // the user probably let the session
        // expire
      }

      @Override
      public void onSuccess(ManagedMatrixInfo result) {
        setMatrix(result);        
      }
    };
  }
  
  public void clearMatrix() {
    matrixInfo = null;    
    asyncProvider.updateRowCount(0, true);
    setEnabled(false);
  }

  protected void setMatrix(ManagedMatrixInfo matrix) {
    matrixInfo = matrix;
    lastColumnFilters = matrixInfo.columnFilters();
    asyncProvider.updateRowCount(matrix.numRows(), true);
    int initSize = NavigationTools.INIT_PAGE_SIZE;
    int displayRows = (matrix.numRows() > initSize) ? initSize : matrix.numRows();
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
  
  /**
   * Called when data is successfully loaded for the first time
   */
  private void onFirstLoad() {
    if (matrixInfo.isOrthologous()) {
      Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_VIEW_ORTHOLOGOUS_DATA);
    }
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

    logMatrixInfo("Begin loading data for " + chosenColumns.size() + " columns and "
        + chosenProbes.length + " probes");
    // load data
    loader.loadInitialMatrix(chosenValueType, initFilters);    
  }
  
  /**
   * To be called when a new matrix is set (as opposed to partial refinement or
   * modification of a previously loaded matrix).
   * @param matrix
   */
  void setInitialMatrix(ManagedMatrixInfo matrix) {
    if (matrix.numRows() > 0) {
      matrixInfo = matrix;
      if (!loadedData) {
        loadedData = true;
        onFirstLoad();
      }
      setupColumns();
      setMatrix(matrix);

      if (firstMatrixLoad) {
        firstMatrixLoad = false;
      } else {
        Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_CHANGE_GENE_SET);
      }

      logMatrixInfo("Data successfully loaded");
    } else {
      Window
          .alert("No data was available for the saved gene set (" + chosenProbes.length + " probes)." +
              "\nThe view will switch to default selection. (Wrong species?)");
      onGettingExpressionFailed();
    }  
  }
  
  protected void onGettingExpressionFailed() {}

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
      highlightedRow = SharedUtils.indexOf(displayedProbes, value);
      grid.redraw();
      Utils.ensureVisualisationAndThen(new Runnable() {
        @Override
        public void run() {
          displayCharts();
        }
      });
    }
  }
}
