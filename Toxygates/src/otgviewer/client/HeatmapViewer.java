/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package otgviewer.client;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import t.clustering.shared.Algorithm;
import t.common.shared.*;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.MatrixServiceAsync;

/**
 * Adapts the HeatmapDialog for use inside otgviewer.
 */
public class HeatmapViewer extends Composite {

  private DataScreen screen;
  private Logger logger;

  protected List<Group> chosenColumns = new ArrayList<Group>();
  protected String[] chosenProbes = new String[0];

  public HeatmapViewer(DataScreen screen) {
    this.screen = screen;
    logger = screen.getLogger();
  }

  public HeatmapDialog dialog(ValueType defaultType) {
    return new HeatmapDialog(screen.manager().matrixService(), defaultType);
  }

  public class HeatmapDialog extends t.clustering.client.HeatmapDialog<Group, String> {
    final DataScreen screen = HeatmapViewer.this.screen;
    private ValueType defaultType;
    private Button saveButton, enrichButton;
    private final ListBox valType;
    private final MatrixServiceAsync matrixService;

    public HeatmapDialog(MatrixServiceAsync service, ValueType defaultType) {
      super(HeatmapViewer.this.logger, service);
      this.matrixService = service;
      this.defaultType = defaultType;
      valType = new ListBox();
    }

    @Override
    protected void addButtons(FlowPanel buttonGroup) {
      enrichButton = new Button("Enrichment...");
      enrichButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          doEnrichment();
        }
      });
      buttonGroup.add(enrichButton);
      enrichButton.setEnabled(false);

      saveButton = new Button("Save as gene set...", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          saveAsGeneSets();
        }
      });
      buttonGroup.add(saveButton);
      super.addButtons(buttonGroup);
    }

    @Override
    protected void guiStateChanged(boolean enabled) {
      super.guiStateChanged(enabled);
      enrichButton.setEnabled(enabled);
      saveButton.setEnabled(enabled);
    }

    @Override
    protected void doClustering(Algorithm algo) {
      matrixService.prepareHeatmap(columnsForClustering(),
        rowsForClustering(), getValueType(), algo,
          HeatmapDialog.HEATMAP_TOOLTIP_DECIMAL_DIGITS, prepareHeatmapCallback());
    }

    protected void doEnrichment() {
      List<StringList> clusterLists = new ArrayList<StringList>();
      int i = 0;
      for (Collection<String> clust : getCurrent2DArray()) {
        StringList sl = new StringList(StringList.PROBES_LIST_TYPE,
            "Cluster " + i, clust.toArray(new String[0]));
        clusterLists.add(sl);
        i++;
      }
      screen.factory().multiEnrichment(screen, clusterLists.toArray(new StringList[0]), null);
    }

    protected void saveAsGeneSets() {
      ClusteringListsStoreHelper helper =
          new ClusteringListsStoreHelper(ClusteringList.USER_CLUSTERING_TYPE, screen) {
        @Override
        protected void onSaveSuccess(String name, ClusteringList items) {
              Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_SAVE_CLUSTERS);
          Window.alert("Clusters are successfully saved.");
        }
      };
      helper.save(getCurrent2DArray(), lastClusteringAlgorithm);
    }

    @Override
    protected void addTopContent(HorizontalPanel topContent) {
      super.addTopContent(topContent);
      topContent.add(new Label("Value:"));

      for (ValueType v : ValueType.values()) {
        valType.addItem(v.toString());
      }
      valType.setSelectedIndex(defaultType.ordinal());
      valType.addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          recluster();
        }
      });
      topContent.add(valType);
    }

    private ValueType getValueType() {
      String vt = valType.getItemText(valType.getSelectedIndex());
      return ValueType.unpack(vt);
    }

    @Override
    protected List<String> rowsForClustering() {
      return Arrays.asList(chosenProbes);
    }

    @Override
    protected List<Group> columnsForClustering() {
      return chosenColumns;
    }
  }

  public static void show(DataScreen screen, ValueType defaultType) {
    HeatmapViewer viewer = new HeatmapViewer(screen);
    show(viewer, screen, defaultType);
  }

  public static void show(HeatmapViewer viewer, DataScreen screen, ValueType defaultType) {
    viewer.chosenColumns = screen.chosenColumns();
    viewer.chosenProbes = screen.displayedAtomicProbes();

    int probesCount = (viewer.chosenProbes != null ? viewer.chosenProbes.length : 0);
    if (probesCount == 0 || probesCount > 1000) {
      Window.alert("Please choose at most 1,000 probes.");
      return;
    }
    if (probesCount < 2) {
      Window.alert("Please choose at least 2 probes.");
      return;
    }
    int columnsCount = viewer.chosenColumns.size();
    if (columnsCount < 2) {
      Window.alert("Please define at least 2 columns.");
      return;
    }
    if (columnsCount > 1000) {
      Window.alert("Please define at most 1,000 columns.");
      return;
    }

    // all checks passed
    HeatmapDialog dialog = viewer.dialog(defaultType);
    dialog.initWindow();
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_SHOW_HEAT_MAP);
  }
}
