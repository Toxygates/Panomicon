package t.common.shared.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
class HierarchicalClustering extends Clusterings {
  
  public HierarchicalClustering() {
    super();
  }

  public HierarchicalClustering(Algorithm algorithm, String clustering, Map<String, String> params,
      String cluster, String title) {
    super(algorithm, clustering, params, cluster, title);
  }

  /**
   * Obtain clustering information from the clustering title.
   * 
   * @param title the title of clustering (e.g. LV_K120_C1)
   * @return
   */
  public static Clusterings parse(String title) {
    String[] items = title.split("_");
    if (items.length != 3) {
      return null;
    }
    
    // check clusterings
    List<String> clusterings = new ArrayList<String>(Arrays.asList(Algorithm.HIERARCHICAL.getClusterings()));
    if (!clusterings.contains(items[0])) {
      return null;
    }

    // check params
    if (items[1].indexOf("K") == -1) {
      // if K is not found
      return null;
    }
    String k = items[1].substring(items[1].indexOf("K"), items[1].length());
    if (k.length() == 0) {
      return null;
    }

    Map<String, String> params = new HashMap<String, String>();
    params.put("K", k);
    
    // e.g.
    // algorithm = HIERARCHICAL
    // clustering = "LV"
    // params = { K -> 120 }
    // cluster = "C1"
    // title = "LV_K120_C1"
    return new HierarchicalClustering(Algorithm.HIERARCHICAL, items[0], params, items[2], title);
  }

}
