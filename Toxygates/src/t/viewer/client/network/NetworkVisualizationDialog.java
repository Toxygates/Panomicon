package t.viewer.client.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.*;

import otgviewer.client.Resources;
import t.common.client.Utils;

public class NetworkVisualizationDialog {
  private static final String[] injectList = { "network-visualization/cytoscape-cose-bilkent.js",
      "network-visualization/cytoscape.min.js", "network-visualization/graphicNode.js",
      "network-visualization/interaction.js", "network-visualization/network.js", "network-visualization/node.js",
      "network-visualization/utils.js", "network-visualization/vis.min.js",
      "network-visualization/application.js" };

  protected DialogBox dialog;

  private Resources resources;
  private Logger logger;

  private Boolean injected = false;

  public NetworkVisualizationDialog(Resources resources, Logger logger) {
    this.resources = resources;
    this.logger = logger;
    dialog = new DialogBox();
  }

  public void initWindow() {
    createPanel();

    injectOnce();

    dialog.show();
  }

  private void injectOnce() {
    if (!injected) {
      Utils.inject(new ArrayList<String>(Arrays.asList(injectList)), logger, () -> {
      });
      injected = true;
    }
  }

  private void createPanel() {
    dialog.setText("Network visualization");

    VerticalPanel panel = new VerticalPanel();
    panel.setPixelSize(1000, 1000);
    HTML mapHtmlDiv = new HTML(resources.networkVisualizationHTML().getText());
    panel.add(mapHtmlDiv);

    dialog.setWidget(panel);
  }
}
