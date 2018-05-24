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

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.sample.*;
import t.model.sample.AttributeSet;
import t.viewer.client.*;
import t.viewer.client.table.ExpressionTable;
import t.viewer.client.table.RichTable.HideableColumn;
import t.viewer.client.table.TableStyle;
import t.viewer.shared.intermine.IntermineInstance;

/**
 * The main data display screen. Data is displayed in the ExpressionTable widget.
 */
public class DataScreen extends MinimalScreen implements ImportingScreen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected ExpressionTable expressionTable;

  protected String[] lastProbes;
  protected List<Group> lastColumns;

  // TODO: factor out heat map management logic + state
  // together with UIFactory.hasHeatMapMenu
  @Nullable
  private MenuItem heatMapMenu;
  Map<String, TickMenuItem> hideableMenuItems = new HashMap<String, TickMenuItem>();

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

    expressionTable.columnsChanged(chosenColumns);
    expressionTable.sampleClassChanged(parser.getSampleClass(attributes()));
    expressionTable.probesChanged(chosenProbes);

    geneSetToolbar.geneSetChanged(chosenGeneSet);
  }

  DataScreen(ScreenManager man, List<MenuItem> intermineItems) {
    super("View data", key, man, man.resources().dataDisplayHTML(),
        man.resources().dataDisplayHelp());
    intermineMenuItems = intermineItems;
    geneSetToolbar = makeGeneSetSelector();
    expressionTable = makeExpressionTable();
    expressionTable.setDisplayPColumns(false);
    // To ensure that GeneSetToolbar has chosenColumns
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

  protected GeneSetToolbar makeGeneSetSelector() {
    return new GeneSetToolbar(this) {
      @Override
      public void itemsChanged(List<String> items) {
        updateProbes();
      }
    };
  }

  protected ExpressionTable makeExpressionTable() {
    return new ExpressionTable(this, true, TableStyle.getStyle("default")) {
      @Override
      protected void onGettingExpressionFailed() {
        super.onGettingExpressionFailed();
        //If a non-loadable gene list was specified, we try with the blank list
        //(all probes for the species)
        if (chosenProbes.length > 0) {
          DataScreen.this.probesChanged(new String[0]);
          DataScreen.this.geneSetChanged(null);
          updateProbes();
        }   
      }
    };
  }

  static final public int STANDARD_TOOL_HEIGHT = 43;

  @Override
  protected void addToolbars() {
    super.addToolbars();
    HorizontalPanel mainTools = new HorizontalPanel();
    mainTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    mainTools.add(expressionTable.tools());
    mainTools.add(geneSetToolbar.selector());
    addToolbar(mainTools, STANDARD_TOOL_HEIGHT);
    addToolbar(expressionTable.analysisTools(), STANDARD_TOOL_HEIGHT);
  }

  @Override
  public Widget content() {
    setupMenuItems();

    ResizeLayoutPanel rlp = new ResizeLayoutPanel();

    rlp.setWidth("100%");
    rlp.add(expressionTable);
    return rlp;
  }

  private void setupMenuItems() {
    MenuBar menuBar = new MenuBar(true);
    MenuItem mActions = new MenuItem("File", false, menuBar);
    MenuItem mntmDownloadCsv =
        new MenuItem("Download CSV (grouped samples)...", false, () -> {          
            expressionTable.downloadCSV(false);
            Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_GROUPED_SAMPLES);
        });
    menuBar.addItem(mntmDownloadCsv);
    mntmDownloadCsv = new MenuItem("Download CSV (individual samples)...", false, () -> {      
        expressionTable.downloadCSV(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
            Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_INDIVIDUAL_SAMPLES);      
    });
    menuBar.addItem(mntmDownloadCsv);

    addMenu(mActions);

    menuBar = new MenuBar(true);
    for (final HideableColumn<ExpressionRow, ?> c : expressionTable.getHideableColumns()) {
    	final String title = c.columnInfo().title();
   
      hideableMenuItems.put(title, 
        //Automatically added to the menuBar
      new TickMenuItem(menuBar, title, c.visible()) {
        @Override
        public void stateChange(boolean newState) {
          expressionTable.setVisible(c, newState);
          if (newState) {
        	  Analytics.trackEvent(Analytics.CATEGORY_TABLE, 
        			  Analytics.ACTION_DISPLAY_OPTIONAL_COLUMN, title);
          }
        }
      });
    }

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
  }
  
  @Override
  public void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists) {
    itemListsChanged(itemLists);
    clusteringListsChanged(clusteringLists);
  }

  @Override
  public List<Sample> getAllSamples() {
    List<Sample> list = new ArrayList<Sample>();
    for (Group g : chosenColumns) {
      List<Sample> ss = Arrays.asList(g.getSamples());
      list.addAll(ss);
    }
    return list;
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
  
  protected void makeHeatMap() {
    HeatmapViewer.show(DataScreen.this, expressionTable.getValueType());
  }

  @Override
  public boolean enabled() {
    // return manager.isConfigured(ProbeScreen.key)
    // && manager.isConfigured(ColumnScreen.key);
    return manager.isConfigured(ColumnScreen.key);
  }

  /**
   * Trigger a data reload, if necessary.
   */
  public void updateProbes() {
    logger.info("chosenProbes: " + chosenProbes.length + " lastProbes: "
        + (lastProbes == null ? "null" : "" + lastProbes.length));

    if (chosenColumns.size() == 0) {
      Window.alert("Please define sample groups to see data.");
      manager().attemptProceed(ColumnScreen.key);
      return;
    }
    
    // Attempt to avoid reloading the data
    if (lastColumns == null || !chosenColumns.equals(lastColumns)) {
      logger.info("Data reloading needed");
      expressionTable.setStyle(getStyle(chosenColumns));
      expressionTable.getExpressions();      
    } else if (!Arrays.equals(chosenProbes, lastProbes)) {
      logger.info("Only refiltering is needed");
      expressionTable.refilterData();
    }

    lastProbes = chosenProbes;
    lastColumns = chosenColumns;
  }

  @Override
  public void show() {
    super.show();
    updateProbes();
  }
  
  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  private TableStyle getStyle(List<Group> columns) {
    return TableStyle.getStyle("default");
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
    expressionTable.probesChanged(probes);
  }

  public void geneSetChanged(ItemList geneSet) {
    chosenGeneSet = geneSet;
    getParser().storeGeneSet(geneSet);
    geneSetToolbar.geneSetChanged(geneSet);
  }

  public void columnsChanged(List<Group> columns) {
    chosenColumns = columns;
    expressionTable.columnsChanged(columns);
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
    String[] r = expressionTable.currentMatrixInfo().getAtomicProbes();
    if (r.length < expressionTable.currentMatrixInfo().numRows()) {
      Window.alert("Too many genes. Only the first " + r.length + " genes will be used.");
    }
    return r;
  }
  
  @Override
  public List<PersistedState<?>> getPersistedItems() {
    List<PersistedState<?>> r = new ArrayList<PersistedState<?>>();
    r.addAll(expressionTable.getPersistedItems());
    return r;
  }

  @Override
  public void loadPersistedState() {
    super.loadPersistedState();
    for (String title: hideableMenuItems.keySet()) {
      TickMenuItem mi = hideableMenuItems.get(title);
      boolean state = expressionTable.persistedVisibility(title, mi.getState());
      mi.setState(state);
    }
  }

  @Override
  public boolean importProbes(String[] probes) {
    if (Arrays.equals(probes, chosenProbes)) {
      return false;
    } else {
      probesChanged(probes);
      storeState();
      updateProbes();
      return true;
    }
  }

  @Override
  public boolean importColumns(List<Group> groups) {
    if (groups.size() > 0 && !groups.equals(chosenColumns)) {
      columnsChanged(groups);
      storeState();
      updateProbes();
      return true;
    } else {
      return false;
    }
  }
}
