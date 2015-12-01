/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import otgviewer.client.components.Screen;
import otgviewer.client.dialog.InputDialog;
import t.common.shared.ClusteringList;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.userclustering.Algorithm;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;

public class ClusteringListsStoreHelper extends ItemListsStoreHelper {

  // private final Logger logger = SharedUtils.getLogger("ClusteringListsStoreHelper");

  private Algorithm algorithm;

  public ClusteringListsStoreHelper(String type, Screen screen, Algorithm algorithm) {
    super(type, screen);
    this.algorithm = algorithm;
  }

  @Override
  protected void init() {
    for (ItemList il : screen.chosenClusteringList) {
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

  public void save(List<Collection<String>> lists) {
    saveAction(lists, "Name entry", "Please enter a name for the list.");
  }

  private void saveAction(final List<Collection<String>> lists, String caption, String message) {
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
          clusters.add(new StringList("probes", names.get(i), lists.get(i).toArray(new String[0])));
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
    screen.storeClusteringLists(screen.getParser());
  }

}
