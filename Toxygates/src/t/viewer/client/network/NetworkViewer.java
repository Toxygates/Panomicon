package t.viewer.client.network;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * A viewer that displays a subset of a network.
 *
 */
public interface NetworkViewer {
  void setHighlightedSourceNodes(Set<String> nodes);
  
  void setHighlightedDestNodes(Set<String> nodes);
  
  @Nullable String getSelectedSourceNode();
  
  @Nullable String getSelectedDestNode();
  
  void onSourceSelectionChanged();
  
  void onDestSelectionChanged();
}
