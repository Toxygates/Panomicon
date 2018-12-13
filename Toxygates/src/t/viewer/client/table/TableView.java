package t.viewer.client.table;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.UIFactory;
import otg.viewer.client.components.ImportingScreen;
import otg.viewer.client.components.ScreenManager;
import otg.viewer.client.screen.data.HeatmapViewer;
import otg.viewer.client.screen.data.MirnaSourceDialog;
import otg.viewer.client.screen.groupdef.ColumnScreen;
import t.common.shared.GroupUtils;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.components.DataView;
import t.viewer.client.components.TickMenuItem;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.*;
import t.viewer.shared.mirna.MirnaSource;

/**
 * A DataView based on a single ExpressionTable.
 */
public class TableView extends DataView implements ExpressionTable.Delegate,
    ETMatrixManager.Loader, AssociationManager.ViewDelegate<ExpressionRow>, 
    MirnaSourceDialog.Delegate {

  public static enum ViewType {
    Single, Dual;    
  }
  
  protected ExpressionTable expressionTable;
  
  protected ImportingScreen screen;
  protected AppInfo appInfo;
  protected ScreenManager manager;
  protected UIFactory factory;
  protected Logger logger;
  protected MatrixServiceAsync matrixService;
  
  public TableView(ImportingScreen screen,
                   String mainTableTitle, 
                   boolean mainTableSelectable) {
    this.screen = screen;
    this.appInfo = screen.appInfo();
    this.manager = screen.manager();
    this.matrixService = manager.matrixService();
    this.factory = manager.factory();
    this.logger = Logger.getLogger("tableView");
    this.expressionTable = makeExpressionTable(mainTableTitle, mainTableSelectable);
    expressionTable.setDisplayPColumns(false);
    expressionTable.loadColumnVisibility();
    initWidget(content());   
    setupMenus();
    
    fetchAssociations();

    addToolbar(expressionTable.analysisTools());
  }
  
  public ViewType type() {
    return ViewType.Single;
  }
  
  @Override
  public void columnsChanged(List<Group> columns) {
    super.columnsChanged(columns);
    expressionTable.columnsChanged(columns);
  }

  @Override
  public void probesChanged(String[] probes) {
    super.probesChanged(probes);
    expressionTable.probesChanged(probes);
  }

  @Override
  public ValueType chosenValueType() {
    return expressionTable.getValueType();
  }
  
  protected Widget content() {
    ResizeLayoutPanel rlp = new ResizeLayoutPanel();
    rlp.setWidth("100%");    
    rlp.add(expressionTable);
    return rlp;
  }

  protected void setupMenus() {
    
    MenuItem mntmDownloadCsv =
        new MenuItem("Download CSV (grouped samples)...", false, () -> {          
            expressionTable.downloadCSV(false);
            Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_GROUPED_SAMPLES);
        });
    fileMenu.addItem(mntmDownloadCsv);
    mntmDownloadCsv = new MenuItem("Download CSV (individual samples)...", false, () -> {      
        expressionTable.downloadCSV(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
            Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_INDIVIDUAL_SAMPLES);      
    });
    fileMenu.addItem(mntmDownloadCsv);    
    
    MenuItem mColumns = new MenuItem("View", false, expressionTable.createColumnVisibilityMenu());
    addTopLevelMenu(mColumns);
    
    // TODO: this is effectively a tick menu item without the tick.
    // It would be nice to display the tick graphic, but then the textual alignment
    // of the other items on the menu becomes odd.
 
    addAnalysisMenuItem(new TickMenuItem("Compare two sample groups", false, false) {
      @Override
      public void stateChange(boolean newState) {
        if (newState) {
          screen.showToolbar(expressionTable.analysisTools());
        } else {
          screen.hideToolbar(expressionTable.analysisTools());
        }
      }
      }.menuItem());
    
    addAnalysisMenuItem(new MenuItem("Select MiRNA sources...", () -> {      
      showMirnaSourceDialog();
    }));

    if (factory.hasHeatMapMenu()) {
      MenuItem heatMapMenu = new MenuItem("Show heat map", () -> makeHeatMap());        
      addAnalysisMenuItem(heatMapMenu);
    }
  }
  
  protected void showMirnaSourceDialog() {
    MirnaSource[] sources = appInfo.mirnaSources();
    new MirnaSourceDialog(screen, this, manager.probeService(), sources,
        screen.getStorage().mirnaSourcesStorage.getIgnoringException().toArray(new MirnaSource[0]))
        .display("Choose miRNA sources", DialogPosition.Center);
  }

  protected void makeHeatMap() {
    HeatmapViewer.show(screen, this, expressionTable.getValueType());
  }
  
  protected static final String defaultMatrix = "DEFAULT";
  
  protected String mainMatrixId() {
    return defaultMatrix;
  }
  
  protected ExpressionTable makeExpressionTable(String mainTableTitle, 
                                                boolean mainTableSelectable) {
    TableFlags flags = new TableFlags(mainMatrixId(),
        true, true, NavigationTools.INIT_PAGE_SIZE,
        mainTableTitle, mainTableSelectable,
        false);
    
    return new ExpressionTable(screen, flags, TableStyle.getStyle("default"),
        this, this, this) {
    };
  }  
  
  protected void afterGetRows() { }
  
  //TODO hook to be overridden - try to remove this
  protected void onGettingExpressionFailed() { }
  
  @Override
  public void reloadDataIfNeeded() {
    logger.info("chosenProbes: " + chosenProbes.length + " lastProbes: "
        + (lastProbes == null ? "null" : "" + lastProbes.length));

    if (chosenColumns.size() == 0) {
      Window.alert("Please define sample groups to see data.");
      screen.manager().attemptProceed(ColumnScreen.key);
      return;
    }
    
    // Attempt to avoid reloading the data
    if (lastColumns == null || !chosenColumns.equals(lastColumns)) {
      logger.info("Data reloading needed");
      Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_VIEW_DATA);
      expressionTable.setStyle(styleForColumns(chosenColumns));
      expressionTable.getExpressions();
      onReloadData();
    } else if (!Arrays.equals(chosenProbes, lastProbes)) {
      logger.info("Only refiltering needed");
      expressionTable.matrix().refilterData(chosenProbes);
      onReloadData();
    }

    lastProbes = chosenProbes;
    lastColumns = chosenColumns;
  }
  
  /**
   * Called when a reload or refilter of the main table happens. To be overridden
   * by subclasses that need to react in that case.
   */
  protected void onReloadData() {
  }

  @Override
  public void loadInitialMatrix(ValueType valueType, 
		  int initPageSize, List<ColumnFilter> initFilters) {
    matrixService.loadMatrix(defaultMatrix, chosenColumns, chosenProbes, 
      valueType, initFilters, 
      new AsyncCallback<ManagedMatrixInfo>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Unable to load dataset");
          logger.log(Level.SEVERE, "Unable to load dataset", caught);
        }

        @Override
        public void onSuccess(ManagedMatrixInfo result) {
            expressionTable.matrix().setInitialMatrix(result);
        }
      });
  }
  
  protected TableStyle styleForColumns(List<Group> columns) {
    boolean foundMirna = false;
    boolean foundNonMirna = false;    
    for (Group g: chosenColumns) {
      if (isMirnaGroup(g)) {      
        foundMirna = true;
      } else {
        foundNonMirna = true;
      }
    }
    
    TableStyle r; 
    if (foundMirna && ! foundNonMirna) {
      r = TableStyle.getStyle("mirna");
    } else {
     r = TableStyle.getStyle("default");
    }
    logger.info("Use table style: " + r);
    return r;    
  }
  
  protected static boolean isMirnaGroup(Group g) {
    return "miRNA".equals(GroupUtils.groupType(g));
  }
  
  public ExpressionTable expressionTable() { return expressionTable; }
  
  // MirnaSourceDialog.Delegate method
  @Override
  public void mirnaSourceDialogMirnaSourcesChanged(MirnaSource[] mirnaSources) {
    screen.getStorage().mirnaSourcesStorage.store(Arrays.asList(mirnaSources));
    fetchAssociations();
  }

  public void afterMirnaSourcesUpdated(MirnaSource[] mirnaSources) {
     expressionTable.associations().getAssociations();
  };
  
  public void fetchAssociations() {
    MirnaSource[] mirnaSources = 
        screen.getStorage().mirnaSourcesStorage.getIgnoringException().toArray(new MirnaSource[0]);
    if (mirnaSources != null) {
      manager.networkService().setMirnaSources(mirnaSources, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Unable to set miRNA sources.");
        }

        @Override
        public void onSuccess(Void result) {
          afterMirnaSourcesUpdated(mirnaSources);
        }
      });
    }
  }
  
  /**
   * The probes currently contained in the current matrix, up to a limit.
   */
  @Override
  public String[] displayedAtomicProbes() {
    String[] r = expressionTable.matrixInfo().getAtomicProbes();
    if (r.length < expressionTable.matrixInfo().numRows()) {
      Window.alert("Too many genes. Only the first " + r.length + " genes will be used.");
    }
    return r;
  }

  @Override
  public Widget tools() {
    return expressionTable.tools();
  }

  // ExpressionTable.Delegate methods
  @Override
  public void onGettingExpressionFailed(ExpressionTable table) {
    // If a non-loadable gene list was specified, we try with the blank list
    // (all probes for the species)
    if (chosenProbes.length > 0) {
      TableView.this.probesChanged(new String[0]);
      TableView.this.onGettingExpressionFailed();
      reloadDataIfNeeded();
    }
    displayInfo("Data loading failed.");
  }

  @Override
  public void afterGetRows(ExpressionTable table) {
    afterGetRows();
  }

  @Override
  public void onApplyColumnFilter() {
  }

  // AssociationManager.ViewDelegate methods
  @Override
  public void associationsUpdated(AssociationManager<ExpressionRow> associations, Association[] result) {
    TableView.this.associationsUpdated(result);
  }

//  @Override
//  public void beforeGetAssociations(AssociationManager<ExpressionRow> associations) {
//    beforeGetAssociations();
//  }
}
