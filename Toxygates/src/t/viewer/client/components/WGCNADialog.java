package t.viewer.client.components;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import t.clustering.shared.WGCNAParams;
import t.clustering.shared.WGCNAResults;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.screen.ImportingScreen;
import t.viewer.client.screen.data.ClusterSaveDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class WGCNADialog extends InteractionDialog {

    InputGrid ig;
    WGCNAResults results;
    Button dendrogram, modules, softThreshold, sampleClustering, saveClusters;
    String matrixId;
    List<Group> groups;
    ValueType valueType;
    ImportingScreen screen;

    public WGCNADialog(ImportingScreen parent, String matrixId, List<Group> groups,
                       ValueType valueType) {
        super(parent);
        this.screen = parent;
        this.matrixId = matrixId;
        this.groups = groups;
        this.valueType = valueType;
    }

    @Override
    protected Widget content() {
        VerticalPanel vp = new VerticalPanel();

        ig = new InputGrid(3, "10em", "Dendrogram cutoff", "Soft power");
        ig.setValue(0, "50");
        ig.setValue(1, "10");
        vp.add(ig);
        ig.setWidth("450px");

        dendrogram = new Button("Show trait dendrogram...", (ClickEvent e) -> showDendrogram());
        modules = new Button("Show module dendrogram...", (ClickEvent e) -> showModules());
        sampleClustering = new Button("Show clustering...", (ClickEvent e) -> showClustering());
        softThreshold = new Button("Show soft threshold...", (ClickEvent e) -> showThreshold());
        saveClusters = new Button("Save clusters...", (ClickEvent e) -> saveClusters());

        ig.getGrid().setWidget(0, 2, sampleClustering);
        ig.getGrid().setWidget(1, 2, softThreshold);

        setEnabled(false);
        vp.add(Utils.wideCentered(Utils.mkHorizontalPanel(true, dendrogram, modules, saveClusters)));

        Button go = new Button("Run", (ClickEvent e) -> {
            userProceed();
        });

        Button close = new Button("Close", cancelHandler());
        Panel buttons = Utils.mkHorizontalPanel(true, go, close);
        buttons.setHeight("50px");
        vp.add(buttons);
        return vp;
    }

    void setEnabled(boolean enabled) {
        dendrogram.setEnabled(enabled);
        modules.setEnabled(enabled);
        sampleClustering.setEnabled(enabled);
        softThreshold.setEnabled(enabled);
        saveClusters.setEnabled(enabled);
    }

    void showDendrogram() {
        Utils.loadImageInPopup("Sample dendrogram", results.getDendrogramTraitsImage());
    }

    void showModules() {
        Utils.loadImageInPopup("Modules", results.getModulesImage());
    }

    void showClustering() {
        Utils.loadImageInPopup("Sample clustering", results.getSampleClusteringImage());
    }

    void showThreshold() {
        Utils.loadImageInPopup("Soft power threshold", results.getSoftThresholdImage());
    }

    void saveClusters() {
        List<Collection<String>> clusters = new ArrayList<>();
        List<String> suffixes = new ArrayList<>();
        for (int i = 0; i < results.getClusters().length; i++) {
            clusters.add(Arrays.asList(results.getClusters()[i]));
            suffixes.add(results.getClusterTitles()[i]);
        }
        ClusterSaveDialog.saveAction(screen, clusters, suffixes, null,
                "Name entry", "Please enter a name for the clusters.");
    }

    @Override
    protected void userProceed() {
        int cutoff = Integer.parseInt(ig.getValue(0));
        int softPower = Integer.parseInt(ig.getValue(1));
        WGCNAParams params = new WGCNAParams(cutoff, softPower);
        parent.manager().matrixService().prepareWGCNAClustering(
                params, matrixId,
                groups, null,
                valueType, new PendingAsyncCallback<WGCNAResults>(parent.manager(),
                        "Unable to perform WGCNA clustering") {
                    @Override
                    public void handleSuccess(WGCNAResults results) {
                        WGCNADialog.this.results = results;
                        Window.alert("OK");
                        setEnabled(true);
                    }

                    @Override
                    public void handleFailure(Throwable caught) {
                        super.handleFailure(caught);
                        setEnabled(false);
                    }
                });
    }
}
