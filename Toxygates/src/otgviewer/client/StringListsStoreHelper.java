/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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
package otgviewer.client;

import java.util.*;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import t.common.client.HasLogger;
import t.common.shared.*;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.InputDialog;

public class StringListsStoreHelper extends ItemListsStoreHelper {
  
  // private final Logger logger = SharedUtils.getLogger("ItemListsStoreHelper");

  public StringListsStoreHelper(String type, Screen screen) {
    super(type, screen);
  }

  /*
   * Override this function to handle what genes were saved
   */
  protected void onSaveSuccess(String name, StringList items) {}

  /**
   * Save simple string list to local storage. An input box which asks the list's title will be
   * shown.
   * 
   * @param list
   */
  public void save(Collection<String> list) {
    saveAction(list, "Name entry", "Please enter a name for the list.");
  }

  /**
   * Save gene set with specified title. No input box will be shown.
   * @return true iff the save action was successful
   */
  public boolean saveAs(Collection<String> list, String name) {
    return saveAction(list, name, false);
  }

  public boolean saveAs(Collection<String> list, String name, boolean overwrite) {
    return saveAction(list, name, overwrite);
  }

  private void saveAction(final Collection<String> list, String caption, String message) {
    InputDialog entry = new InputDialog(message) {
      @Override
      protected void onChange(String value) {
        if (value == null) { // on Cancel clicked
          inputDialog.setVisible(false);
          return;
        }

        saveAction(list, value, false);
        inputDialog.setVisible(false);
      }
    };
    inputDialog = Utils.displayInPopup(caption, entry, DialogPosition.Center);
  }

  private boolean saveAction(Collection<String> list, String name, boolean overwrite) {
    if (!validate(name, overwrite)) {
      return false;
    }

    StringList sl = new StringList(type, name, list.toArray(new String[0]));
    putIfAbsent(type).put(name, sl);

    storeItemLists();

    onSaveSuccess(name, sl);
    return true;
  }

  private void storeItemLists() {
    screen.itemListsChanged(buildItemLists());
    screen.storeItemLists(screen.getParser());
  }
  
  private final static String SET_PREFIX = "Set:";
  private final static String CLUSTER_PREFIX = "Clust:";
  
  /**
   * Given a mixed collection, extract both clusters and normal lists
   * into a unified StringList format.
   * @param parent
   * @return
   */
  public static List<StringList> compileLists(Collection<ItemList> ils) {    
    List<StringList> r = new ArrayList<StringList>();
    
    for (StringList l : StringList.pickProbeLists(ils, null)) {
      r.add((StringList) l.copyWithName(SET_PREFIX + l.name()));
    }
    
    for (ItemList cl: ClusteringList.pickUserClusteringLists(ils, null)) {
      for (StringList l: ((ClusteringList) cl).asStringLists()) {
        r.add((StringList) l.copyWithName(CLUSTER_PREFIX + l.name()));
      }
    }
    
    return r;
  }
  
  /**
   * Given a DataListenerWidget, extract both clusters and normal lists
   * into a unified StringList format.
   * @param parent
   * @return
   */
  public static List<StringList> compileLists(DataListenerWidget parent) {    
    List<ItemList> r = new ArrayList<ItemList>();
    r.addAll(parent.chosenItemLists);
    r.addAll(parent.chosenClusteringList);
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
   * @param items
   * @return
   */
  public static List<ItemList> rebuildLists(HasLogger log,
      Collection<StringList> items) {
    List<ItemList> r = new ArrayList<ItemList>();
    Map<String, ClusterBuilder> clusterBuilders = new HashMap<String, ClusterBuilder>();
    
    for (StringList sl: items) {
      if (sl.name().startsWith(CLUSTER_PREFIX)) {
        String[] spl = sl.name().split(":");
        if (spl.length != 2) {                     
          log.getLogger().warning("Unable to reconstruct cluster with name: " + sl.name());          
          continue;
        }         
        String nameWithIdx = spl[1]; //e.g. "MyCluster 2"
        
        String[] spl2 = spl[1].split("\\s+");
        if (spl2.length < 2) {
          log.getLogger().warning("Unable to reconstruct cluster with name: " + sl.name());
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
