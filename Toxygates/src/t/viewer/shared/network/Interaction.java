package t.viewer.shared.network;

import java.io.Serializable;
import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class Interaction implements Serializable {
  //GWT constructor
  Interaction() {}
  
  String label;
  Double weight;
  Node from, to;
  
  public Interaction(Node from, Node to, @Nullable String label, @Nullable Double weight) {
    this.label = label;
    this.weight = weight;
    this.from = from;
    this.to = to;
  }
  
  public Node from() { return from; }
  public Node to() { return to; }
  public String label() { return label; }
  public Double weight() { return weight; }
}
