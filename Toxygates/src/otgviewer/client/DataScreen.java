/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.sample.Group;
import t.viewer.client.*;
import t.viewer.client.components.DataView;
import t.viewer.client.table.TableView;
import t.viewer.shared.intermine.IntermineInstance;

/**
 * The main data display screen. Data is displayed in the ExpressionTable widget.
 */
public class DataScreen extends DLWScreen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected DataView dataView;

  protected String[] lastProbes;
  protected List<Group> lastColumns;

  // TODO: factor out heat map management logic + state
  // together with UIFactory.hasHeatMapMenu
  @Nullable
  private MenuItem heatMapMenu;

  public DataScreen(ScreenManager man) {
    super("View data", key, true, man, man.resources().dataDisplayHTML(),
        man.resources().dataDisplayHelp());
    geneSetToolbar = makeGeneSetSelector();    
    // To ensure that GeneSetToolbar has chosenColumns
    addListener(geneSetToolbar);
    dataView = makeDataView();
    addListener(dataView);
    setupMenuItems();
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

//  protected DataView pickDataView() {
//    String[] types = chosenColumns.stream().map(g -> GroupUtils.groupType(g)).distinct().
//        toArray(String[]::new);
//    DataView dataView = types.length >= 2 ? 
//        makeDualTableView() : makeTableView();    
//    
//    this.addListener(dataView);            
//    return dataView;
//  }
  
//  protected TableView makeDualTableView() {
//    //TODO need to override beforeGetAssociations?
//    return new DualTableView(this, mainTableTitle(),
//      mainTableSelectable());          
//  }

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
  

  protected void setupMenuItems() {
    for (MenuItem mi: dataView.topLevelMenus()) {
      addMenu(mi);
    }
    
    addAnalysisMenuItem(new MenuItem("Enrichment...", () -> runEnrichment(null)));   
    for (MenuItem mi: dataView.analysisMenuItems()) {
      addAnalysisMenuItem(mi);
    }

    GeneSetsMenuItem geneSetsMenu = factory().geneSetsMenuItem(this);
    addListener(geneSetsMenu);
    addMenu(geneSetsMenu.menuItem());        
  }
  
  public void runEnrichment(@Nullable IntermineInstance preferredInstance) {
    logger.info("Enrich " + DataScreen.this.displayedAtomicProbes().length + " ps");
    StringList genes = 
        new StringList(StringList.PROBES_LIST_TYPE, 
            "temp", DataScreen.this.displayedAtomicProbes());
    DataScreen.this.factory().enrichment(DataScreen.this, genes, preferredInstance);
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

  @Override
  public void probesChanged(String[] probes) {
    super.probesChanged(probes);
    logger.info("received " + probes.length + " probes");

    StorageParser p = getParser(this);
    storeProbes(p);

    lastProbes = null;
    lastColumns = null;
  }

  @Override
  public void geneSetChanged(ItemList geneSet) {
    super.geneSetChanged(geneSet);

    StorageParser p = getParser(this);
    storeGeneSet(p);
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
    boolean changed = super.importProbes(probes);
    if (changed) {
      reloadDataIfNeeded();
    }
    return changed;
  }

  @Override
  public boolean importColumns(List<Group> groups) {
    boolean changed = super.importColumns(groups);
    if (changed) {
      reloadDataIfNeeded();
    }
    return changed;
  }
}
