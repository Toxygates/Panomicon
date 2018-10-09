package t.viewer.client.network;

import com.google.gwt.core.client.JavaScriptObject;

import t.viewer.shared.network.Network;

/**
 * Helper methods for converting Java networks to/from JavaScript networks.
 */
public class NetworkConversion {
  /**
   * Converts a Java network to a JavaScript network.
   */
  private static native JavaScriptObject convertNetworkToJS(Network network) /*-{
    // If there's already a stored JSON representation of the network, use that instead.
    var jsonString = network.@t.viewer.shared.network.Network::jsonString()();
    if (jsonString != "") {
      return @t.viewer.client.network.NetworkConversion::reanimateNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)(JSON.parse(jsonString));
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

    var network = @t.viewer.client.network.NetworkConversion::convertNetworkToJS(Lt/viewer/shared/network/Network;)(network);
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
    return @t.viewer.client.network.NetworkConversion::convertNetworkToJava(Lcom/google/gwt/core/client/JavaScriptObject;)(network);
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
}
