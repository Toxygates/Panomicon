package t.viewer.client.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
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
    exportSaveNetwork();

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

    // Helper function to map a function over every element of a Java list and  
    // store the result in a JavaScript list.
    function mapJavaList(list, func) {
      var resultList = [];
      var itemCount = list.@java.util.List::size()();
      var i;
      for (i = 0; i < itemCount; i++) {
        var item = list.@java.util.List::get(I)(i);
        resultList.push(func(item));
      }
      return resultList;
    }

    var jsNodes = mapJavaList(
        network.@t.viewer.shared.network.Network::nodes()(),
        function(node) {
          var id = node.@t.viewer.shared.network.Node::id()();
          var symbols = mapJavaList(
              node.@t.viewer.shared.network.Node::symbols()(),
              function(symbol) {
                return symbol;
              });
          var type = node.@t.viewer.shared.network.Node::type()();
          if (type == 'miRNA') {
            type = 'microRNA';
          }

          var jsWeights = {};
          var weights = node.@t.viewer.shared.network.Node::weights()();
          var weightKeySet = weights.@java.util.HashMap::keySet()();
          var weightKeyIterator = weightKeySet.@java.util.Set::iterator()();
          while (weightKeyIterator.@java.util.Iterator::hasNext()()) {
            var key = weightKeyIterator.@java.util.Iterator::next()();
            var weight = weights.@java.util.HashMap::get(Ljava/lang/Object;)(key);
            jsWeights[key] = weight;
          }

          var newNode = new $wnd.makeNode(id, type, symbols);
          newNode.setWeights(jsWeights);
          return newNode;
        });

    var jsInteractions = mapJavaList(
        network.@t.viewer.shared.network.Network::interactions()(),
        function(interaction) {
          var label = interaction.@t.viewer.shared.network.Interaction::label()();
          var weight = interaction.@t.viewer.shared.network.Interaction::weight()();
          var from = interaction.@t.viewer.shared.network.Interaction::from()();
          var fromId = from.@t.viewer.shared.network.Node::id()();
          var to = interaction.@t.viewer.shared.network.Interaction::to()();
          var toId = to.@t.viewer.shared.network.Node::id()();
          return $wnd.makeInteraction(fromId, toId, label, weight);
        });

    $wnd.convertedNetwork = $wnd.makeNetwork(networkName, jsInteractions,
        jsNodes);
  }-*/;

  /**
   * Handles the logic for actually saving a network from the visualization dialog. Currently a
   * placeholder implementation for testing.
   * 
   * @param network the actual JavaScript object representing a network
   * @param jsonString
   */
  public native void saveNetwork(JavaScriptObject network) /*-{
    console.log("NetworkVisualizationDialog.saveNetwork called");
    console.log("Stringified network: \"" + JSON.stringify(network) + "\"");
    $wnd.savedNetwork = network;
  }-*/;

  /**
   * Exports the saveNetwork method to the window so that it can be called from hand-written
   * JavaScript.
   */
  public native void exportSaveNetwork() /*-{
    var that = this;
    $wnd.saveNetworkToToxygates = $entry(function(network, jsonString) {
      that.@t.viewer.client.network.NetworkVisualizationDialog::saveNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)(network);
    });
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
