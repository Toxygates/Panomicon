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

package t.gwt.viewer.client.network;

import com.google.gwt.core.client.JavaScriptObject;

import t.shared.viewer.network.Network;

/**
 * Helper methods for converting Java networks to/from JavaScript networks.
 */
public class NetworkConversion {
  /**
   * Converts a Java network to a JavaScript network.
   */
  public static native JavaScriptObject convertNetworkToJS(Network network) /*-{
    // If there's already a stored JSON representation of the network, use that instead.
    var jsonString = network.@t.shared.viewer.network.Network::jsonString()();
    if (jsonString != "") {
      return @t.gwt.viewer.client.network.NetworkConversion::reanimateNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)(JSON.parse(jsonString));
    }

    var networkName = network.@t.shared.viewer.network.Network::title()();

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
      network.@t.shared.viewer.network.Network::nodes()(),
      function(node) {
        var id = node.@t.shared.viewer.network.Node::id()();
        var symbols = mapJavaList(
          node.@t.shared.viewer.network.Node::symbols()(),
          function(symbol) {
            return symbol;
          });
        var type = node.@t.shared.viewer.network.Node::type()();
        if (type == 'miRNA') {
          type = 'microRNA';
        }

        var jsWeights = {};
        var weights = node.@t.shared.viewer.network.Node::weights()();
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
      network.@t.shared.viewer.network.Network::interactions()(),
      function(interaction) {
        var label = interaction.@t.shared.viewer.network.Interaction::label()();
        var weight = interaction.@t.shared.viewer.network.Interaction::weight()();
        var from = interaction.@t.shared.viewer.network.Interaction::from()();
        var fromId = from.@t.shared.viewer.network.Node::id()();
        var to = interaction.@t.shared.viewer.network.Interaction::to()();
        var toId = to.@t.shared.viewer.network.Node::id()();
        return $wnd.makeInteraction(fromId, toId, label, weight);
    });
    
    var jsNetwork = $wnd.makeNetwork(networkName, jsInteractions, jsNodes);
    network.@t.shared.viewer.network.Network::storeJsonString(Ljava/lang/String;)(JSON.stringify(jsNetwork));
    
    return jsNetwork;
  }-*/;

  /**
   * Converts a Java network to a string by first converting it to a JavaScript
   * network, then converting it to JSON.
   */
  public static native String packNetwork(Network network) /*-{
    var jsonString = network.@t.shared.viewer.network.Network::jsonString()();
    if (jsonString != "") {
      return jsonString;
    }

    var network = @t.gwt.viewer.client.network.NetworkConversion::convertNetworkToJS(Lt/shared/viewer/network/Network;)(network);
    return JSON.stringify(network);
  }-*/;

  /**
   * Converts a JavaScript network to a Java network.
   */
  public static native Network convertNetworkToJava(JavaScriptObject network) /*-{
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
      var javaNode = @t.shared.viewer.network.Node::new(Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/util/HashMap;)(node.id, javaSymbols, node.type, javaWeights);
      javaNodes.@java.util.ArrayList::add(Ljava/lang/Object;)(javaNode);
      nodeDictionary[node.id] = javaNode;
    });
    
    var javaInteractions = @java.util.ArrayList::new(I)(network.interactions.length);
    network.interactions.forEach(function(interaction) {
      var from = nodeDictionary[interaction.from];
      var to =  nodeDictionary[interaction.to];
      var javaInteraction = @t.shared.viewer.network.Interaction::new(Lt/shared/viewer/network/Node;Lt/shared/viewer/network/Node;Ljava/lang/String;Ljava/lang/Double;)(from, to, interaction.label, interaction.weight);
      javaInteractions.@java.util.ArrayList::add(Ljava/lang/Object;)(javaInteraction);
    });
    
    var jsonString = JSON.stringify(network);
    
    return @t.shared.viewer.network.Network::new(Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/lang/String;)(network.title, javaNodes, javaInteractions, jsonString);
  }-*/;

  /**
   * Converts a JSON string into a JavaScript network.
   */
  public static native JavaScriptObject unpackToJavaScript(String packedString) /*-{
    return @t.gwt.viewer.client.network.NetworkConversion::reanimateNetwork(Lcom/google/gwt/core/client/JavaScriptObject;)(JSON.parse(packedString))
  }-*/;

  /**
   * Restores the prototypes of a network and its nodes and interactions, ensuring
   * that they have the appropriate class methods. Used on networks deserialized
   * from JSON to restore their functionality.
   */
  private static native JavaScriptObject reanimateNetwork(JavaScriptObject network) /*-{
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
