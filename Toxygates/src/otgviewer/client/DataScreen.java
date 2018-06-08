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

package otgviewer.client;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.components.*;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.sample.Group;
import t.model.sample.AttributeSet;
import t.viewer.client.*;
import t.viewer.client.components.DataView;
import t.viewer.client.table.TableView;
import t.viewer.shared.intermine.IntermineInstance;

import com.google.gwt.user.client.ui.*;

/**
 * The main data display screen. It displays data in a single DataView instance.
 */
public class DataScreen extends MinimalScreen implements ImportingScreen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected DataView dataView;

  protected String[] lastProbes;
  protected List<Group> lastColumns;

  // TODO: factor out heat map management logic + state
  // together with UIFactory.hasHeatMapMenu
  @Nullable
  private MenuItem heatMapMenu;

  private List<MenuItem> intermineMenuItems;

  GeneSetsMenuItem geneSetsMenu;

  protected String[] chosenProbes = new String[0];
  protected List<Group> chosenColumns = new ArrayList<Group>();
  public List<ItemList> chosenItemLists = new ArrayList<ItemList>();
  public ItemList chosenGeneSet = null;
  protected List<ItemList> chosenClusteringList = new ArrayList<ItemList>();

  @Override
  public void loadState(AttributeSet attributes) {
    StorageParser parser = getParser();
    chosenProbes = parser.getProbes();
    chosenColumns = parser.getChosenColumns(schema(), attributes());
    chosenItemLists = parser.getItemLists();
    chosenGeneSet = parser.getGeneSet();
    chosenClusteringList = parser.getClusteringLists();
    dataView.columnsChanged(chosenColumns);
    dataView.sampleClassChanged(parser.getSampleClass(attributes()));
    dataView.probesChanged(chosenProbes);  

    geneSetToolbar.geneSetChanged(chosenGeneSet);

    geneSetsMenu.itemListsChanged(chosenItemLists);
  }

  DataScreen(ScreenManager man, List<MenuItem> intermineItems) {
    super("View data", key, man, man.resources().dataDisplayHTML(),
        man.resources().dataDisplayHelp());
    geneSetToolbar = makeGeneSetSelector();    
    dataView = makeDataView();
    setupMenuItems();
  }

  @Override
  public List<ItemList> clusteringList() {
    return chosenClusteringList;
  }

  @Override
  public List<ItemList> itemLists() {
    return chosenItemLists;
  }

  public ItemList geneSet() {
    return chosenGeneSet;
  }

  @Override
  public List<Group> chosenColumns() {
    return chosenColumns;
  }

  @Override
  public String[] chosenProbes() {
    return chosenProbes;
  }

  protected GeneSetToolbar makeGeneSetSelector() {
    return new GeneSetToolbar(this) {
      @Override
      public void itemsChanged(List<String> items) {
        reloadDataIfNeeded();
      }
    };
  }
  
  protected static final String defaultMatrix = "DEFAULT";

  protected @Nullable String mainTableTitle() { return null; }
  
  protected boolean mainTableSelectable() { return false; }  

  //TODO remove
  protected void beforeGetAssociations() {}

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
    Widget dvTools = dataView.tools();
    if (dvTools != null) {
      mainTools.add(dvTools);
    }
    mainTools.add(geneSetToolbar.selector());    
    mainTools.add(makeInfoPanel());
    addToolbar(mainTools, STANDARD_TOOL_HEIGHT);    
    for (Widget w: dataView.toolbars()) {
      addToolbar(w, STANDARD_TOOL_HEIGHT);
    }    
  }

  protected void displayInfo(String message) {
    logger.info("User info: " + message);
    infoLabel.setText(message);
  }
  
  @Override
  public Widget content() {    
    return dataView;
  }

  protected TableView makeDataView() {
    return new TableView(this, mainTableTitle(),
      mainTableSelectable()) {
        @Override
        protected void beforeGetAssociations() {
          super.beforeGetAssociations();
          DataScreen.this.beforeGetAssociations();
        }
    };
  }

  protected MenuBar analysisMenu;
  protected void setupMenuItems() {
    analysisMenu = new MenuBar(true);
    MenuItem analysisItem = new MenuItem("Tools", false, analysisMenu);
    addMenu(analysisItem);
        
    for (MenuItem mi: dataView.topLevelMenus()) {
      addMenu(mi);
    }
    
    addAnalysisMenuItem(new MenuItem("Enrichment...", () -> runEnrichment(null)));   
    for (MenuItem mi: dataView.analysisMenuItems()) {
      addAnalysisMenuItem(mi);
    }

<<<<<<< local
    GeneSetsMenuItem geneSetsMenu = factory().geneSetsMenuItem(this);
    addListener(geneSetsMenu);
    addMenu(geneSetsMenu.menuItem());       
  }
  
  @Override
  public void addAnalysisMenuItem(MenuItem mi) {
    analysisMenu.add(mi);
=======
    geneSetsMenu = factory().geneSetsMenuItem(this);
    //addListener(geneSetsMenu);
    addMenu(geneSetsMenu.menuItem());

    MenuItem mColumns = new MenuItem("View", false, menuBar);
    addMenu(mColumns);

    menuBar = new MenuBar(true);
    mActions = new MenuItem("Tools", false, menuBar);
    for (MenuItem item : intermineMenuItems) {
      menuBar.addItem(item);
    }
    // TODO: this is effectively a tick menu item without the tick.
    // It would be nice to display the tick graphic, but then the textual alignment
    // of the other items on the menu becomes odd.
    menuBar.addItem(new TickMenuItem("Compare two sample groups", false, false) {
      @Override
      public void stateChange(boolean newState) {
        if (!visible) {
          // Trigger screen
          manager.attemptProceed(DataScreen.key);
          setState(true);
          showToolbar(expressionTable.analysisTools());
        } else {
          // Just toggle
          if (newState) {
            showToolbar(expressionTable.analysisTools());
          } else {
            hideToolbar(expressionTable.analysisTools());
          }
        }
      }
    }.menuItem());
    menuBar.addItem(new MenuItem("Enrichment...", () -> runEnrichment(null)));
    addMenu(mActions);
    
    if (factory().hasHeatMapMenu()) {
      menuBar.addItem(new MenuItem("Show heat map", () -> makeHeatMap()));
    }
>>>>>>> other
  }
  
  @Override
  public void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists) {
    itemListsChanged(itemLists);
    clusteringListsChanged(clusteringLists);
  }

  @Override
  public void propagateTo(DataViewListener other) {
    other.datasetsChanged(getParser().getDatasets());
    other.sampleClassChanged(getParser().getSampleClass(attributes()));
    other.probesChanged(chosenProbes);
    other.compoundsChanged(getParser().getCompounds());
    other.columnsChanged(chosenColumns);
    other.customColumnChanged(getParser().getCustomColumn(schema(), attributes()));
    other.itemListsChanged(chosenItemLists);
    other.geneSetChanged(chosenGeneSet);
    other.clusteringListsChanged(chosenClusteringList);
  }

  @Override
  public void runEnrichment(@Nullable IntermineInstance preferredInstance) {
    logger.info("Enrich " + DataScreen.this.displayedAtomicProbes().length + " ps");
    StringList genes = 
        new StringList(StringList.PROBES_LIST_TYPE, 
            "temp", DataScreen.this.displayedAtomicProbes());
    DataScreen.this.factory().enrichment(DataScreen.this, genes, preferredInstance);
  }

  @Override
  public boolean enabled() {
    return manager.isConfigured(ColumnScreen.key);
  }

  /**
   * Trigger a data reload, if necessary.
   */
  public void reloadDataIfNeeded() {
    dataView.reloadDataIfNeeded();    
  }

  @Override
  public void show() {
    super.show();
    
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
    logger.info("received " + probes.length + " probes");

    chosenProbes = probes;

    getParser().storeProbes(chosenProbes);

    lastProbes = null;
    lastColumns = null;
    dataView.probesChanged(probes);
  }

  public void geneSetChanged(ItemList geneSet) {
    chosenGeneSet = geneSet;
    getParser().storeGeneSet(geneSet);
    geneSetToolbar.geneSetChanged(geneSet);
  }

  public void columnsChanged(List<Group> columns) {
    chosenColumns = columns;
    dataView.columnsChanged(columns);
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    chosenItemLists = lists;
    getParser().storeItemLists(lists);
    geneSetsMenu.itemListsChanged(lists);
  }

  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    chosenClusteringList = lists;
    getParser().storeClusteringLists(lists);
    geneSetsMenu.clusteringListsChanged(lists);
  }

  public String[] displayedAtomicProbes() {
    return dataView.displayedAtomicProbes();    
  }

  @Override
  public List<PersistedState<?>> getPersistedItems() {
    return dataView.getPersistedItems();    
  }

  @Override
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

  @Override
  public boolean importColumns(List<Group> groups) {
    if (groups.size() > 0 && !groups.equals(chosenColumns)) {
      columnsChanged(groups);
      storeState();
      reloadDataIfNeeded();
      return true;
    } else {
      return false;
    }
  }
}
