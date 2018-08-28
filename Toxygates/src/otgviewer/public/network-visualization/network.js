"use strict";

/** types of nodes that can be added to the visualization */
const nodeType = Object.freeze({
  MSG_RNA: "mRNA",
  MICRO_RNA: "microRNA",
});

/** default colours for nodes in the graph */
const nodeColor = Object.freeze({
  MSG_RNA: "#007f7f",
  MICRO_RNA: "#827f00",
});

/** list of shapes that can be used to draw a node */
const nodeShape = Object.freeze({
  MSG_RNA: "ellipse",
  MICRO_RNA: "pentagon",
});

/**
 * Class that defines a Network <br>
 * Based on Toxygates Class Model <br>
 * title = String <br>
 * interactions = List&#60;Interaction> <br>
 * nodes = List&#60;Node>
 *
 * @author Rodolfo Allendes
 * @version 0.1
 */
class Network{

  /**
   * A network (or graph) is usually identified by a set of nodes and edges
   * (i.e. the connections between nodes). In addition to these two required
   * parameter, our model also considers a name (string) for the network.
   * The constructor initializes all the required parameters, using default
   * values when neccesary.
   * @param {String} title - the name given to a network
   * @param {[Interaction]} interactions - the array of interactions that exist
   * between nodes
   * @param {[ToxyNode]} nodes - the nodes that comprise the newtwork
   */
  constructor(title="", interactions=[], nodes=[]){
    this.title = title;
    this.interactions = interactions;
    this.nodes = nodes;
  }

  /**
   * TO-DO
   * @param {Node} from
   * @return a list of all interactions in the network that start from the given
   * node N
   */
  interactionsFrom(from){

  }

  /**
   * TO-DO
   * @param {Node} n
   * @return a list of all the interactions that have a destination the given
   * node N
   */
  interactionsTo(n){

  }


  /**
   * Given a node N, it searches the current list of nodes, based on ID and, if
   * not found, adds it to the list of nodes in the network.
   * @param n the node to be added
   * @return true if the node was added to the network, false in any other case
   */
  addNode(n){
    // check if the node is already in the network
    var i = this.nodes.findIndex(o => o.id === n.id);
    if( i === -1 ){
      this.nodes.push(n);
      return true;
    }
    return false;
  }

  /**
   * Given a node N, it searches the network for its location, and once found,
   * it calls the funcion of class Node to performe the actual update.
   * @param {Node} n
   * @return true when the node is part of the network and all the corresponding
   * fields have been updated, false in any other case (ex. when the node is not
   * part of the network)
   */
  updateNode(n){
    var i = this.nodes.findIndex(o => o.id === n.id);
    if( i !== -1 ){
      this.nodes[i].update(n);
      return true;
    }
    return false;
  }

 /**
  * Given an id and an (x,y) coordinate pair, it searches the network for the
  * corresponding node, and if found, it updates is position with the given
  * coordinates
  * @param {String} id
  * @param {int} x the x coordinate of the node
  * @param {int} y the y coordinate of the node
  * @return true when the node is part of the network and it's position could be
  * updated; false in any other case
  */
  updateNodePosition(id, x, y){
    var i = this.nodes.findIndex(o => o.id === id);
    if( i !== -1 ){
      this.nodes[i].x = x;
      this.nodes[i].y = y;
      return true;
    }
    return false;
  }

  /**
   * Given an interaction I, add the interaction to the network.
   * TO-DO does it make sense to look for duplicates before adding?
   * @param i the interaction to be added
   * @return true
   */
  addInteraction(i){
    this.interactions.push(i);
    return true;
  }

  /**
   * Convinience function used to retrieve all nodes and interactions of a
   * network, and return them in a format suitable for visualization using
   * Cytoscape.js
   * @return an array of elements, in the format required by cytoscape.js for
   * the display of a network graph
   */
  getCytoElements(){
    var eles = [];
    // add node elements
    this.nodes.forEach(function(e){
      // define the node attributes
      var label = (e.symbol.length > 0)? e.symbol[0] : e.id;
      var color = e.color;
      if( color === undefined ){
       color = (e.type === nodeType.MICRO_RNA )? nodeColor.MICRO_RNA : nodeColor.MSG_RNA ;
     }
      var shape = e.shape;
      if( shape === undefined ){
        shape = (e.type === nodeType.MICRO_RNA )? nodeShape.MICRO_RNA : nodeShape.MSG_RNA ;
      }
      // create the node object
      var node = {
        group: 'nodes',
        classes: e.type,
        data:{
          id: e.id,
          label: label,
          color: color,
          type: e.type,
          weight: e.weight,
        },
        style:{
          shape: shape,
        },
      };
      if( e.x !== null ){
        node["position"] = { x:e.x, y:e.y };
      }
      // add the node to the list of elements
      eles.push( node );
    }); // node elements

    // add interaction elements
    this.interactions.forEach(function(e){
      eles.push({
        group: 'edges',
        data:{
          id: e.from+e.to,
          source: e.from,
          target: e.to
        }
      });
    });
    return eles;

  } // getCytoscapeElements

  /**
   * The idea behind this function is to be able to write the structure of a
   * graph to a JSON file, that can be also be loaded to the application, thus
   * providing persistance to the work done by a researcher
   * @return a json structured string that represents the current network
   */
  exportJSON(){
    console.log("Network: "+JSON.stringify(this));
    return JSON.stringify(this);
  }

} // class Network

/**
 * Convenience function used to access the Node class from within GWT
 * @param {String} title - the name given to a network
 * @param {[Interaction]} interactions - the array of interactions that exist
 * between nodes
 * @param {[ToxyNode]} nodes - the nodes that comprise the newtwork
 @ return the newly created network object
 */
function makeNetwork(title="", interactions=[], nodes=[]){
  return new Network(title, interactions, nodes);
}
