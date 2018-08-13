"use strict";

/**
 * Class that defines the structure of the Nodes that make up a Toxygates
 * network <br>
 * A node will have the following parameters, based on the original Toxygates
 * Class Model <br>
 * id = String - a unique identifier for the node - non empty<br>
 * type = nodeType - the type of node, from the list of available options <br>
 * symbols = [String] - a list of all the symbols used as names for the node <br>
 * weights = {string: float} - a set of numerical attributes for the node,
 * usually associated to gene expression
 *
 * @author Rodolfo Allendes
 * @version 0.1
 */
class ToxyNode{

  /**
   * Constructor
   * @param {string} id: unique identifier for this node - non empty
   * @param {nodeType} type: type of node, currently limited to messengerRNA and
   * microRNA
   * @param {[string]} symbol: list of gene symbols used to identify a node
   * @param {{string: float}}weight: set of numerical attributes associated to
   * a node
   */
  constructor(id, type, symbol){
    this.id = id;
    this.type = type;
    this.symbol = symbol;
    this.weight = {};
  }

  /**
   *
   */
  setWeights(weights){
    this.weight = weights;
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

} // class ToxyNode

/**
 * Convenience function used to access the Node class from within GWT
 * @param {string} id: the id of the node
 * @param {nodeType} type: the type given to the node
 * @param {[string]} symbols: a list of symbols used to identify the node
 * @return the newly created Node
 */
function makeNode(id, type, symbols){
  return new ToxyNode(id, type, symbols);
}
