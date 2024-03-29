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

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import t.shared.common.AType;
import t.shared.common.GroupUtils;
import t.shared.common.ValueType;
import t.shared.common.sample.ExpressionRow;
import t.gwt.viewer.client.Analytics;
import t.gwt.viewer.client.ClientGroup;
import t.gwt.viewer.client.Utils;
import t.gwt.viewer.client.components.PendingAsyncCallback;
import t.gwt.viewer.client.network.*;
import t.gwt.viewer.client.rpc.NetworkService;
import t.gwt.viewer.client.rpc.NetworkServiceAsync;
import t.gwt.viewer.client.screen.data.DataScreen;
import t.gwt.viewer.client.screen.data.NetworkMenu;
import t.gwt.viewer.client.storage.NamedObjectStorage;
import t.shared.viewer.Association;
import t.shared.viewer.ColumnFilter;
import t.shared.viewer.mirna.MirnaSource;
import t.shared.viewer.network.Format;
import t.shared.viewer.network.Network;
import t.shared.viewer.network.NetworkInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A DataView that displays an interaction network as two tables.
 */
public class DualTableView extends TableView implements NetworkMenu.Delegate, NetworkVisualizationDialog.Delegate {
  protected ExpressionTable sideExpressionTable;
  private NetworkMenu networkMenu;

  protected final static String mainMatrix = NetworkService.tablePrefix + "MAIN";
  protected final static String sideMatrix = NetworkService.tablePrefix + "SIDE";
  
  final static int MAX_SECONDARY_ROWS = 100;

  protected NetworkServiceAsync networkService;
  
  // do not access directly: see explanation below
  private NamedObjectStorage<PackedNetwork> _networkStorage;
  
  private NetworkVisualizationDialog netvizDialog;

  protected MenuItem sideDownloadGrouped, sideDownloadSingle;
  
  public enum DualMode {
    Forward("mRNA", "miRNA", AType.MiRNA) {
      @Override
      void setVisibleColumns(ExpressionTable table) {
        table.associations().setVisible(AType.MiRNA, true);
        table.associations().setVisible(AType.MRNA, false);
      }
    },
    
    Reverse("miRNA", "mRNA", AType.MRNA) {
      @Override
      void setVisibleColumns(ExpressionTable table) {
        table.associations().setVisible(AType.MiRNA, false);
        table.associations().setVisible(AType.MRNA, true);
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
  
  NetworkInfo networkInfo;
  
  protected SplitLayoutPanel splitLayout;
  
  public DualTableView(DataScreen screen, String mainTableTitle) {
    super(screen, mainTableTitle, true);
    networkService = screen.manager().networkService();
    expressionTable.selectionModel().addSelectionChangeHandler(e -> {      
      network.onSourceSelectionChanged();      
    });
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e -> {   
      network.onDestSelectionChanged();   
    });
  }
  
  /*
   * The networks need to be available in a superclass constructor, so this 
   * needs to be initialized when it first gets used, which is earlier than 
   * DualTableView's constructor code is executed.  
   */
  public NamedObjectStorage<PackedNetwork> networkStorage() {
    if (_networkStorage == null) {
      _networkStorage = new NamedObjectStorage<>(screen.getStorage().packedNetworksStorage,
          packedNetwork -> packedNetwork.title(),
          (packedNetwork, newName) -> packedNetwork.changeTitle(newName));
    }
    return _networkStorage;
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

    sideExpressionTable = new ExpressionTable(screen, flags, mode.sideStyle(),
        this, this, this);
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
    return network;
  }
  
  protected DualTableNetwork network;

  protected void flipDualView() {    
    preferredDoubleMode = mode.flip();    
    List<ClientGroup> allColumns = new ArrayList<ClientGroup>(chosenColumns);
    allColumns.addAll(sideExpressionTable.chosenColumns());
    columnsChanged(allColumns);
    probesChanged(new String[0]);

    reloadDataIfNeeded();
  }
    
  @Override
  protected void associationsUpdated(Association[] result) {
    super.associationsUpdated(result);
    /**
     * This mechanism is temporary and may eventually be replaced by a linking map
     * provided by the server.
     */
    network.updateLinkingMap();
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
    
    fileMenu.addSeparator();    
    sideDownloadGrouped =
        new MenuItem("Download side table CSV (grouped samples)...", false, () -> {          
            sideExpressionTable.downloadCSV(false);
            Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_GROUPED_SAMPLES);
        });
    fileMenu.addItem(sideDownloadGrouped);
    sideDownloadSingle = new MenuItem("Download side table CSV (individual samples)...", false, () -> {      
        sideExpressionTable.downloadCSV(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
            Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_INDIVIDUAL_SAMPLES);      
    });
    fileMenu.addItem(sideDownloadSingle);  
    
    networkMenu = new NetworkMenu(this);
    topLevelMenus.add(networkMenu.menuItem());
  }
  
