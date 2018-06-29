"use strict";

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
   * @constructor
   * @param {String} title - the name given to a network
   * @default "empty"
   * @param {Array} interactions - the array of interactions that exist
   * between nodes
   * @param {Array} nodes - the nodes that comprise the newtwork
   */
  constructor(title="", interactions=[], nodes=[]){
    this.title = title;
    this.interactions = interactions;
    this.nodes = nodes;
  }

  /**
   * TO-DO
   * @param {Node} n
   * @return a list of all interactions in the network that start from the given
   * node N
   */
  intearctionsFrom(from){

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
    // else{ // update the node's information
    //   this.nodes[i].update(n);
    // }
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
    if( i === -1 ){
      return false;
    }
    this.nodes[i].update(n);
    // console.log("have to update: "+this.nodes[i].toString());
    // console.log("with this: "+n.toString());
  }

  /**
  *
  * @param {String} id
  * @param {int} x the x coordinate of the node
  * @param {int} y the y coordinate of the node
  * @return true when the node is part of the network and it's position could be
  * updated; false in any other case
  */
  updateNodePosition(id, x, y){

    var i = this.nodes.findIndex(o => o.id === id);
    if( i === -1 ){
      return false;
    }
    this.nodes[i].x = x;
    this.nodes[i].y = y;
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
  getCytoscapeElements(){
    var eles = [];
    // add node elements
    this.nodes.forEach(function(e){
      var c = (e.type === 'microRNA')? '#827f00' : '#007f7f';
      var node = {
        group: 'nodes',
        classes: e.type,
        data:{
          id: e.id,
          label: e.symbol,
          color: c
        }
      };
      if( e.x !== null ){
        node["position"] = { x:e.x, y:e.y };
      }
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
   * GetVisNodes()
   */
  getVisNodes(){
    var eles = [];
    this.nodes.forEach(function(e){
      var s = (e.type === 'microRNA')? 'box':'circle';
      eles.push({
        id: e.id,
        label: e.symbol,
        type: e.type,
        shape: s,
        x: e.x,
        y: e.y
      });
    });
    return eles;
  }

  /**
   * GetVisEdges()
   */
  getVisEdges(){
    var eles = [];
    this.interactions.forEach(function(e){
      eles.push({
        from: e.from,
        to: e.to
      });
    });
    return eles;
  }

  /**
   * exportJSON
   * The idea behind this function is to be able to write the structure of a
   * graph to a JSON file, that can be also be loaded to the application, thus
   * providing persistance to the work done by a researcher
   */
  exportJSON(){
    console.log("Network: "+JSON.stringify(this));
  }


} // class Network

function makeNetwork(title="", interactions=[], nodes=[]) {
  return new Network(title, interactions, nodes);
}
