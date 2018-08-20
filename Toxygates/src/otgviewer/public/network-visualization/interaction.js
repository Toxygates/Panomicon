"use strict";

/**
 * Original structure used for Togygates interactions
 * String label
 * double weight
 * Node from, to - using the string that represents the id of each node instead
 */
class Interaction{

  /**
   * Constructor
   * @param from starting node for an interaction
   * @param to ending node for an interaction
   * @param {string} label the label of the interaction
   * @param {{any}} weight a list of weights associated to the interaction
   */
  constructor(from, to, label=null, weight=null){
    this.from = from;
    this.to = to;
    this.label = label;
    this.weight = weight;
  }

} // class Interaction

/**
 * Convenience function used to access the Interaction class from within GWT
 * @param {node} from: in a directed graph, the node of origin for the edge
 * @param {node} to: in a directed graph, the ending node for the edge
 * @param {string} label: a single string that identifies the edge being defined
 * @param {string} weight: a singe string associated to a property of the edge
 * @return the newly created Node
 */
function makeInteraction(from, to, label, weight){
  return new Interaction(from, to, label, weight)
}
