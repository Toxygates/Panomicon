package t.viewer.client.table;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.NetworkMenu;
import otgviewer.client.components.ImportingScreen;
import otgviewer.client.components.PendingAsyncCallback;
import t.common.shared.AType;
import t.common.shared.GroupUtils;
import t.common.shared.sample.Group;
import t.viewer.client.Utils;
import t.viewer.client.network.*;
import t.viewer.client.rpc.NetworkServiceAsync;
import t.viewer.shared.Association;
import t.viewer.shared.network.Format;
import t.viewer.shared.network.Network;

/**
 * A DataView that displays an interaction network as two tables.
 */
public class DualTableView extends TableView implements NetworkMenu.Delegate, NetworkVisualizationDialog.Delegate {
  protected ExpressionTable sideExpressionTable;
  private NetworkMenu networkMenu;

  protected final static String sideMatrix = "SECONDARY";
  final static int MAX_SECONDARY_ROWS = Network.MAX_SIZE;
  private List<Network> _networks; // Don't access directly; see explanation below

  public static enum DualMode {
    Forward("mRNA", "miRNA", AType.MiRNA) {
      @Override
      void setVisibleColumns(ExpressionTable table) {
        table.setVisible(AType.MiRNA, true);
        table.setVisible(AType.MRNA, false);
      }
    },
    
    Reverse("miRNA", "mRNA", AType.MRNA) {
      @Override
      void setVisibleColumns(ExpressionTable table) {
        table.setVisible(AType.MiRNA, false);
        table.setVisible(AType.MRNA, true);
      }
    };
    
    public final String mainType, sideType;
    public final AType linkingType;

    DualMode(String mainType, String sideType, AType linkingType) {
      this.mainType = mainType;
      this.sideType = sideType;
      this.linkingType = linkingType;      
    }
    
    int sideTableWidth() { 
      if (sideType.equals("miRNA")) {
        return 550;
      } else {
        return 800;
      }
    }
    TableStyle mainStyle() { return TableStyle.getStyle(mainType); }
    TableStyle sideStyle() { return TableStyle.getStyle(sideType); }
    DualMode flip() { return (this == Forward) ? Reverse : Forward; }   
    
    void setVisibleColumns(ExpressionTable table) { }
  }
  
  /**
   * The preferred/default mode when two column types are available.
   */
  DualMode preferredDoubleMode = DualMode.Forward;
  
  /**
   * The current mode.
   */
  DualMode mode = DualMode.Forward;
  
  NetworkController controller;
  
  protected SplitLayoutPanel splitLayout;
  
  
  public DualTableView(ImportingScreen screen, String mainTableTitle) {
    super(screen, mainTableTitle, true);
    expressionTable.selectionModel().addSelectionChangeHandler(e -> {      
      network.onSourceSelectionChanged();      
    });
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e -> {   
      network.onDestSelectionChanged();   
    });
  }
  
  @Override
  public ViewType type() {
    return ViewType.Dual;
  }
  
  /*
   * Note: It's probably best/easiest to rebuild the layout completely
   * each time the chosen columns change
   */
  @Override
  protected Widget content() {    
    DualMode mode = DualMode.Forward;
    TableFlags flags =
        new TableFlags(sideMatrix, true, false, MAX_SECONDARY_ROWS, mode.sideType, true, true);

    sideExpressionTable = new ExpressionTable(screen, flags, mode.sideStyle());
    sideExpressionTable.addStyleName("sideExpressionTable");

    splitLayout = new SplitLayoutPanel();    
    splitLayout.setWidth("100%");
    splitLayout.addEast(sideExpressionTable, 550);
    splitLayout.add(expressionTable);    
    return splitLayout;
  }
  
  protected DualTableNetwork makeNetwork() {
    network = new DualTableNetwork(expressionTable, sideExpressionTable, 
        mode, MAX_SECONDARY_ROWS);
    controller = new NetworkController(network) {
      @Override
      public Map<String, Collection<String>> linkingMap() {
        return network.linkingMap();
      }    
    };
    return network;
  }
  
  protected DualTableNetwork network;

  protected void flipDualView() {    
    preferredDoubleMode = mode.flip();    
    List<Group> allColumns = new ArrayList<Group>(chosenColumns);
    allColumns.addAll(sideExpressionTable.chosenColumns());
    columnsChanged(allColumns);

    reloadDataIfNeeded();
  }
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    sideExpressionTable.clearMatrix();
  }
  
  //TODO refactor
  @Override
  protected void associationsUpdated(Association[] result) {
    super.associationsUpdated(result);
    network.extractSideTableProbes();
  }
  
  private Widget tools;
  @Override
  public Widget tools() {
    if (tools == null) {
      Button flipButton = new Button("Flip mRNA-microRNA", (ClickHandler) event -> flipDualView());
      tools = Utils.mkHorizontalPanel(true, flipButton);
    }
    return Utils.mkHorizontalPanel(true, super.tools(), tools);
  }
  
  @Override
  protected void setupMenus() {
    super.setupMenus();
    networkMenu = new NetworkMenu(this);
    topLevelMenus.add(networkMenu.menuItem());
  }
  
  //Initial title only - need the constant here since the field won't be initialised
