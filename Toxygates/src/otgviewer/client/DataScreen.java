/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import otgviewer.client.components.GeneSetSelector;
import otgviewer.client.components.ListChooser;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.StorageParser;
import otgviewer.client.components.TickMenuItem;
import otgviewer.shared.Group;
import t.common.shared.ItemList;
import t.common.shared.sample.ExpressionRow;
import t.viewer.client.table.ExpressionTable;
import t.viewer.client.table.RichTable.HideableColumn;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The main data display screen. Data is displayed in the ExpressionTable widget.
 */
public class DataScreen extends Screen {

  public static final String key = "data";
  protected GeneSetSelector gs;
  protected ExpressionTable et;

  protected String[] lastProbes;
  protected List<Group> lastColumns;

  //TODO: factor out heat map management logic + state
  //together with UIFactory.hasHeatMapMenu
  @Nullable  
  private MenuItem heatMapMenu;

  public DataScreen(ScreenManager man) {
    super("View data", key, true, man, resources.dataDisplayHTML(), resources
        .dataDisplayHelp());
    gs = makeGeneSetSelector();
    et = makeExpressionTable();
    addListener(et);
    // To ensure that GeneSetSelector has chosenColumns
    addListener(gs);
  }

  protected GeneSetSelector makeGeneSetSelector() {
    return new GeneSetSelector(this);
  }

  protected ExpressionTable makeExpressionTable() {
    return new ExpressionTable(this, true);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    HorizontalPanel mainTools = new HorizontalPanel();
    mainTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    mainTools.add(et.tools());
    mainTools.add(gs.selector());
    addToolbar(mainTools, 43);
    addToolbar(et.analysisTools(), 43);
  }

  public Widget content() {
    setupMenuItems();

    ResizeLayoutPanel rlp = new ResizeLayoutPanel();

    rlp.setWidth("100%");
    rlp.add(et);
    return rlp;
  }

  private void setupMenuItems() {
    MenuBar mb = new MenuBar(true);
    MenuItem mActions = new MenuItem("File", false, mb);
    final DataScreen w = this;
    MenuItem mntmDownloadCsv =
        new MenuItem("Download CSV (grouped samples)...", false, new Command() {
          public void execute() {
            et.downloadCSV(false);

          }
        });
    mb.addItem(mntmDownloadCsv);
    mntmDownloadCsv =
        new MenuItem("Download CSV (individual samples)...", false,
            new Command() {
              public void execute() {
                et.downloadCSV(true);

              }
            });
    mb.addItem(mntmDownloadCsv);

    addMenu(mActions);

    mb = new MenuBar(true);
    for (final HideableColumn<ExpressionRow, ?> c : et.getHideableColumns()) {
      new TickMenuItem(mb, c.columnInfo().title(), c.visible()) {
        @Override
        public void stateChange(boolean newState) {
          et.setVisible(c, newState);
        }
      };
    }

    MenuItem mColumns = new MenuItem("View", false, mb);
    addMenu(mColumns);

    addAnalysisMenuItem(new MenuItem("Save visible genes as list...",
        new Command() {
          public void execute() {
            // Create an invisible listChooser that we exploit only for
            // the sake of saving a new list.
            ListChooser lc =
                new ListChooser(appInfo().predefinedProbeLists(), "probes") {
                  @Override
                  protected void listsChanged(List<ItemList> lists) {
                    w.itemListsChanged(lists);
                    w.storeItemLists(w.getParser());
                  }
                };
            // TODO make ListChooser use the DataListener propagate mechanism?
            lc.setLists(chosenItemLists);
            lc.setItems(Arrays.asList(et.displayedAtomicProbes()));
            lc.saveAction();
          }
        }));

    // TODO: this is effectively a tick menu item without the tick.
    // It would be nice to display the tick graphic, but then the textual alignment
    // of the other items on the menu becomes odd.
    addAnalysisMenuItem(new TickMenuItem("Compare two sample groups", false,
        false) {
      public void stateChange(boolean newState) {
        if (!visible) {
          // Trigger screen
          manager.attemptProceed(DataScreen.key);
          setState(true);
          showToolbar(et.analysisTools());
        } else {
          // Just toggle
          if (newState) {
            showToolbar(et.analysisTools());
          } else {
            hideToolbar(et.analysisTools());
          }
        }
      }
    }.menuItem());

    if (factory().hasHeatMapMenu()) {
      heatMapMenu = new MenuItem("Show heat map", new Command() {
        public void execute() {
          if (chosenProbes.length < 2) {
            Window.alert("Please choose at least 2 probes.");
          } else if (chosenProbes.length > 1000) {
            Window.alert("Please choose at most 1,000 probes.");
          } else if (chosenColumns.size() < 2) {
            Window.alert("Please choose at least 2 samples.");
          } else if (chosenColumns.size() > 1000) {
            Window.alert("Please choose at most 1,000 samples.");
          } else {
            new HeatmapDialog(DataScreen.this, et.getValueType());
          }
        }
      });
      heatMapMenu.setEnabled(false);
      addAnalysisMenuItem(heatMapMenu);
    }
  }

  @Override
  public boolean enabled() {
    // return manager.isConfigured(ProbeScreen.key)
    // && manager.isConfigured(ColumnScreen.key);
    return manager.isConfigured(ColumnScreen.key);
  }

  private void updateProbes() {
    logger.info("chosenProbes: " + chosenProbes.length + " lastProbes: "
        + (lastProbes == null ? "null" : "" + lastProbes.length));

    // Attempt to avoid reloading the data
    if (lastColumns == null || !chosenColumns.equals(lastColumns)) {
      logger.info("Data reloading needed");
      et.getExpressions();
    } else if (!Arrays.equals(chosenProbes, lastProbes)) {
      logger.info("Only refiltering is needed");
      et.refilterData();
    }

    lastProbes = chosenProbes;
    lastColumns = chosenColumns;
  }

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
  }

  @Override
  public void geneSetChanged(String geneSet) {
    super.geneSetChanged(geneSet);

    if (heatMapMenu != null) {
      heatMapMenu.setEnabled(!gs.isDefaultItemSelected());
    }

    StorageParser p = getParser(this);
    storeGeneSet(p);

    lastProbes = null;
    lastColumns = null;

    if (chosenGeneSet != null) {
      updateProbes();
    }
  }

}
