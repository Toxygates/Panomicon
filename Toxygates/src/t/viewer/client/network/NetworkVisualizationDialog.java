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

import t.viewer.client.Utils;
import t.viewer.shared.network.Network;

public class NetworkVisualizationDialog {
  private static final String[] injectList = { "network-visualization/cytoscape-cose-bilkent.js",
      "network-visualization/cytoscape.min.js", "network-visualization/graphicNode.js",
      "network-visualization/interaction.js", "network-visualization/network.js", "network-visualization/node.js",
      "network-visualization/utils.js", "network-visualization/vis.min.js",
      "network-visualization/application.js" };

  protected DialogBox dialog;
  DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
  HTML uiDiv = new HTML();
  private HandlerRegistration resizeHandler;
  private Logger logger;
  private Delegate delegate;

  private static Boolean injected = false;

  public interface Delegate {
    void saveNetwork(Network network);
    //List<Network> networks();
  }

  public NetworkVisualizationDialog(Delegate delegate, Logger logger) {
    this.logger = logger;
    this.delegate = delegate;
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

    Button btnSave = new Button("Save and close");
    btnSave.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        saveCurrentNetwork();
        NetworkVisualizationDialog.this.dialog.hide();
        resizeHandler.removeHandler();
      }
    });
    buttonGroup.add(btnSave);

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
   * Converts a Java network to a JavaScript network , and stores it as
   * window.convertedNetwork.
   */
  private static native void convertAndStoreNetwork(Network network) /*-{
    $wnd.convertedNetwork = @t.viewer.client.network.NetworkVisualizationDialog::convertNetworkToJS(Lt/viewer/shared/network/Network;)(network);
  }-*/;

  /**
   * Converts a Java network to a JavaScript network.
   */
  private static native JavaScriptObject convertNetworkToJS(Network network) /*-{
    var jsonString = network.@t.viewer.shared.network.Network::jsonString()();
    if (jsonString != "") {
      return @t.viewer.client.network.NetworkVisualizationDialog::reanimateNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)(JSON.parse(jsonString));
    }

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

          var newNode = $wnd.makeNode(id, type, symbols);
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

    return $wnd.makeNetwork(networkName, jsInteractions, jsNodes);
  }-*/;

  /**
   * Converts a Java network to a string by first converting it to a JavaScript
   * network, then converting it to JSON.
   */
  public static native String packNetwork(Network network) /*-{
    var jsonString = network.@t.viewer.shared.network.Network::jsonString()();
    if (jsonString != "") {
      return jsonString;
    }

    var network = @t.viewer.client.network.NetworkVisualizationDialog::convertNetworkToJS(Lt/viewer/shared/network/Network;)(network);
    return JSON.stringify(network);
  }-*/;

  /**
   * Converts a JavaScript network to a Java network.
   */
  private static native Network convertNetworkToJava(JavaScriptObject network) /*-{
  var javaNodes = @java.util.ArrayList::new(I)(network.nodes.length);
  var nodeDictionary = {};
  network.nodes.forEach(function(node) {
    var javaWeights = @java.util.HashMap::new(I)(Object.keys(node.weight).length);
    Object.keys(node.weight).forEach(function(key) {
      //javaWeights.@java.util.HashMap::put(Ljava/lang/String;Ljava/lang/Double;)(key, node.weight[key]);
      javaWeights.@java.util.HashMap::put(Ljava/lang/Object;Ljava/lang/Object;)(key, node.weight[key]);
    });
    var javaSymbols = @java.util.ArrayList::new(I)(node.symbol.length);
    node.symbol.forEach(function(symbol) {
      javaSymbols.@java.util.ArrayList::add(Ljava/lang/Object;)(symbol);
    });
    var javaNode = @t.viewer.shared.network.Node::new(Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/util/HashMap;)(node.id, javaSymbols, node.type, javaWeights);
    javaNodes.@java.util.ArrayList::add(Ljava/lang/Object;)(javaNode);
    nodeDictionary[node.id] = javaNode;
  });
  
  var javaInteractions = @java.util.ArrayList::new(I)(network.interactions.length);
  network.interactions.forEach(function(interaction) {
    var from = nodeDictionary[interaction.from];
    var to =  nodeDictionary[interaction.to];
    var javaInteraction = @t.viewer.shared.network.Interaction::new(Lt/viewer/shared/network/Node;Lt/viewer/shared/network/Node;Ljava/lang/String;Ljava/lang/Double;)(from, to, interaction.label, interaction.weight);
    javaInteractions.@java.util.ArrayList::add(Ljava/lang/Object;)(javaInteraction);
  });
  
  var jsonString = JSON.stringify(network);
  
  return @t.viewer.shared.network.Network::new(Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/lang/String;)(network.title, javaNodes, javaInteractions, jsonString);
}-*/;

  public static native Network unpackNetwork(String packedString) /*-{
    var network = JSON.parse(packedString);
    return @t.viewer.client.network.NetworkVisualizationDialog::convertNetworkToJava(Lcom/google/gwt/core/client/JavaScriptObject;)(network);
  }-*/;

  /**
   * Restores the prototypes of a network and its nodes and interactions, ensuring
   * that they have the appropriate class methods. Used on networks deserialized
   * from JSON to restore their functionality.
   */
  public static native JavaScriptObject reanimateNetwork(JavaScriptObject network) /*-{
    Object.setPrototypeOf(network, $wnd.makeNetwork().__proto__);
    network.interactions.forEach(function(interaction) {
      Object.setPrototypeOf(interaction, $wnd.makeInteraction().__proto__);
    });
    network.nodes.forEach(function(node) {
      Object.setPrototypeOf(node, $wnd.makeNode().__proto__);
    });
    return network;
  }-*/;

  /**
   * Handles the logic for actually saving a network from the visualization dialog
   */
  public native void saveNetwork(JavaScriptObject network) /*-{
    var javaNetwork = @t.viewer.client.network.NetworkVisualizationDialog::convertNetworkToJava(Lcom/google/gwt/core/client/JavaScriptObject;)(network);
    var delegate = this.@t.viewer.client.network.NetworkVisualizationDialog::delegate;
    delegate.@t.viewer.client.network.NetworkVisualizationDialog.Delegate::saveNetwork(Lt/viewer/shared/network/Network;)(javaNetwork);
  }-*/;

  public native void saveCurrentNetwork() /*-{
    this.@t.viewer.client.network.NetworkVisualizationDialog::saveNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)($wnd.toxyNet);
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
