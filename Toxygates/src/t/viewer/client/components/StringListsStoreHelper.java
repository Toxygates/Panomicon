/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */
package t.viewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import t.clustering.shared.ClusteringList;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

public class StringListsStoreHelper {
  private final static String SET_PREFIX = "Set:";
  private final static String CLUSTER_PREFIX = "Clust:";
  
  /**
   * Given a mixed collection, extract both clusters and normal lists
   * into a unified StringList format.
   */
  public static List<StringList> compileLists(Collection<ItemList> ils) {    
    List<StringList> r = new ArrayList<StringList>();
    
    for (StringList l : StringList.pickProbeLists(ils, null)) {
      r.add(l.copyWithName(SET_PREFIX + l.name()));
    }
    
    for (ItemList cl: ClusteringList.pickUserClusteringLists(ils, null)) {
      for (StringList l: ((ClusteringList) cl).asStringLists()) {
        r.add(l.copyWithName(CLUSTER_PREFIX + l.name()));
      }
    }
    
    return r;
  }
  
  /**
   * Compile item lists and clustering list into a StringList format.
   */
  public static List<StringList> compileLists(List<ItemList> itemLists, List<ItemList> clusteringList) {
    List<ItemList> r = new ArrayList<ItemList>();
    r.addAll(itemLists);
    r.addAll(clusteringList);
    return compileLists(r);    
  }
  
  private static class ClusterBuilder {
    String baseName;
    ClusterBuilder(String baseName) {
      this.baseName = baseName;
    }
    
    List<StringList> lists = new ArrayList<StringList>();
   
    void addCluster(StringList cluster) {
      lists.add(cluster);
    }
    
    ClusteringList build() {
      Collections.sort(lists);
      return new ClusteringList(ClusteringList.USER_CLUSTERING_TYPE, 
          baseName, null, lists.toArray(new StringList[0]));
    }
  }
  
  /**
   * Given a collection of StringLists, group them into ClusteringList
   * and StringList. This is approximately the inverse operation of 
   * compileLists (algorithm details etc for ClusteringList are not preserved)
   */
  public static List<ItemList> rebuildLists(Logger log,
      Collection<StringList> items) {
    List<ItemList> r = new ArrayList<ItemList>();
    Map<String, ClusterBuilder> clusterBuilders = new HashMap<String, ClusterBuilder>();
    
    for (StringList sl: items) {
      if (sl.name().startsWith(CLUSTER_PREFIX)) {
        String[] spl = sl.name().split(":");
        if (spl.length != 2) {                     
          log.warning("Unable to reconstruct cluster with name: " + sl.name());          
          continue;
        }         
        String nameWithIdx = spl[1]; //e.g. "MyCluster 2"
        
        String[] spl2 = spl[1].split("\\s+");
        if (spl2.length < 2) {
          log.warning("Unable to reconstruct cluster with name: " + sl.name());
          continue;
        }
        String baseName = spl2[0]; //e.g. "MyCluster"
        
        if (!clusterBuilders.containsKey(baseName)) {
          clusterBuilders.put(baseName, new ClusterBuilder(baseName));
        }
        ClusterBuilder cb = clusterBuilders.get(baseName);
        cb.addCluster(sl.copyWithName(nameWithIdx));        
      } else {
        String useName = "";
        if (sl.name().startsWith(SET_PREFIX)) {
          useName = sl.name().substring(SET_PREFIX.length());
        } else {
          //Assue it is a normal set
          useName = sl.name();
        }
        r.add(sl.copyWithName(useName));      
      }
    }
    
    for (ClusterBuilder cb: clusterBuilders.values()) {
      r.add(cb.build());
    }
    
    return r;
  }
}
