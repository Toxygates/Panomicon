package t.common.shared;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This class is intended mainly for grouping shared datasets, which will have
 * the same user title but different IDs.
 * @author johan
 */

@SuppressWarnings("serial")
public class GroupedDataset extends Dataset {

  private List<Dataset> subDatasets;
  
  public GroupedDataset(String title, String description, List<Dataset> subDatasets) {
    super(title, description, "", new Date(), "");
    this.subDatasets = subDatasets;
  }
  
  @Override
  public Collection<Dataset> getSubDatasets() {
    return subDatasets;    
  }
}
