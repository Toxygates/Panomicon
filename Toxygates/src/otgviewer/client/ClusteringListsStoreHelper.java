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
package otgviewer.client;

import java.util.*;

import otgviewer.client.components.ImportingScreen;
import t.clustering.shared.Algorithm;
import t.common.shared.*;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.InputDialog;

public class ClusteringListsStoreHelper extends ItemListsStoreHelper {
  // private final Logger logger = SharedUtils.getLogger("ClusteringListsStoreHelper");

  public ClusteringListsStoreHelper(String type, ImportingScreen screen) {
    super(type, screen);
  }

  @Override
  protected void init() {
    for (ItemList il : screen.clusteringList()) {
      if (il instanceof ClusteringList) {
        ClusteringList sl = (ClusteringList) il;
        putIfAbsent(sl.type()).put(sl.name(), sl);
      }
    }
  }

  /*
   * Override this function to handle what genes were saved
   */
  protected void onSaveSuccess(String name, ClusteringList items) {}

  public void save(List<Collection<String>> lists, Algorithm algorithm) {
    saveAction(lists, algorithm, "Name entry", "Please enter a name for the list.");
  }

  private void saveAction(final List<Collection<String>> lists, final Algorithm algorithm,
      String caption, String message) {
    InputDialog entry = new InputDialog(message) {
      @Override
      protected void onChange(String value) {
        if (value == null) { // on Cancel clicked
          inputDialog.setVisible(false);
          return;
        }

        if (!validate(value)) {
          return;
        }

        List<String> names = generateNameList(value, lists.size());
        List<StringList> clusters = new ArrayList<StringList>();
        for (int i = 0; i < lists.size(); ++i) {
          clusters.add(new StringList(StringList.PROBES_LIST_TYPE, 
              names.get(i), lists.get(i).toArray(new String[0])));
        }

        ClusteringList cl =
            new ClusteringList(type, value, algorithm, clusters.toArray(new StringList[0]));
        putIfAbsent(type).put(value, cl);

        storeItemLists();
        inputDialog.setVisible(false);

        onSaveSuccess(value, cl);
      }
    };
    inputDialog = Utils.displayInPopup(caption, entry, DialogPosition.Center);
  }

  private List<String> generateNameList(String base, int size) {
    if (size > 1) {
      return getSerialNumberedNames(base, size);
    } else {
      return new ArrayList<String>(Arrays.asList(new String[] {base}));
    }
  }

  private List<String> getSerialNumberedNames(String base, int count) {
    List<String> names = new ArrayList<String>(count);
    for (int i = 0; i < count; ++i) {
      names.add(base + " " + (i + 1));
    }
    return names;
  }

  private void storeItemLists() {
    screen.clusteringListsChanged(buildItemLists());
  }

  /**
   * Delete specified list from storage
   * 
   * @param name the name to be deleted
   */
  @Override
  public void delete(String name) {
    if (!itemLists.containsKey(type)) {
      throw new RuntimeException("Type \"" + type + "\" not found.");
    }
    
    if (itemLists.get(type).remove(name) != null) {
      screen.clusteringListsChanged(buildItemLists());
    }
  }

}
