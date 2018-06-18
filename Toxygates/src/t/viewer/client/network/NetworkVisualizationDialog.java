package t.viewer.client.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
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

  private HandlerRegistration resizeHandler;

  public NetworkVisualizationDialog(Resources resources, Logger logger) {
    this.resources = resources;
    this.logger = logger;
    dialog = new DialogBox() {
      @Override
      protected void beginDragging(MouseDownEvent event) {
        event.preventDefault();
      };
    };
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

  protected int mainWidth() {
    return Window.getClientWidth() - 44;
  }

  protected int mainHeight() {
    return Window.getClientHeight() - 62;
  }

  private void createPanel() {
    dialog.setText("Network visualization");

    DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
    dockPanel.setPixelSize(mainWidth(), mainHeight());

    // VerticalPanel vPanel = new VerticalPanel();
    // vPanel.setPixelSize(mainWidth(), mainHeight());
    HTML mapHtmlDiv = new HTML(resources.networkVisualizationHTML().getText());
    // vPanel.add(mapHtmlDiv);

    FlowPanel buttonGroup = new FlowPanel();
    Button btnClose = new Button("Close");
    btnClose.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        NetworkVisualizationDialog.this.dialog.hide();
        resizeHandler.removeHandler();
      }
    });
    buttonGroup.add(btnClose);

    dockPanel.addNorth(mapHtmlDiv, 365);
    dockPanel.addSouth(buttonGroup, 27);

    SimplePanel displayPanel = new SimplePanel();
    displayPanel.setStyleName("visualization");
    displayPanel.getElement().setId("display");
    dockPanel.add(displayPanel);

    resizeHandler = Window.addResizeHandler((ResizeEvent event) -> {
      dockPanel.setPixelSize(mainWidth(), mainHeight());
    });
    dialog.setWidget(dockPanel);
    dialog.center();
    dialog.setModal(true);
  }
}
