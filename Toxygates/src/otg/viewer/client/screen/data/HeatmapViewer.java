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

package otg.viewer.client.screen.data;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.ImportingScreen;
import t.clustering.shared.Algorithm;
import t.clustering.shared.ClusteringList;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.viewer.client.*;
import t.viewer.client.components.DataView;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.InputDialog;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.StringList;

/**
 * Adapts the HeatmapDialog for use inside otg.viewer.
 */
public class HeatmapViewer extends Composite {

  private ImportingScreen screen;
  private Logger logger;

  protected List<ClientGroup> chosenColumns = new ArrayList<ClientGroup>();
  protected String[] chosenProbes = new String[0];

  public HeatmapViewer(ImportingScreen screen) {
    this.screen = screen;
    logger = screen.getLogger();
  }

  public HeatmapDialog dialog(ValueType defaultType, String matrixId) {
    return new HeatmapDialog(screen.manager().matrixService(), defaultType,
        matrixId);
  }

  public class HeatmapDialog extends t.clustering.client.HeatmapDialog<Group, String> {
    final ImportingScreen screen = HeatmapViewer.this.screen;
    private ValueType defaultType;
    private Button saveButton, enrichButton;
    private final ListBox valType;
    private final MatrixServiceAsync matrixService;

    public HeatmapDialog(MatrixServiceAsync service, ValueType defaultType, 
        String matrixId) {
      super(matrixId, HeatmapViewer.this.logger, service);
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
      matrixService.prepareHeatmap(matrixId, columnsForClustering(),
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
      screen.factory().displayMultiEnrichmentDialog(screen, clusterLists.toArray(new StringList[0]), null);
    }

    protected void saveAsGeneSets() {
      save(getCurrent2DArray(), lastClusteringAlgorithm);
    }
    
    protected DialogBox inputDialog;
    
    public void save(List<Collection<String>> lists, Algorithm algorithm) {
      saveAction(lists, algorithm, "Name entry", "Please enter a name for the list.");
    }

    private void saveAction(final List<Collection<String>> lists, final Algorithm algorithm,
        String caption, String message) {
      final String type = ClusteringList.USER_CLUSTERING_TYPE;
      
      InputDialog entry = new InputDialog(message) {
        @Override
        protected void onChange(String name) {
          if (name == null) { // on Cancel clicked
            inputDialog.setVisible(false);
            return;
          }

          if (!screen.clusteringLists().validateNewObjectName(name, false)) {
            return;
          }

          List<String> names = generateNameList(name, lists.size());
          List<StringList> clusters = new ArrayList<StringList>();
          for (int i = 0; i < lists.size(); ++i) {
            clusters.add(new StringList(StringList.PROBES_LIST_TYPE, 
                names.get(i), lists.get(i).toArray(new String[0])));
          }

          ClusteringList cl =
              new ClusteringList(type, name, algorithm, clusters.toArray(new StringList[0]));
          
          screen.clusteringLists().put(name, cl);
          screen.clusteringListsChanged();
          inputDialog.setVisible(false);

          Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_SAVE_CLUSTERS);
          Window.alert("Clusters are successfully saved.");
        }
      };
      inputDialog = Utils.displayInPopup(caption, entry, DialogPosition.Center);
    }
    
    private List<String> generateNameList(String base, int size) {
      if (size > 1) {
        return getSerialNumberedNames(base, size);
      } else {
        return new ArrayList<String>(Arrays.asList(new String[] {base}));
      }
    }
    
    private List<String> getSerialNumberedNames(String base, int count) {
      List<String> names = new ArrayList<String>(count);
      for (int i = 0; i < count; ++i) {
        names.add(base + " " + (i + 1));
      }
      return names;
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
      return ClientGroup.convertToGroups(chosenColumns);
    }
  }

  public static void show(ImportingScreen screen, DataView view, ValueType defaultType) {
    HeatmapViewer viewer = new HeatmapViewer(screen);
    show(viewer, view, defaultType, DataScreen.defaultMatrix);
  }

  public static void show(HeatmapViewer viewer, DataView view, ValueType defaultType,
      String matrixId) {    
    viewer.chosenColumns = view.chosenColumns();
    viewer.chosenProbes = view.displayedAtomicProbes();

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
    HeatmapDialog dialog = viewer.dialog(defaultType, matrixId);
    dialog.initWindow();
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_SHOW_HEAT_MAP);
  }
}
