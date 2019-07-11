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

package otg.viewer.client.screen.data;

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.*;
import t.common.shared.GroupUtils;
import t.model.sample.AttributeSet;
import t.viewer.client.Analytics;
import t.viewer.client.ClientGroup;
import t.viewer.client.Groups;
import t.viewer.client.Utils;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.storage.StorageProvider;
import t.viewer.client.table.DualTableView;
import t.viewer.client.table.TableView;
import t.viewer.client.table.TableView.ViewType;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
import t.viewer.shared.intermine.IntermineInstance;
import t.viewer.shared.mirna.MirnaSource;

/**
 * The main data display screen. It displays data in a single DataView instance.
 */
public class DataScreen extends MinimalScreen implements ImportingScreen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected TableView tableView;

  protected String[] lastProbes;

  protected GeneSetsMenu geneSetsMenu;
  
  protected Groups groups;
  
  protected String[] chosenProbes = new String[0];
  public List<ItemList> itemLists = new ArrayList<ItemList>();
  public ItemList chosenGeneSet = null;
  protected List<ItemList> clusteringLists = new ArrayList<ItemList>();

  private String[] urlProbes = null;

  @Override
  public void loadState(AttributeSet attributes) {
    StorageProvider storage = getStorage();
    chosenProbes = storage.probesStorage.getIgnoringException().toArray(new String[0]);
    groups.storage().loadFromStorage();
    itemLists = storage.itemListsStorage.getIgnoringException();
    chosenGeneSet = storage.chosenGenesetStorage.getIgnoringException();
    clusteringLists = storage.clusteringListsStorage.getIgnoringException();

    if (tableView == null || tableView.type() != preferredViewType()) {
      rebuildGUI();
    }
    
    if (tableView.needMirnaSources() && !tableView.mirnaSourcesSet()) {
      Window.alert("Please select miRNA sources (in the tools menu) to enable mRNA-miRNA associations.");
    }

    tableView.columnsChanged(groups.activeGroups());
    tableView.probesChanged(chosenProbes);  
    geneSetToolbar.geneSetChanged(chosenGeneSet);
    geneSetsMenu.itemListsChanged(itemLists);
  }
  
  public void sendMirnaSources() {
    logger.info("Sending mirna sources to server");
    List<MirnaSource> mirnaSources = getStorage().mirnaSourcesStorage.getIgnoringException();
    if (mirnaSources == null) {
      mirnaSources = new ArrayList<MirnaSource>();
    }
    if (tableView != null) {
      tableView.beforeUpdateMirnaSources();
    }
    MirnaSource[] mirnaSourceArray  = mirnaSources.toArray(new MirnaSource[0]);
    manager.networkService().setMirnaSources(mirnaSourceArray, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Unable to set miRNA sources.");
      }

      @Override
      public void onSuccess(Void result) {
        if (tableView != null) {
          tableView.afterMirnaSourcesUpdated();
        }
      }
    });
  }

  public TableView dataView() {
    return tableView;
  }

  @Override
  protected void rebuildGUI() {
    tableView = makeDataView();
    super.rebuildGUI();
    logger.info("DataScreen rebuilding to " + preferredViewType());
    setupMenuItems();
  }

  public DataScreen(ScreenManager man) {
    super("View data", key, man, man.resources().dataDisplayHTML(),
        man.resources().dataDisplayHelp());
    groups = new Groups(getStorage().groupsStorage);
    geneSetToolbar = makeGeneSetSelector();
    sendMirnaSources();
  }

  @Override
  public List<ItemList> clusteringLists() {
    return clusteringLists;
  }

  @Override
  public List<ItemList> itemLists() {
    return itemLists;
  }

  public ItemList geneSet() {
    return chosenGeneSet;
  }

  @Override
  public List<ClientGroup> chosenColumns() {
    return groups.activeGroups();
  }

  protected GeneSetToolbar makeGeneSetSelector() {
    return new GeneSetToolbar(this);
  }
  
  protected static final String defaultMatrix = "DEFAULT";

  protected @Nullable String mainTableTitle() { return null; }
  
  protected boolean mainTableSelectable() { return false; }  

  static final public int STANDARD_TOOL_HEIGHT = 43;

  protected Label infoLabel;
  
  protected Widget makeInfoPanel() {
    infoLabel = Utils.mkEmphLabel("");
    infoLabel.addStyleName("infoLabel");
    HorizontalPanel p = Utils.mkHorizontalPanel(true, infoLabel);
    p.setHeight(STANDARD_TOOL_HEIGHT + "px");
    return p;       
  }
  
  protected HorizontalPanel mainTools;
  
  @Override
  protected void addToolbars() {
    super.addToolbars();
    mainTools = new HorizontalPanel();
    mainTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    if (tableView != null) {
      Widget dvTools = tableView.tools();
      if (dvTools != null) {
        mainTools.add(dvTools);
      }
    }
    mainTools.add(geneSetToolbar.selector());    
    mainTools.add(makeInfoPanel());
    addToolbar(mainTools, STANDARD_TOOL_HEIGHT);
    if (tableView != null) {
      for (Widget w: tableView.toolbars()) {
        addToolbar(w, STANDARD_TOOL_HEIGHT);
      }
    }
  }

  protected void displayInfo(String message) {
    logger.info("User info: " + message);
    infoLabel.setText(message);
  }
  
  @Override
  protected Widget content() {
    return tableView != null ? tableView : new SimplePanel(); 
  }

  public TableView.ViewType preferredViewType() {  
    groups.storage().loadFromStorage();
    List<ClientGroup> chosenColumns = groups.activeGroups();
    String[] types =
        chosenColumns.stream().map(g -> GroupUtils.groupType(g)).distinct().toArray(String[]::new);    
    return types.length >= 2 ? ViewType.Dual : ViewType.Single;
  }

  protected TableView makeDataView() {
    ViewType type = preferredViewType();
    switch(type) {
      case Dual:
        return new DualTableView(this, mainTableTitle());
      default:
      case Single:
        return new TableView(this, mainTableTitle(), mainTableSelectable()) {
          @Override
          protected void onGettingExpressionFailed() {
            geneSetChanged(null);
          }
        };      
    }
  }

  protected MenuBar analysisMenu;
  protected void setupMenuItems() {
    analysisMenu = tableView.analysisMenu();    
    for (MenuItem mi: tableView.topLevelMenus()) {
      addMenu(mi);
    }
    
    analysisMenu.addSeparator();
    
    for (MenuItem mi: intermineMenuItems(appInfo())) {
      addAnalysisMenuItem(mi);
    }
    
    addAnalysisMenuItem(new MenuItem("Enrichment...", () -> runEnrichment(null)));   

    geneSetsMenu = factory().geneSetsMenu(this);
    addMenu(geneSetsMenu.menuItem());       
  }
  
  public void addAnalysisMenuItem(MenuItem mi) {
    analysisMenu.addItem(mi);
  }
  
  @Override
  public void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists) {
    itemListsChanged(itemLists);
    clusteringListsChanged(clusteringLists);
  }

  @Override
  public void runEnrichment(@Nullable IntermineInstance preferredInstance) {
    logger.info("Enrich " + DataScreen.this.displayedAtomicProbes().length + " ps");
    StringList genes = 
        new StringList(StringList.PROBES_LIST_TYPE, 
            "temp", DataScreen.this.displayedAtomicProbes());
    DataScreen.this.factory().displayEnrichmentDialog(DataScreen.this, genes, preferredInstance);
  }

  @Override
  public boolean enabled() {
    groups.storage().loadFromStorage();
    List<ClientGroup> chosenColumns = groups.activeGroups();
    return chosenColumns != null && chosenColumns.size() > 0;
  }

  /**
   * Trigger a data reload, if necessary.
   */
  public void reloadDataIfNeeded() {
    tableView.reloadDataIfNeeded();    
  }
  
  @Override
  public void show() {         
    super.show();
    getProbes();
    reloadDataIfNeeded();   
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public String getGuideText() {
    return "Here you can inspect expression values for the sample groups you have defined. "
        + "Click on column headers to sort data.";
  }

  public void probesChanged(String[] probes) {

    chosenProbes = probes;

    getStorage().probesStorage.store(Arrays.asList(chosenProbes));

    lastProbes = null;
    tableView.probesChanged(probes);
  }

  /**
   * Changes the gene set. Note: tracks a "Change Gene Set" event with Google Analytics.
   */
  public void geneSetChanged(ItemList geneSet) {
    chosenGeneSet = geneSet;
    getStorage().chosenGenesetStorage.store(geneSet);
    geneSetToolbar.geneSetChanged(geneSet);
    Analytics.trackEvent(Analytics.CATEGORY_TABLE, Analytics.ACTION_CHANGE_GENE_SET);
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    itemLists = lists;
    getStorage().itemListsStorage.store(lists);
    geneSetsMenu.itemListsChanged(lists);
  }

  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    clusteringLists = lists;
    getStorage().clusteringListsStorage.store(lists);
    geneSetsMenu.clusteringListsChanged(lists);
  }

  public String[] displayedAtomicProbes() {
    return tableView.displayedAtomicProbes();    
  }

  @Override
  public void setUrlProbes(String[] probes) {
    urlProbes = probes;
  }

  /**
   * Fetch probes if applicable. Used to load probes that were read from URL string.
   */
  public void getProbes() {
    if (urlProbes != null) {
      manager().probeService().identifiersToProbes(urlProbes, true, true, false, null,
          new PendingAsyncCallback<String[]>(this,
              "Failed to resolve gene identifiers") {
            @Override
            public void handleSuccess(String[] probes) {
              importProbes(probes);
            }
          });
    }
    urlProbes = null;
  }

  public boolean importProbes(String[] probes) {
    if (Arrays.equals(probes, chosenProbes)) {
      return false;
    } else {
      probesChanged(probes);
      storeState();
      reloadDataIfNeeded();     
      return true;
    }
  }
}
