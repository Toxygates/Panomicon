package t.viewer.shared.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An interaction network.
 *
 */
@SuppressWarnings("serial")
public class Network implements Serializable {
  /**
   * In a network with two node types, the maximum number of nodes of either of the two types.
   */
  public static final int MAX_SIZE = 200;
  
  private List<Interaction> interactions = new ArrayList<Interaction>();
  private List<Node> nodes = new ArrayList<Node>();
  private String title;
  
  Network() {}
  
  public Network(String title,
                 List<Node> nodes,
                 List<Interaction> interactions) {
    this.title = title;
    this.nodes = nodes;
    this.interactions = interactions;
  }
  
  public List<Interaction> interactionsFrom(Node from) {
    return interactions.stream().filter(i -> i.from.equals(from)).collect(Collectors.toList());
  }
  
  public List<Interaction> interactionsTo(Node to) {
    return interactions.stream().filter(i -> i.to.equals(to)).collect(Collectors.toList());
  }
  
  public String title() { return title; }
  
  public List<Node> nodes() { return nodes; }
  
  public List<Interaction> interactions() { return interactions; }   
}
