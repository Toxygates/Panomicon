package t.viewer.shared.network;

import java.io.Serializable;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;
import t.viewer.shared.AssociationValue;

@SuppressWarnings("serial")
public class Node implements Serializable {
  String id, type, symbol;
  double weight;
  
  //GWT constructor
  Node() {}
  

  public static Node fromRow(ExpressionRow row, String type) {
    return new Node(row.getProbe(), 
      SharedUtils.mkString(row.getGeneSyms(), "/"), type, row.getValue(0).getValue());        
  }
  
  public static Node fromAssociation(AssociationValue av, String type) {
    //Bogus node weight
    return new Node(av.formalIdentifier(), av.title(), type, 1.0);
  }
  
  public Node(String id, String symbol, String type, double weight) {
    this.id = id;
    this.symbol = symbol;
    this.type = type;
    this.weight = weight;
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof Node) {
      return id.equals(((Node)other).id);
    }
    return false;
  }
  
  public String id() { return id; }
  public String type() { return type; }
  public double weight() { return weight; }
  public String symbol() { return symbol; }
  
}
