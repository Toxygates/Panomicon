package t.viewer.client.network;

import java.util.*;

import t.viewer.shared.network.*;

abstract public class NetworkController {
  
  private NetworkViewer viewer;
  public NetworkController(NetworkViewer viewer) {
    this.viewer = viewer;
  }
  
  /**
   * Build the interaction network represented by the current view.
   * @return
   */
  public Network buildNetwork(String title, boolean reversed) {
    List<Node> nodes = new ArrayList<Node>();
    nodes.addAll(viewer.getSourceNodes());    
    nodes.addAll(viewer.getDestNodes());
    Map<String, Node> lookup = new HashMap<String, Node>();    
    for (Node n: nodes) {
      lookup.put(n.id(), n);
    }

    List<Interaction> interactions = new ArrayList<Interaction>();
    Map<String, Collection<String>> fullMap = linkingMap();
    for (String mainProbe: fullMap.keySet()) {
      for (String target: fullMap.get(mainProbe)) {        
        Node main = lookup.get(mainProbe);
        Node side = lookup.get(target);

        //Directed interaction normally from miRNA to mRNA
        Node from = (!reversed) ? main : side;
        Node to = (!reversed) ? side : main;
        if (from != null && to != null) {
          Interaction i = new Interaction(from, to, null, null);
          interactions.add(i);
        }
      }
    }
    return new Network(title, nodes, interactions);
  }
  
  abstract public Map<String, Collection<String>> linkingMap();
   
}
