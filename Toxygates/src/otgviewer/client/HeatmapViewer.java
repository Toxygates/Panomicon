package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import t.clustering.shared.Algorithm;
import t.common.shared.ClusteringList;
import t.common.shared.StringList;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.MatrixServiceAsync;

/**
 * Adapts the HeatmapDialog for use inside otgviewer.
 */
public class HeatmapViewer extends DataListenerWidget {

  private Screen screen;
  public HeatmapViewer(Screen screen) {
    this.screen = screen;
  }

  public HeatmapDialog dialog(ValueType defaultType) {
    return new HeatmapDialog(screen.manager().matrixService(), defaultType);
  }

  public class HeatmapDialog extends t.clustering.client.HeatmapDialog<Group, String> {
    final Screen screen = HeatmapViewer.this.screen;
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
    screen.propagateTo(viewer);
    viewer.probesChanged(screen.displayedAtomicProbes());

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
