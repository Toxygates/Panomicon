package t.viewer.shared.network;

import java.io.Serializable;
import java.util.Optional;

@SuppressWarnings("serial")
public class Interaction implements Serializable {
  //GWT constructor
  Interaction() {}
  
  Optional<String> label;
  Optional<Double> weight;
  Node from, to;
  
  public Interaction(Node from, Node to, Optional<String> label, Optional<Double> weight) {
    this.label = label;
    this.weight = weight;
    this.from = from;
    this.to = to;
  }
  
  public Node from() { return from; }
  public Node to() { return to; }
  public Optional<String> label() { return label; }
  public Optional<Double> weight() { return weight; }
}
