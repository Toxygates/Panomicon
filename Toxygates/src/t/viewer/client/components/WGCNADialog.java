package t.viewer.client.components;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import t.clustering.shared.WGCNAParams;
import t.clustering.shared.WGCNAResults;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.screen.Screen;

import java.util.List;

public class WGCNADialog extends InteractionDialog {

    InputGrid ig;
    WGCNAResults results;
    Button dendrogram, modules;
    String matrixId;
    List<Group> groups;
    ValueType valueType;

    public WGCNADialog(Screen parent, String matrixId, List<Group> groups,
                       ValueType valueType) {
        super(parent);
        this.matrixId = matrixId;
        this.groups = groups;
        this.valueType = valueType;
    }

    @Override
    protected Widget content() {
        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("200px");

        ig = new InputGrid("Dendrogram cutoff", "Soft power");
        ig.setValue(0, "50");
        ig.setValue(1, "10");
        vp.add(ig);

        dendrogram = new Button("Show dendrogram...", (ClickEvent e) -> showDendrogram());
        modules = new Button("Show modules...", (ClickEvent e) -> showModules());
        dendrogram.setEnabled(false);
        modules.setEnabled(false);
        vp.add(Utils.wideCentered(Utils.mkHorizontalPanel(true, dendrogram, modules)));

        SimplePanel sp = new SimplePanel();
        sp.setHeight("50px");
        vp.add(sp);

        Button go = new Button("Run", (ClickEvent e) -> {
            userProceed();
        });

        Button close = new Button("Close", cancelHandler());
        vp.add(Utils.mkHorizontalPanel(true, go, close));
        return vp;
    }

    void showDendrogram() {
        if (results != null) {
            Utils.loadImageInPopup("Sample dendrogram...", results.getDendrogramImage());
        }
    }

    void showModules() {
        if (results != null) {
            Utils.loadImageInPopup("Modules...", results.getModulesImage());
        }
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
                        dendrogram.setEnabled(true);
                        modules.setEnabled(true);
                        WGCNADialog.this.results = results;
                        Window.alert("OK");
                    }
                });
    }
}
