package t.viewer.shared.network;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Node implements Serializable {
  String id;
  String type;
  double weight;
  
  //GWT constructor
  Node() {}
  
  public Node(String id, String type, double weight) {
    this.id = id;
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
}
