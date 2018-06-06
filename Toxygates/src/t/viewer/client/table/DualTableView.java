package t.viewer.client.table;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.DLWScreen;
import otgviewer.client.components.PendingAsyncCallback;
import t.common.shared.AType;
import t.common.shared.GroupUtils;
import t.common.shared.sample.Group;
import t.viewer.client.Utils;
import t.viewer.client.network.DualTableNetwork;
import t.viewer.client.network.NetworkController;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.Association;
import t.viewer.shared.network.Format;
import t.viewer.shared.network.Network;

/**
 * A DataView that displays an interaction network as two tables.
 */
public class DualTableView extends TableView {
  protected ExpressionTable sideExpressionTable;
  protected final static String sideMatrix = "SECONDARY";
  final static int MAX_SECONDARY_ROWS = Network.MAX_SIZE;
  

  static enum DualMode {
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
    
    final String mainType, sideType;
    final AType linkingType;    
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
  
  
  public DualTableView(DLWScreen screen, String mainTableTitle) {
    super(screen, mainTableTitle, true);
    expressionTable.selectionModel().addSelectionChangeHandler(e -> {      
      network.onSourceSelectionChanged();      
    });
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e -> {   
      network.onDestSelectionChanged();   
    });       
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
      mode.mainType, mode.sideType, mode.linkingType, MAX_SECONDARY_ROWS);
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
    MenuItem mi = new MenuItem("Download interaction network (DOT)...", 
      () -> downloadNetwork(Format.DOT));             
    addAnalysisMenuItem(mi);
    
    mi = new MenuItem("Download interaction network (SIF)...", 
      () -> downloadNetwork(Format.SIF));       
    addAnalysisMenuItem(mi);
    
    mi = new MenuItem("Download interaction network (Custom)...", 
      () -> downloadNetwork(Format.Custom));       
    addAnalysisMenuItem(mi);    
  }
  
  protected void downloadNetwork(Format format) {
    MatrixServiceAsync matrixService = screen.manager().matrixService();
    Network network = controller.buildNetwork("miRNA-mRNA interactions", mode != DualMode.Forward);
    matrixService.prepareNetworkDownload(network, format, new PendingAsyncCallback<String>(screen) {
      public void handleSuccess(String url) {
        Utils.displayURL("Your download is ready.", "Download", url);
      }
    });
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
  
  /**
   * Based on the available columns, pick the correct display mode.
   * The mode may be split or non-split.
   */
  protected DualMode pickMode(List<Group> columns) {
    String[] types = columns.stream().map(g -> GroupUtils.groupType(g)).distinct().
        toArray(String[]::new);
    if (types.length >= 2) {
      return preferredDoubleMode;
    } else {
      logger.severe("DualTableView constructed but only one column type");
      return null;
    }
  }
  
//  @Override
//  public void loadState(StorageParser p, DataSchema schema, AttributeSet attributes) {
//    //TODO this is a state management hack to force the columns to be fully re-initialised
//    //every time we show the screen.
//    chosenColumns = new ArrayList<Group>();
//    super.loadState(p, schema, attributes);
//  }
  
  @Override
  protected void changeColumns(List<Group> columns) {
    logger.info("Dual mode pick for " + columns.size() + " columns");
    mode = pickMode(columns);    
    
    expressionTable.setTitleHeader(mode.mainType);    
    expressionTable.setStyleAndApply(mode.mainStyle());
    
    makeNetwork();
    sideExpressionTable.setTitleHeader(mode.sideType);
    sideExpressionTable.setStyleAndApply(mode.sideStyle());

    super.changeColumns(columnsForMainTable(columns));
    
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
  
}
