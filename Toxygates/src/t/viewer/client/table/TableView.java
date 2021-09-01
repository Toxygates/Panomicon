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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import t.viewer.client.UIFactory;
import t.viewer.client.screen.ScreenManager;
import t.viewer.client.screen.groupdef.ColumnScreen;
import t.shared.common.*;
import t.shared.common.sample.ExpressionRow;
import t.shared.common.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.ClientGroup;
import t.viewer.client.components.*;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.screen.data.DataScreen;
import t.viewer.client.screen.data.HeatmapViewer;
import t.viewer.client.screen.data.MirnaSourceDialog;
import t.viewer.shared.*;
import t.viewer.shared.mirna.MirnaSource;

/**
 * A DataView based on a single ExpressionTable.
 */
public class TableView extends DataView implements ExpressionTable.Delegate,
    ETMatrixManager.Loader, AssociationManager.ViewDelegate<ExpressionRow>, 
    MirnaSourceDialog.Delegate {

  public enum ViewType {
    Single, Dual
  }
  
  protected ExpressionTable expressionTable;
  
  protected DataScreen screen;
  protected AppInfo appInfo;
  protected ScreenManager manager;
  protected UIFactory factory;
  protected Logger logger;
  protected MatrixServiceAsync matrixService;
  protected NonRepeatingAlert mirnaSourcesUnsetWarning;
  
  public TableView(DataScreen screen,
                   String mainTableTitle, 
                   boolean mainTableSelectable) {
    this.screen = screen;
    this.appInfo = screen.appInfo();
    this.manager = screen.manager();
    this.matrixService = manager.matrixService();
    this.factory = manager.factory();
    this.logger = Logger.getLogger("tableView");
    this.expressionTable = makeExpressionTable(mainTableTitle, mainTableSelectable);
    mirnaSourcesUnsetWarning = new NonRepeatingAlert("Please select miRNA sources (in the tools menu) to enable mRNA-miRNA associations.");
    expressionTable.setDisplayPColumns(false);
    expressionTable.loadColumnVisibility();
    initWidget(content());   
    setupMenus();

    addToolbar(expressionTable.analysisTools());
  }
  
  public ViewType type() {
    return ViewType.Single;
  }
  
  @Override
  public void columnsChanged(List<ClientGroup> columns) {
    super.columnsChanged(columns);
    expressionTable.columnsChanged(columns);
  }

  @Override
  public void probesChanged(String[] probes) {
    super.probesChanged(probes);
    expressionTable.probesChanged(probes);
  }
  
  @Override
  public void showMirnaSourcesAlert(boolean force) {
    mirnaSourcesUnsetWarning.possiblyAlert(force);
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
    
    // Note: this is secretly a (stateful) tick menu item, but without the tick.
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

    MenuItem heatMapMenu = new MenuItem("Show heat map", () -> makeHeatMap());
    addAnalysisMenuItem(heatMapMenu);
  }
  
  protected void showMirnaSourceDialog() {
    MirnaSource[] sources = appInfo.mirnaSources();
    new MirnaSourceDialog(screen, this, sources,
        screen.getStorage().mirnaSourcesStorage.getIgnoringException()).
        display("Choose miRNA sources", DialogPosition.Center);
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
  
  // hook to be overridden
  protected void onGettingExpressionFailed() { }
  
  @Override
  public void reloadDataIfNeeded() {
//    logger.info("chosenProbes: " + chosenProbes.length + " lastProbes: "
//        + (lastProbes == null ? "null" : "" + lastProbes.length));

    if (chosenColumns.size() == 0) {
      Window.alert("Please define sample groups to see data.");
      screen.manager().attemptProceed(ColumnScreen.key);
      return;
    }
    
    // Attempt to avoid reloading the data
    if (lastColumns == null || !chosenColumns.equals(lastColumns)) {
//      logger.info("Data reloading needed");
      Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_VIEW_DATA);
      expressionTable.setStyle(styleForColumns(chosenColumns));
      expressionTable.getExpressions();
      onReloadData();
    } else if (!Arrays.equals(chosenProbes, lastProbes)) {
//      logger.info("Only refiltering needed");
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
    matrixService.loadMatrix(defaultMatrix, ClientGroup.convertToGroups(chosenColumns), 
        chosenProbes, valueType, initFilters, 
      new AsyncCallback<ManagedMatrixInfo>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Unable to load dataset");
          logger.log(Level.SEVERE, "Unable to load dataset", caught);
        }

        @Override
        public void onSuccess(ManagedMatrixInfo result) {
            expressionTable.matrix().setInitialMatrix(chosenProbes, result);
        }
      });
  }
  
  protected TableStyle styleForColumns(List<ClientGroup> columns) {
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
    if (needMirnaSources() && !mirnaSourcesSet()) {
      Window.alert("miRNA sources not enabled; mRNA-miRNA associations will not be available.");
    }
    screen.sendMirnaSources();
  }
  
  public void beforeUpdateMirnaSources() {
    if (expressionTable.associations().isVisible(AType.MiRNA)) {
      expressionTable.associations().removeAssociation(AType.MiRNA);
      expressionTable.redrawGrid();
    } else if (expressionTable.associations().isVisible(AType.MRNA)) {
      expressionTable.associations().removeAssociation(AType.MRNA);
      expressionTable.redrawGrid();
    }
  }

  public void afterMirnaSourcesUpdated() {
    if (expressionTable.associations().isVisible(AType.MiRNA)) {
      expressionTable.associations().getAssociations(new AType[] {AType.MiRNA});
      
    } else if (expressionTable.associations().isVisible(AType.MRNA)) {
      expressionTable.associations().getAssociations(new AType[] {AType.MRNA});
    }
  }

    public boolean needMirnaSources() {
    return (expressionTable.associations().isVisible(AType.MiRNA) ||
        expressionTable.associations().isVisible(AType.MRNA));
  }
  
  /**
   * The probes currently contained in the current matrix, up to a limit.
   */
  @Override
  public String[] displayedAtomicProbes(boolean limit) {
    String[] r = expressionTable.matrixInfo().getAtomicProbes(limit);
    if (limit && r.length < expressionTable.matrixInfo().numRows()) {
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
      //Have to push this all the way out to the screen to ensure the new empty set gets stored
      screen.probesChanged(new String[0]);
      onGettingExpressionFailed();
      reloadDataIfNeeded();
    }
    displayInfo("Data loading failed.");
  }

  @Override
  public void afterGetRows(ExpressionTable table) { 
    mirnaSourcesUnsetWarning.reset();
  }

  @Override
  public void onApplyColumnFilter() {
  }

  // AssociationManager.ViewDelegate methods
  @Override
  public void associationsUpdated(AssociationManager<ExpressionRow> associations, Association[] result) {
    TableView.this.associationsUpdated(result);
  }
  
  @Override
  public boolean mirnaSourcesSet() {
    List<MirnaSource> mirnaSources = screen.getStorage().mirnaSourcesStorage.getIgnoringException();
    return mirnaSources != null && mirnaSources.size() > 0;
  }
}
