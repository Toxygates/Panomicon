"use strict";

const nodeType = Object.freeze({
  MSG_RNA: 1,
  MICRO_RNA: 2
});


/**
 * Class that defines a Node <br>
 * Based on Toxygates Class Model <br>
 * id = String <br>
 * type = List&#60;Interaction> <br>
 * symbol = List&#60;Node>
 *
 * @author Rodolfo Allendes
 * @version 0.1
 */
class Node{

  /**
   * Constructor
   * @param id unique identifier for this node - string
   * @param type type of node, it might be good tl find a way to restrict the
   * value it can to a certain list, just in case we want to use an Enum type
   * in Java, but not sure yet
   * @param symbol gene symbol for a node
   */
  constructor(id, type, symbol){
    this.id = id;
    this.type = type;
    this.symbol = symbol;
    this.weight = {}; // class

  }

  /**  setters and getters **/
  // get id(){ return this.id; }
  // get type(){ return this.type; }
  // get symbol(){ return this.symbol; }
  // get weight(){ return this.weight; }
  //
  // set id(i){ this._id = i; }
  // set type(t){ this._type = t; }
  // set symbol(s){ this._symbol = s; }
  // set weight(w){ this._weight = w; }

  /**
   * addWeight
   * Search for a specific weight, within the node definition, and it updates
   * the corresponding value. If the key value is not found, then a new weight
   * is added to the list
   * @param label the label used for the specific weight component
   * @param value the value for the specified weight component
   */
  addWeight(label, value){
     this.weight[label] = value;
   }
  
  setWeights(weights) {
    this.weight = weights;
  }

   /**
    *
    */
  update(n){
    console.log("entre a update");
    var k = Object.keys(n.weight);
    console.log(k.length);
    for(var i=0; i < k.length; ++i ){
      this.weight[k[i]] = n.weight[k[i]];
    }
    // console.log(this);
  }

  toString(){
    return this.id+" "+this.weight.toString();
  }

}

// Needed for now because I can't figure out how to access the Node class from within GWT. 
function makeNode(id, type, symbol) {
  return new Node(id, type, symbol);
}
