/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.shared.clustering;

import java.util.*;

@SuppressWarnings("serial")
class HierarchicalClustering extends Clusterings {
  
  public HierarchicalClustering() {
    super();
  }

  public HierarchicalClustering(AlgorithmEnum algorithm, String clustering, Map<String, String> params,
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
    List<String> clusterings = new ArrayList<String>(Arrays.asList(AlgorithmEnum.HIERARCHICAL.getClusterings()));
    if (!clusterings.contains(items[0])) {
      return null;
    }

    // check params
    if (items[1].indexOf("K") == -1) {
      // if K is not found
      return null;
    }
    String k = items[1].substring(items[1].indexOf("K"));
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
    return new HierarchicalClustering(AlgorithmEnum.HIERARCHICAL, items[0], params, items[2], title);
  }

}
