"use strict";

/**
 * Class that extends the definition of a Node, to include graphical properties,
 * such as the node position in a 2-dimensional plane, and its color.
 *
 * @author Rodolfo Allendes
 * @version 0.1
 */
class GraphicNode extends Node{

  /**
   * @constructor
   * @param id unique identifier for this node - string
   * @param type type of node, it might be good tl find a way to restrict the
   * value it can to a certain list, just in case we want to use an Enum type
   * in Java, but not sure yet
   * @param symbol gene symbol for a node
   */
  constructor(id, type, symbol, x=null, y=null){
    super(id, type, symbol);

    this.x = x;
    this.y = y;

  }

}
