/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package otg.viewer.client.components;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.sample.Group;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.shared.AppInfo;
import t.viewer.shared.ItemList;
import t.viewer.shared.intermine.IntermineInstance;

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

import otg.viewer.client.intermine.InterMineData;

public interface ImportingScreen extends OTGScreen {
  void setUrlProbes(String[] probes);

  void setUrlColumns(List<String[]> groups, List<String> names);

  void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists);

  void runEnrichment(@Nullable IntermineInstance preferredInstance);

  List<ItemList> clusteringList();

  List<ItemList> itemLists();

  void clusteringListsChanged(List<ItemList> lists);

  void itemListsChanged(List<ItemList> lists);

  List<Group> chosenColumns();

  String[] chosenProbes();
  
  default List<MenuItem> intermineMenuItems(AppInfo appInfo) {
    List<MenuItem> intermineItems = new ArrayList<MenuItem>();
    for (IntermineInstance ii: appInfo.intermineInstances()) {
      intermineItems.add(intermineMenu(ii));
    }
    return intermineItems;    
  }
  
  default MenuItem intermineMenu(final IntermineInstance inst) {
    MenuBar mb = new MenuBar(true);
    final String title = inst.title();
    MenuItem mi = new MenuItem(title + " data", mb);

    mb.addItem(new MenuItem("Import gene sets from " + title + "...", () -> {      
      new InterMineData(this, inst).importLists(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT, Analytics.ACTION_IMPORT_GENE_SETS,
            title);      
    }));

    mb.addItem(new MenuItem("Export gene sets to " + title + "...", () -> {      
      new InterMineData(this, inst).exportLists();
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT, Analytics.ACTION_EXPORT_GENE_SETS,
            title);      
    }));

    mb.addItem(new MenuItem("Enrichment...", () -> {      
        //TODO this should be disabled if we are not on the data screen.
        //The menu item is only here in order to be logically grouped with other 
        //TargetMine items, but it is a duplicate and may be removed.
      this.runEnrichment(inst);
    }));

    mb.addItem(new MenuItem("Go to " + title, () -> 
        Utils.displayURL("Go to " + title + " in a new window?", "Go", inst.webURL())
        ));
      
    return mi;
  }
}
