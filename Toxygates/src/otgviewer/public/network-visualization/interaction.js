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
   * @param args space reserved for label and weight of an interaction, optional
   * parameter
   */
  constructor(from, to, ...args){
    this.from = from;
    this.to = to;
    this.label = args[0];
    this.weight = args[1];
  }

  /** setters and getters **/
  // get from(){ return this.from; }
  // get to(){ return this.to; }
  // get label(){ return this.label; }
  // get weight(){ return this.weight; }
  //
  // set from(f){ this._from = f; }
  // set to(t){ this._to = t; }
  // set label(l){ this._label = l; }
  // set weight(w){ this._weight = w; }
}