//  @Override
//  protected String mainTableTitle() { return "mRNA"; }     
  
  protected boolean mainTableSelectable() { return true; }  
  
  protected List<Group> columnsOfType(List<Group> from, String type) {
    return from.stream().filter(g -> type.equals(GroupUtils.groupType(g))).
        collect(Collectors.toList());    
  }
  
  protected List<Group> columnsForMainTable(List<Group> from) {    
    List<Group> r = columnsOfType(from, mode.mainType);
    
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<Group> columnsForSideTable(List<Group> from) {
    return columnsOfType(from, mode.sideType);        
  }
  
  //TODO refactor
//  protected TableStyle mainTableStyle() {
//    return super.styleForColumns(columnsForMainTable(chosenColumns));
//  }

  //TODO refactor
//  protected TableStyle sideTableStyle() {
//    return super.styleForColumns(columnsForSideTable(chosenColumns));    
//  }
  
//  @Override
//  public void loadState(StorageParser p, DataSchema schema, AttributeSet attributes) {
//    //TODO this is a state management hack to force the columns to be fully re-initialised
//    //every time we show the screen.
//    chosenColumns = new ArrayList<Group>();
//    super.loadState(p, schema, attributes);
//  }
  
  @Override
  public void columnsChanged(List<Group> columns) {
    logger.info("Dual mode pick for " + columns.size() + " columns");
    mode = preferredDoubleMode;    
    
    expressionTable.setTitleHeader(mode.mainType);    
    expressionTable.setStyleAndApply(mode.mainStyle());
    
    makeNetwork();
    sideExpressionTable.setTitleHeader(mode.sideType);
    sideExpressionTable.setStyleAndApply(mode.sideStyle());

    super.columnsChanged(columnsForMainTable(columns));
    
    List<Group> sideColumns = columnsForSideTable(columns);
    if (sideExpressionTable != null && !sideColumns.isEmpty()) {    
      sideExpressionTable.columnsChanged(sideColumns);    
    }
    logger.info("Dual table mode: " + mode);

    splitLayout.setWidgetSize(sideExpressionTable, mode.sideTableWidth());
  }  
  
  @Override
  public void reloadDataIfNeeded() {       
    expressionTable.setAssociationAutoRefresh(false);
    mode.setVisibleColumns(expressionTable);      
    expressionTable.setAssociationAutoRefresh(true);

    super.reloadDataIfNeeded();
    sideExpressionTable.clearMatrix();
    sideExpressionTable.setIndicatedProbes(new HashSet<String>(), false);
    expressionTable.setIndicatedProbes(new HashSet<String>(), false);    
  }
  
  // NetworkMenu.Delegate methods
  @Override
  public void visualizeNetwork() {
    new NetworkVisualizationDialog(this, logger)
        .initWindow(controller.buildNetwork("miRNA-mRNA interactions", mode != DualMode.Forward));
  }

  @Override
  public void downloadNetwork(Format format) {
    NetworkServiceAsync networkService = screen.manager().networkService();
    Network network = controller.buildNetwork("miRNA-mRNA interactions", mode != DualMode.Forward);
    String messengerFirstColumn = (mode == DualMode.Forward) ? expressionTable.matrixInfo.columnName(0)
        : sideExpressionTable.matrixInfo.columnName(0);
    String microFirstColumn = (mode == DualMode.Reverse) ? expressionTable.matrixInfo.columnName(0)
        : sideExpressionTable.matrixInfo.columnName(0);
    networkService.prepareNetworkDownload(network, format, messengerFirstColumn, microFirstColumn,
        new PendingAsyncCallback<String>(screen) {
      @Override
      public void handleSuccess(String url) {
        Utils.displayURL("Your download is ready.", "Download", url);
      }
    });
  }

  /*
   * The networks need to be available in a superclass constructor, so this 
   * needs to be initialized when it first gets used, which is earlier than 
   * DualTableView's constructor code is executed.  
   */
  @Override
  public List<Network> networks() {
    if (_networks == null) {
      _networks = screen.getParser().getNetworks();
    }
    return _networks;
  }

  // NetworkVisualizationDialog.Delegate methods
  @Override
  public void saveNetwork(Network network) {
    networks().add(network);
    screen.getParser().storeNetworks(networks());
    networkMenu.networksChanged();
  }

  @Override
  public void deleteNetwork(Network network) {
    if (!networks().remove(network)) {

    }
    screen.getParser().storeNetworks(networks());
    networkMenu.networksChanged();
  }

  @Override
  public void visualizeNetwork(Network network) {
    new NetworkVisualizationDialog(this, logger).initWindow(network);
  }
}
