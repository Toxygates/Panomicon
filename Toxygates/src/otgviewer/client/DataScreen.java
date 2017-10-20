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

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.components.*;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.table.*;
import t.viewer.client.table.RichTable.HideableColumn;
import t.viewer.shared.intermine.IntermineInstance;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

/**
 * The main data display screen. Data is displayed in the ExpressionTable widget.
 */
public class DataScreen extends Screen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected ExpressionTable expressionTable;

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
    expressionTable = makeExpressionTable();
    expressionTable.setDisplayPColumns(false);
    addListener(expressionTable);
    // To ensure that GeneSetToolbar has chosenColumns
    addListener(geneSetToolbar);
  }

  protected GeneSetToolbar makeGeneSetSelector() {
    return new GeneSetToolbar(this) {
      @Override
      public void itemsChanged(List<String> items) {
        updateProbes();
      }
    };
  }
  
  protected static final String defaultMatrix = "DEFAULT";

  protected ExpressionTable makeExpressionTable() {
    return new ExpressionTable(this, true, TableStyle.getStyle("default"),
        defaultMatrix) {
      @Override
      protected void onGettingExpressionFailed() {
        super.onGettingExpressionFailed();
        DataScreen.this.probesChanged(new String[0]);
        DataScreen.this.geneSetChanged(null);
        updateProbes();
      }
      
      @Override
      protected void associationsUpdated() {
        DataScreen.this.associationsUpdated();
      }
    };
  }
  
  protected void associationsUpdated() {}
  
  protected TableStyle styleForColumns(List<Group> columns) {
    boolean foundMirna = false;
    boolean foundNonMirna = false;
    for (Group g: chosenColumns) {
      if (GroupUtils.isMirnaGroup(g)) {      
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
    return mainTablePanel();
  }
  
  protected Widget mainTablePanel() {
    ResizeLayoutPanel rlp = new ResizeLayoutPanel();
    rlp.setWidth("100%");
    rlp.add(expressionTable);
    return rlp;
  }

  private void setupMenuItems() {
    MenuBar menuBar = new MenuBar(true);
    MenuItem mActions = new MenuItem("File", false, menuBar);
    MenuItem mntmDownloadCsv =
        new MenuItem("Download CSV (grouped samples)...", false, new Command() {
          @Override
          public void execute() {
            expressionTable.downloadCSV(false);
            Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_GROUPED_SAMPLES);
          }
        });
    menuBar.addItem(mntmDownloadCsv);
    mntmDownloadCsv = new MenuItem("Download CSV (individual samples)...", false, new Command() {
      @Override
      public void execute() {
        expressionTable.downloadCSV(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
            Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_INDIVIDUAL_SAMPLES);
      }
    });
    menuBar.addItem(mntmDownloadCsv);

    addMenu(mActions);

    menuBar = new MenuBar(true);
    //TODO store the TickMenuItem in HideableColumn so that the state can be synchronised
    for (final HideableColumn<ExpressionRow, ?> c : expressionTable.getHideableColumns()) {
    	final String title = c.columnInfo().title();
      new TickMenuItem(menuBar, title, c.visible()) {
        @Override
        public void stateChange(boolean newState) {
          expressionTable.setVisible(c, newState);
          if (newState) {
        	  Analytics.trackEvent(Analytics.CATEGORY_TABLE, 
        			  Analytics.ACTION_DISPLAY_OPTIONAL_COLUMN, title);
          }
        }
      };
    }

    GeneSetsMenuItem geneSetsMenu = factory().geneSetsMenuItem(this);
    addListener(geneSetsMenu);
    addMenu(geneSetsMenu.menuItem());

    MenuItem mColumns = new MenuItem("View", false, menuBar);
    addMenu(mColumns);

    // TODO: this is effectively a tick menu item without the tick.
    // It would be nice to display the tick graphic, but then the textual alignment
    // of the other items on the menu becomes odd.
    addAnalysisMenuItem(new TickMenuItem("Compare two sample groups", false, false) {
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

    addAnalysisMenuItem(new MenuItem("Enrichment...", new Command() {
      @Override
      public void execute() {
        runEnrichment(null);
      }
    }));
    
    if (factory().hasHeatMapMenu()) {
      heatMapMenu = new MenuItem("Show heat map", new Command() {
        @Override
        public void execute() {
          makeHeatMap();          
        }
      });
      addAnalysisMenuItem(heatMapMenu);
    }
  }
  
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
      expressionTable.setStyle(styleForColumns(chosenColumns));
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
    String[] r = expressionTable.currentMatrixInfo().getAtomicProbes();
    if (r.length < expressionTable.currentMatrixInfo().numRows()) {
      Window.alert("Too many genes. Only the first " + r.length + " genes will be used.");
    }
    return r;
  }

}