  //Initial title only - need the constant here since the field won't be initialised
//  @Override
//  protected String mainTableTitle() { return "mRNA"; }     
  
  protected boolean mainTableSelectable() { return true; }  
  
  protected List<ClientGroup> columnsOfType(List<ClientGroup> from, String type) {
    return from.stream().filter(g -> type.equals(GroupUtils.groupType(g))).
        collect(Collectors.toList());    
  }
  
  protected List<ClientGroup> columnsForMainTable(List<ClientGroup> from) {    
    List<ClientGroup> r = columnsOfType(from, mode.mainType);
    
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<ClientGroup> columnsForSideTable(List<ClientGroup> from) {
    return columnsOfType(from, mode.sideType);        
  }
  
  @Override
  public void columnsChanged(List<ClientGroup> columns) {
    logger.info("Dual mode pick for " + columns.size() + " columns");
    mode = preferredDoubleMode;    
    
    expressionTable.setTitleHeader(mode.mainType);    
    expressionTable.setStyleAndApply(mode.mainStyle());
    
    makeNetwork();
    sideExpressionTable.setTitleHeader(mode.sideType);
    sideExpressionTable.setStyleAndApply(mode.sideStyle());

    super.columnsChanged(columnsForMainTable(columns));
    
    List<ClientGroup> sideColumns = columnsForSideTable(columns);
    if (sideExpressionTable != null && !sideColumns.isEmpty()) {    
      sideExpressionTable.columnsChanged(sideColumns);    
    }
    logger.info("Dual table mode: " + mode);

    splitLayout.setWidgetSize(sideExpressionTable, mode.sideTableWidth());
    
    sideDownloadGrouped.setText("Download " + mode.sideType + " CSV (grouped)...");
    sideDownloadSingle.setText("Download " + mode.sideType + " CSV (individual)...");
  }  
  
  @Override
  public void afterMirnaSourcesUpdated() {
    super.afterMirnaSourcesUpdated();
    //The server-side network will have changed, so we force the side expression table
    //to reflect this.
    
    sideExpressionTable.refetchRows(MAX_SECONDARY_ROWS);
  }
  
  @Override
  public boolean needMirnaSources() {
    return true;
  }
  
  @Override
  public void reloadDataIfNeeded() {
    // Task: doesn't make sense to set visible columns every time we reload data
    expressionTable.associations().setAssociationAutoRefresh(false);
    mode.setVisibleColumns(expressionTable);
    expressionTable.associations().setAssociationAutoRefresh(true);

    super.reloadDataIfNeeded();    
    sideExpressionTable.setIndicatedProbes(new HashSet<String>(), false);
    expressionTable.setIndicatedProbes(new HashSet<String>(), false);
  }
  
  @Override
  protected void onReloadData() {
    sideExpressionTable.matrix().clear();
  }

  @Override
  protected String mainMatrixId() {
    return mainMatrix;
  }
  
  @Override
  public void loadInitialMatrix(ValueType valueType, List<ColumnFilter> initFilters) {
    networkService.loadNetwork(mainMatrix, ClientGroup.convertToGroups(expressionTable.chosenColumns), 
        chosenProbes, sideMatrix, 
        ClientGroup.convertToGroups(sideExpressionTable.chosenColumns), 
        valueType,
      new PendingAsyncCallback<NetworkInfo>(screen.manager(), "Unable to load network") {
        
        @Override
        public void handleSuccess(NetworkInfo result) {
            expressionTable.matrix().setInitialMatrix(chosenProbes, result.mainInfo());
            sideExpressionTable.matrix().setInitialMatrix(chosenProbes, result.sideInfo());
          setNetwork(result);
        }
      });
  }
  
  protected void setNetwork(NetworkInfo ni) {
    networkInfo = ni;
  }
  
  // NetworkMenu.Delegate methods
  @Override
  public void visualizeNetwork() {
    networkService.currentView(mainMatrix, new PendingAsyncCallback<Network>(screen.manager(),
        "Unable to load network view") {
      @Override
      public void handleSuccess(Network result) {
        result.changeTitle(networkStorage().suggestName(result.title()));
        netvizDialog = new NetworkVisualizationDialog(DualTableView.this, 
            networkStorage(), logger);
        netvizDialog.initWindow(result);
      }      
    });
  }

  @Override
  public void downloadNetwork(Format format) {       
    String messengerFirstColumn = (mode == DualMode.Forward) ? expressionTable.matrix().info().columnName(0)
        : sideExpressionTable.matrix().info().columnName(0);
    String microFirstColumn = (mode == DualMode.Reverse) ? expressionTable.matrix().info().columnName(0)
        : sideExpressionTable.matrix().info().columnName(0);
    networkService.prepareNetworkDownload(mainMatrix, format, 
      messengerFirstColumn, microFirstColumn,
        new PendingAsyncCallback<String>(screen.manager()) {
      @Override
      public void handleSuccess(String url) {
        Utils.displayURL("Your download is ready.", "Download", url);
      }
    });
  }

  @Override
  public List<PackedNetwork> networks() {
    return networkStorage().allObjects();
  }

  @Override
  public void deleteNetwork(PackedNetwork network) {
    networkStorage().remove(network.title());
    networkStorage().saveToStorage();
    networkMenu.networksChanged();
  }

  @Override
  public void visualizeNetwork(PackedNetwork network) {
    netvizDialog = new NetworkVisualizationDialog(DualTableView.this, 
        networkStorage(), logger);
    netvizDialog.initWindow(network.unpack());
  }
  
  @Override
  public void saveProbesAsGeneSet(PackedNetwork network) {
    NetworkToGeneSetDialog dialog = new NetworkToGeneSetDialog(network.unpack(), screen);
    dialog.initWindow();
  }

  // NetworkVisualizationDialog.Delegate methods
  @Override
  public void saveNetwork(PackedNetwork network) {
    networkStorage().put(network);
    networkStorage().saveToStorage();
    networkMenu.networksChanged();
  }

  @Override
  public void showMirnaSourceDialog() {
    super.showMirnaSourceDialog();
  }

  @Override
  public FilterEditPanel filterEditPanel() {
    return new FilterEditPanel(expressionTable.grid, expressionTable.matrix(),
        sideExpressionTable.grid, sideExpressionTable.matrix());
  }

  @Override
  public void onNetworkVisualizationDialogClose() {
    netvizDialog = null;
  }

  @Override
  public void addPendingRequest() {
    screen.manager().addPendingRequest();
  }

  @Override
  public void removePendingRequest() {
    screen.manager().removePendingRequest();
  }

  // ExpressionTable.Delegate methods
  @Override
  public void onGettingExpressionFailed(ExpressionTable table) {
    if (table == expressionTable) {
      super.onGettingExpressionFailed(table);
    }
  }

  @Override
  public void afterGetRows(ExpressionTable table) {
    super.afterGetRows(table);
    if (table == expressionTable) {
      sideExpressionTable.refetchRows(MAX_SECONDARY_ROWS);
    } else if (table == sideExpressionTable) {
      if (netvizDialog != null) {
        networkService.currentView(mainMatrix,
            new PendingAsyncCallback<Network>(screen.manager(), "Unable to load network view") {
              @Override
              public void handleSuccess(Network result) {
                netvizDialog.loadNetwork(result);
                // Resume accepting user input, which would have been blocked in 
                // onApplyColumnFilter or mirnaSourcesDialogMirnaSourcesChanged
                screen.manager().removePendingRequest();
              }
            });
      }
    }
  }

  @Override
  public void onApplyColumnFilter() {
    // If the network visualization dialog is open, we block user input until 
    // we eventually get a new network (which happens in afterGetRows)
    if (netvizDialog != null) {
      screen.manager().addPendingRequest();
    }
  }

  // AssociationManager.ViewDelegate methods
  @Override
  public void associationsUpdated(AssociationManager<ExpressionRow> associations, Association[] result) {
    if (associations == expressionTable.associations()) {
      super.associationsUpdated(associations, result);
    }
  }

  //MirnaSourceDialog.Delegate method
  @Override
  public void mirnaSourceDialogMirnaSourcesChanged(MirnaSource[] mirnaSources) {
    // As in onApplyColumnFilter, block user input until we get a new network in afterGetRows
    if (netvizDialog != null) {
      screen.manager().addPendingRequest();
    }
    super.mirnaSourceDialogMirnaSourcesChanged(mirnaSources);
  }
}
