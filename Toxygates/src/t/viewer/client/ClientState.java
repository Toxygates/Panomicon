package t.viewer.client;

import java.util.List;

import t.common.shared.Dataset;
import t.common.shared.ItemList;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleColumn;
import t.model.SampleClass;

/**
 * Captures the current client-side state.
 */
public class ClientState {
  
  public ClientState(Dataset[] datasets, SampleClass sampleClass,
      String[] probes, List<String> compounds,
      List<Group> columns,
      SampleColumn customColumn, List<ItemList> itemLists,
      ItemList geneSet,
      List<ItemList> chosenClusteringList) {
    
    this.sampleClass = sampleClass;
    this.datasets = datasets;
    this.probes = probes;
    this.compounds = compounds;
    this.columns = columns;
    this.customColumn = customColumn;
    this.itemLists = itemLists;
    this.chosenClusteringList = chosenClusteringList;
    this.geneSet = geneSet;
  }
  
  public final Dataset[] datasets;
  public final SampleClass sampleClass; 
  public final String[] probes;
  public final List<String> compounds;
  public final List<Group> columns;
  public final SampleColumn customColumn;
  public final List<ItemList> itemLists, chosenClusteringList;
  public final ItemList geneSet;  
}
