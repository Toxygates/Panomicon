package t.viewer.client.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.Resources;
import t.viewer.client.Utils;
import t.viewer.shared.network.Network;

public class NetworkVisualizationDialog {
  private static final String[] injectList = { "network-visualization/cytoscape-cose-bilkent.js",
      "network-visualization/cytoscape.min.js", "network-visualization/graphicNode.js",
      "network-visualization/interaction.js", "network-visualization/network.js", "network-visualization/node.js",
      "network-visualization/utils.js", "network-visualization/vis.min.js",
      "network-visualization/application.js" };

  protected DialogBox dialog;

  private Logger logger;

  private static Boolean injected = false;

  DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);

  private HandlerRegistration resizeHandler;

  HTML uiDiv = new HTML();

  public NetworkVisualizationDialog(Resources resources, Logger logger) {
    this.logger = logger;
    dialog = new DialogBox() {
      @Override
      protected void beginDragging(MouseDownEvent event) {
        event.preventDefault();
      };
    };
  }

  public void initWindow(@Nullable Network network) {
    createPanel();

    Utils.loadHTML(GWT.getModuleBaseURL() + "network-visualization/uiPanel.html", new Utils.HTMLCallback() {
      @Override
      protected void setHTML(String html) {
        uiDiv.setHTML(html);
        injectOnce(() -> {
          setupDockPanel();
          convertAndStoreNetwork(network);
          startVisualization();
        });
      }
    });

    dialog.show();
  }

  private void injectOnce(final Runnable callback) {
    if (!injected) {
      Utils.inject(new ArrayList<String>(Arrays.asList(injectList)), logger, callback);
      injected = true;
    } else {
      callback.run();
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

    dockPanel.setPixelSize(mainWidth(), mainHeight());

    resizeHandler = Window.addResizeHandler((ResizeEvent event) -> {
      dockPanel.setPixelSize(mainWidth(), mainHeight());
    });
    dialog.setWidget(dockPanel);
    dialog.center();
    dialog.setModal(true);
  }

  /**
   * Sets up the dock panel. Needs to happen later so that ui panel height can
   * be fetched from injected JavaScript.
   */
  private void setupDockPanel() {
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

    dockPanel.addNorth(uiDiv, getUiHeight());
    dockPanel.addSouth(buttonGroup, 27);

    SimplePanel displayPanel = new SimplePanel();
    displayPanel.setStyleName("visualization");
    displayPanel.getElement().setId("display");
    dockPanel.add(displayPanel);
  }

  private native int getUiHeight() /*-{
    return $wnd.uiHeight();
  }-*/;

  /**
   * Converts a Network.java object to a JavaScript Network object, and stores it
   * as window.convertedNetwork.
   */
  private static native void convertAndStoreNetwork(Network network) /*-{
    var networkName = network.@t.viewer.shared.network.Network::title()();

    var nodes = network.@t.viewer.shared.network.Network::nodes()();
    var nodeCount = nodes.@java.util.List::size()();

    var jsNodes = [];
    var i;
    for (i = 0; i < nodeCount; i++) {
      var node = nodes.@java.util.List::get(I)(i);
      var id = node.@t.viewer.shared.network.Node::id()();
      var symbol = node.@t.viewer.shared.network.Node::symbol()();
      var type = node.@t.viewer.shared.network.Node::type()();
      if (type == 'miRNA') {
        type = 'microRNA';
      }
      //var weight = node.@t.viewer.shared.network.Node::weight()();
      //      console.log("Node info: id = " + id + "; symbol = " + symbol
      //          + "; type = " + type + "; weight = " + weight);
      var newNode = new $wnd.makeNode(id, type, id);
      jsNodes.push(newNode);
    }

    var interactions = network.@t.viewer.shared.network.Network::interactions()();
    var interactionCount = interactions.@java.util.List::size()();

    var jsInteractions = [];
    var j;
    for (j = 0; j < interactionCount; j++) {
      var interaction = interactions.@java.util.List::get(I)(j);
      var label = interaction.@t.viewer.shared.network.Interaction::label()();
      var weight = interaction.@t.viewer.shared.network.Interaction::weight()();
      var from = interaction.@t.viewer.shared.network.Interaction::from()();
      var fromId = from.@t.viewer.shared.network.Node::id()();
      var to = interaction.@t.viewer.shared.network.Interaction::to()();
      var toId = to.@t.viewer.shared.network.Node::id()();
      //      console.log("Node info: label = " + label + "; weight = " + weight
      //          + "; from = " + from + "; to = " + to);
      var newInteraction = $wnd.makeInteraction(fromId, toId, label, weight);
      jsInteractions.push(newInteraction);
    }

    $wnd.convertedNetwork = $wnd.makeNetwork(networkName, jsInteractions,
        jsNodes);
  }-*/;

  /**
   * Called after UI HTML has been loaded, all scripts have been injected, and
   * network has been converted and stored in the JavaScript window, to inform the
   * visualization system that it should start drawing the network.
   */
  private native void startVisualization() /*-{
    $wnd.onReadyForVisualization();
  }-*/;
}
