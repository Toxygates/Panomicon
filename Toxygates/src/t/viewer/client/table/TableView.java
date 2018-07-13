package t.viewer.client.table;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.*;
import otgviewer.client.components.*;
import otgviewer.client.dialog.MirnaSourceDialog;
import t.common.shared.GroupUtils;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.PersistedState;
import t.viewer.client.components.DataView;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.table.RichTable.HideableColumn;
import t.viewer.shared.*;
import t.viewer.shared.mirna.MirnaSource;

/**
 * A DataView based on a single ExpressionTable.
 */
public class TableView extends DataView {

  public static enum ViewType {
    Single, Dual;    
  }
  
  protected ExpressionTable expressionTable;
  
  protected ImportingScreen screen;
  protected AppInfo appInfo;
  protected ScreenManager manager;
  protected UIFactory factory;
  private Map<String, TickMenuItem> tickMenuItems = new HashMap<String, TickMenuItem>();
  protected Logger logger;
  
  public TableView(ImportingScreen screen,
                   String mainTableTitle, 
                   boolean mainTableSelectable) {
    this.screen = screen;
    this.appInfo = screen.appInfo();
    this.manager = screen.manager();
    this.factory = manager.factory();
    this.logger = Logger.getLogger("tableView");
    this.expressionTable = makeExpressionTable(mainTableTitle, mainTableSelectable);
    expressionTable.setDisplayPColumns(false);
    initWidget(content());   
    setupMenus();
    
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
  public void sampleClassChanged(SampleClass sc) {
    super.sampleClassChanged(sc);
    expressionTable.sampleClassChanged(sc);
  }

  @Override
  public void probesChanged(String[] probes) {
    super.probesChanged(probes);
    expressionTable.probesChanged(probes);
  }

  @Override
  public ValueType chosenValueType() {
    return expressionTable.chosenValueType;
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
    
    MenuBar menuBar = new MenuBar(true);
    //TODO store the TickMenuItem in HideableColumn so that the state can be synchronised
    for (final HideableColumn<ExpressionRow, ?> c : expressionTable.getHideableColumns()) {
        final String title = c.columnInfo().title();
   
      tickMenuItems.put(title, 
        //Automatically added to the menuBar
      new TickMenuItem(menuBar, title, c.visible()) {
        @Override
        public void stateChange(boolean newState) {
          expressionTable.setVisible(c, newState);
          expressionTable.getAssociations();
          if (newState) {
              Analytics.trackEvent(Analytics.CATEGORY_TABLE, 
                      Analytics.ACTION_DISPLAY_OPTIONAL_COLUMN, title);
          }
        }
      });
    }

    MenuItem mColumns = new MenuItem("View", false, menuBar);
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
      MirnaSource[] sources = appInfo.mirnaSources();        
      new MirnaSourceDialog(screen, manager.probeService(), sources, 
        mirnaState).
        display("Choose miRNA sources", DialogPosition.Center);
    }));

    if (factory.hasHeatMapMenu()) {
      MenuItem heatMapMenu = new MenuItem("Show heat map", () -> makeHeatMap());        
      addAnalysisMenuItem(heatMapMenu);
    }
  }
  
  protected void makeHeatMap() {
    HeatmapViewer.show(screen, this, expressionTable.getValueType());
  }
  
  protected static final String defaultMatrix = "DEFAULT";
  
  protected ExpressionTable makeExpressionTable(String mainTableTitle, 
                                                boolean mainTableSelectable) {
    TableFlags flags = new TableFlags(defaultMatrix,
        true, true, NavigationTools.INIT_PAGE_SIZE,
        mainTableTitle, mainTableSelectable,
        false);
    
    return new ExpressionTable(screen, flags, TableStyle.getStyle("default")) {
      @Override
      protected void onGettingExpressionFailed() {
        super.onGettingExpressionFailed();
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
      protected void associationsUpdated(Association[] result) {
        TableView.this.associationsUpdated(result);
      }
      
      @Override
      public void getAssociations() {
        beforeGetAssociations();
        super.getAssociations();        
      }
      
      @Override
      protected void setMatrix(ManagedMatrixInfo matrix) {
        super.setMatrix(matrix);
        displayInfo("Successfully loaded " + matrix.numRows() + " probes");
      }
    };
  }  
  
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
      expressionTable.setStyle(styleForColumns(chosenColumns));
      expressionTable.getExpressions();      
    } else if (!Arrays.equals(chosenProbes, lastProbes)) {
      logger.info("Only refiltering is needed");
      expressionTable.refilterData();
    }

    lastProbes = chosenProbes;
    lastColumns = chosenColumns;
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
  
  @Override
  public ExpressionTable expressionTable() { return expressionTable; }

  protected PersistedState<MirnaSource[]> mirnaState = new PersistedState<MirnaSource[]>(
      "miRNASources", "mirnaSources") {
    @Override
    protected String doPack(MirnaSource[] state) {
      return Arrays.stream(state).map(ms -> ms.pack()).collect(Collectors.joining(":::"));
    }

    @Override
    protected MirnaSource[] doUnpack(String state) {
      String[] spl = state.split(":::");
      return Arrays.stream(spl).map(ms -> MirnaSource.unpack(ms)).
          filter(ms -> ms != null).toArray(MirnaSource[]::new);
    }

    @Override
    public void onValueChange(MirnaSource[] state) {
      if (state != null) {        
        manager.networkService().setMirnaSources(state, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Unable to set miRNA sources.");
          }

          @Override
          public void onSuccess(Void result) {
            expressionTable.getAssociations();
          }
        });
      }
    }
  };
  
  
  @Override
  public List<PersistedState<?>> getPersistedItems() {
    List<PersistedState<?>> r = super.getPersistedItems();
    r.addAll(expressionTable.getPersistedItems());
    r.add(mirnaState);
    return r;
  }
  
  @Override
  public void loadPersistedState() {
    super.loadPersistedState();
    for (String title: tickMenuItems.keySet()) {
      TickMenuItem mi = tickMenuItems.get(title);
      boolean state = expressionTable.persistedVisibility(title, mi.getState());
      mi.setState(state);
    }
  }
  
  @Override
  public String[] displayedAtomicProbes() {
    String[] r = expressionTable.currentMatrixInfo().getAtomicProbes();
    if (r.length < expressionTable.currentMatrixInfo().numRows()) {
      Window.alert("Too many genes. Only the first " + r.length + " genes will be used.");
    }
    return r;
  }

  @Override
  public Widget tools() {
    return expressionTable.tools();
  }  
  
  
}
