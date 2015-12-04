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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.SaveActionHandler;
import t.common.shared.ClusteringList;
import t.common.shared.ItemList;
import t.common.shared.SharedUtils;
import t.common.shared.StringList;
import t.common.shared.userclustering.Algorithm;

public class GeneSetsMenuItem extends DataListenerWidget {

  protected final Logger logger = SharedUtils.getLogger("GeneSetsMenuItem");

  private final DataScreen screen;

  private MenuBar root;
  private MenuItem mi;

  public GeneSetsMenuItem(DataScreen screen) {
    this.screen = screen;
    root = new MenuBar(true);
    mi = new MenuItem("Gene Sets", false, root);

    createMenuItem();
  }

  protected boolean hasUserClustering() {
    return true;
  }

  protected boolean hasPredefinedClustering() {
    return true;
  }

  /*
   * Hack GWT-MenuItemSeparator to indicate caption upon the separator
   */
  class MenuItemCaptionSeparator extends MenuItemSeparator {
    public MenuItemCaptionSeparator(String caption) {
      super();
      getElement().removeAllChildren();

      Element div = DOM.createDiv();
      div.setInnerHTML(caption.replaceAll("\n", "<br>"));
      DOM.appendChild(getElement(), div);
      setStyleName(div, "menuSeparatorCaption");

      div = DOM.createDiv();
      DOM.appendChild(getElement(), div);
      setStyleName(div, "menuSeparatorInner");
    }
  }

  private void createMenuItem() {
    createUserSets();

    if (hasUserClustering()) {
      createUserClusterings();
    }

    if (hasPredefinedClustering()) {
    }

  }

  private void createUserSets() {
    root.addSeparator(new MenuItemCaptionSeparator("User sets"));

    List<StringList> geneSets = StringList.pickProbeLists(screen.chosenItemLists, null);
    ensureSorted(geneSets);

    for (final StringList sl : geneSets) {
      MenuBar item = new MenuBar(true);

      item.addItem(new MenuItem("Show", false, showUserSet(sl)));
      item.addItem(new MenuItem("Edit", false, editUserSet(sl)));
      item.addItem(new MenuItem("Delete", false, deleteUserSet(sl)));
      root.addItem(sl.name(), item);
    }

    root.addSeparator(new MenuItemSeparator());
    root.addItem(new MenuItem("Add new", false, addNewUserSet()));
  }

  private ScheduledCommand showCluster(final ClusteringList cl, final StringList sl) {
    return new Command() {
      public void execute() {
        screen.geneSetChanged(
            new ClusteringList("userclustering", cl.name(), cl.algorithm(), new StringList[] {sl}));
        screen.probesChanged(sl.items());
        screen.updateProbes();
      }
    };
  }

  private ScheduledCommand deleteClustering(final ClusteringList cl) {
    return new Command() {
      @Override
      public void execute() {
        if (!Window
            .confirm("About to delete the clustering \"" + cl.name() + "\". \nAre you sure?")) {
          return;
        }

        ClusteringListsStoreHelper helper =
            new ClusteringListsStoreHelper("userclustering", screen);
        helper.delete(cl.name());
        // If the user deletes chosen gene set, switch to "All probes" automatically.
        if (screen.chosenGeneSet != null && cl.type().equals(screen.chosenGeneSet.type())
            && cl.name().equals(screen.chosenGeneSet.name())) {
          switchToAllProbes();
        }
      }
    };
  }

  private void switchToAllProbes() {
    screen.geneSetChanged(null);
    screen.probesChanged(new String[0]);
    screen.updateProbes();
  }

  private void ensureSorted(List<? extends ItemList> list) {
    Collections.sort(list, new Comparator<ItemList>() {
      @Override
      public int compare(ItemList o1, ItemList o2) {
        String name1 = o1.name();
        String name2 = o2.name();
        if (name1.length() == name2.length()) {
          return name1.compareTo(name2);
        }
        return (name1.length() < name2.length() ? -1 : 1);
      }
    });
  }

