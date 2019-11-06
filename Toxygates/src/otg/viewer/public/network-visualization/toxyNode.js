"use strict";

/**
 * @class ToxyNode
 * @classdesc Define the structure of nodes within a toxygates graph.
 * @author Rodolfo Allendes
 * @version 0.1
 *
 * @param {String} id A unique identifier for th nodes
 * @param {String} type The type of node. Takes one of two values: 'msgRNA' or
 * 'microRNA'
 * @param {Array<String>} symbol List of gene symbols used to identify a node.
 *
 * @property {{String: float}} weight Set of numerical attributes associated to
 * a node. Usually related to expression or p values.
 * @property {int} x the x coordinate (in pixels) that represents the point in
 * the display where the center of the node is located
 * @property {int} y the y coordinate (in pixels) that represents the point in
 * the display where the center of the node is located
 * @property {String} shape indicates the shape (within the list of possibilities
 * given by cytoscape) used to display the node
 * @property {String} color a string, in RGB Hex format, used to store the
 * background color that should be used to draw the node
 */
class ToxyNode{

  constructor(id, type, symbol){
    this.id = id;
    this.type = type;
    this.symbol = symbol;
    this.weight = {};

    // visual properties of a node
    this.x = undefined;  // x coordinate (in pixels) - the location of the node
    this.y = undefined; // y coordinate (in pixels) - the location of the node
    this.shape = undefined; // the shape used to draw the node
    this.color = undefined; // background color of the node
    this.borderColor = undefined; // color used for the border of the node
  }

  /**
   * Update the current list of weights associated with the node for the one
   * provided as parameter
   */
  setWeights(weights){
    this.weight = weights;
  }

  /**
   * Set the position of the node
   * @param {number} x
   * @param {number} y
   */
  setPosition(x, y){
    this.x = x;
    this.y = y;
  }

  /**
   * Search for a specific weight, within the node definition, and it updates
   * the corresponding value. If the key value is not found, then a new weight
   * is added to the list
   * @param {string} label: the label used for the specific weight component
   * @param {float} value: the value for the specified weight component
   */
  addWeight(label, value){
     this.weight[label] = value;
   }

   /**
    * Given a dummy node object, it updates the current node's weight list with
    * the values provided through the dummy object. If a weight listed in the
    * dummy exists in the list of weights for the current node, then the value
    * is merely updated, otherwise, a new {key: value} pair is added to the
    * current node
    * @param {Node} n dummy object used to update a list of weight values at the
    * same time
    */
  update(n){
    var k = Object.keys(n.weights);
    for(var i=0; i < k.length; ++i ){
      this.weight[k[i]] = n.weight[k[i]];
    }
  }

  /**
   * Creates and returns a string representation for the node. This
   * representation includes the id of the node and the list of weights
   * associated to the node
   * @return the string representation for the node
   */
  toString(){
    return this.id+" "+this.weight.toString();
  }

  /**
   *
   */
  toJSON(){
    return{
      id: this.id,
      type: this.type,
      symbol: this.symbol,
      weight: this.weight,

      // visual properties of a node
      x: this.x,
      y: this.y,
      shape: this.shape,
      color: this.color,
      borderColor: this.borderColor,
    }
  }

} // class ToxyNode

/**
 * Convenience function used to access the Node class from within GWT
 * @param {string} id: the id of the node
 * @param {nodeType} type: the type given to the node
 * @param {Array<string>} symbols: a list of symbols used to identify the node
 * @return the newly created Node
 */
function makeNode(id, type, symbols){
  return new ToxyNode(id, type, symbols);
}
