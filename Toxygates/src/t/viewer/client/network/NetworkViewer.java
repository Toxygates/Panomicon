package t.viewer.client.network;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import t.viewer.shared.network.Node;

/**
 * A viewer that displays a subset of a network.
 *
 */
public interface NetworkViewer {

  List<Node> getSourceNodes();
  
  List<Node> getDestNodes();
  
  void setHighlightedSourceNodes(Set<String> nodes);
  
  void setHighlightedDestNodes(Set<String> nodes);
  
  @Nullable String getSelectedSourceNode();
  
  @Nullable String getSelectedDestNode();
  
  String getSourceType();
  
  String getDestType();
  
  void onSourceSelectionChanged();
  
  void onDestSelectionChanged();
}