  private GeneSetEditor geneSetEditor() {
    // TODO same code as GeneSetSelector
    GeneSetEditor gse = screen.factory().geneSetEditor(screen);
    gse.addSaveActionHandler(new SaveActionHandler() {
      @Override
      public void onSaved(String title, List<String> items) {
        String[] itemsArray = items.toArray(new String[0]);
        screen.geneSetChanged(new StringList("probes", title, itemsArray));
        screen.probesChanged(itemsArray);
        screen.updateProbes();
      }

      @Override
      public void onCanceled() {}
    });
    addListener(gse);
    return gse;
  }

  private Command showUserSet(final StringList sl) {
    return new Command() {
      public void execute() {
        screen.geneSetChanged(sl);
        screen.probesChanged(sl.items());
        screen.updateProbes();
      }
    };
  }

  private Command addNewUserSet() {
    return new Command() {
      public void execute() {
        geneSetEditor().createNew(screen.displayedAtomicProbes());
      }
    };
  }

  private Command editUserSet(final StringList sl) {
    return new Command() {
      public void execute() {
        geneSetEditor().edit(sl.name());
      }
    };
  }

  private Command deleteUserSet(final StringList sl) {
    return new Command() {
      public void execute() {
        if (!Window
            .confirm("About to delete the user set \"" + sl.name() + "\". \nAre you sure?")) {
          return;
        }

        StringListsStoreHelper helper = new StringListsStoreHelper("probes", screen);
        helper.delete(sl.name());
        // If the user deletes chosen gene set, switch to "All probes" automatically.
        if (screen.chosenGeneSet != null && sl.type().equals(screen.chosenGeneSet.type())
            && sl.name().equals(screen.chosenGeneSet.name())) {
          switchToAllProbes();
        }
      }
    };
  }

  private void createUserClusterings() {
    root.addSeparator(new MenuItemCaptionSeparator("Clusterings (user)"));

    List<ClusteringList> clusterings =
        ClusteringList.pickUserClusteringLists(screen.chosenClusteringList, null);
    ensureSorted(clusterings);

    for (final ClusteringList cl : clusterings) {
      MenuBar mb = new MenuBar(true);

      // put clustering description
      String caption = clusteringCaption(cl.algorithm());
      mb.addSeparator(new MenuItemCaptionSeparator(caption));

      for (final StringList sl : cl.items()) {
        mb.addItem(new MenuItem(sl.name(), showCluster(cl, sl)));
      }
      mb.addSeparator(new MenuItemSeparator());
      mb.addItem(new MenuItem("Delete", deleteClustering(cl)));
      root.addItem(cl.name(), mb);
    }

    root.addSeparator(new MenuItemSeparator());
  }

  private String clusteringCaption(Algorithm algorithm) {
    StringBuffer sb = new StringBuffer();
    sb.append("Row : ");
    sb.append(algorithm.getRowMethod().asParam());
    sb.append(", ");
    sb.append(algorithm.getRowDistance().asParam());
    sb.append("\n Col : ");
    sb.append(algorithm.getColMethod().asParam());
    sb.append(", ");
    sb.append(algorithm.getColDistance().asParam());
    return sb.toString();
  }

  public MenuItem menuItem() {
    return mi;
  }

  /**
   * Refresh menu items on itemListsChanged fired. Note the events would be also fired when the
   * DataScreen is activated. [DataScreen#show -> Screen#show -> Screen#lodaState ->
   * DataListenerWidget#lodaState]
   * 
   * @see otgviewer.client.DataScreen#show()
   */
  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    root.clearItems();
    createMenuItem();
  }

  /**
   * Refresh menu items on clusteringListsChanged fired. Note the events would be also fired when
   * the DataScreen is activated. [DataScreen#show -> Screen#show -> Screen#lodaState ->
   * DataListenerWidget#lodaState]
   * 
   * @see otgviewer.client.DataScreen#show()
   */
  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    super.clusteringListsChanged(lists);
    root.clearItems();
    createMenuItem();
  }

}
