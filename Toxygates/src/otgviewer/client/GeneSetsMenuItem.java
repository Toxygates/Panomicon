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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.SaveActionHandler;
import t.clustering.shared.Algorithm;
import t.common.shared.ClusteringList;
import t.common.shared.ItemList;
import t.common.shared.SharedUtils;
import t.common.shared.StringList;
import t.common.shared.clustering.ProbeClustering;
import t.viewer.client.CodeDownload;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;

public class GeneSetsMenuItem extends DataListenerWidget {

  protected final Logger logger = SharedUtils.getLogger("GeneSetsMenuItem");

  private final DataScreen screen;

  private MenuBar root;
  private MenuItem mi;
  
  private final int ITEMS_PER_MENU = 20;

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

  /**
   * Obtain menu item
   * 
   * @return MenuItem for gene sets
   */
  public MenuItem menuItem() {
    return mi;
  }

  private void createMenuItem() {
    root.addItem(new MenuItem("Show all", false, showAll()));
    createUserSets();
    if (hasUserClustering()) {
      createUserClusterings();
    }
    if (hasPredefinedClustering()) {
      createPredefinedClusterings();
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
    root.addSeparator(new MenuItemSeparator());
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
        mb.addItem(new MenuItem(sl.name(), showClustering(cl, sl)));
      }
      mb.addSeparator(new MenuItemSeparator());
      mb.addItem(new MenuItem("Delete", deleteClustering(cl)));
      root.addItem(cl.name(), mb);
    }

    root.addSeparator(new MenuItemSeparator());
    root.addItem(new MenuItem("Add new", addNewClustering()));
    root.addSeparator(new MenuItemSeparator());
  }

  private String clusteringCaption(@Nullable 
      Algorithm algorithm) {
    if (algorithm == null) {
      return "Unknown algorithm";
    }
    
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

  private void createPredefinedClusterings() {
    root.addSeparator(new MenuItemCaptionSeparator("Clusterings (pre-defined)"));

    Collection<ProbeClustering> clusterings = screen.appInfo().probeClusterings();

    // append items recursively
    for (t.common.shared.clustering.Algorithm algo : t.common.shared.clustering.Algorithm
        .values()) {
      MenuBar mb = new MenuBar(true);

      appendChildren(mb, algo, ProbeClustering.filterByAlgorithm(clusterings, algo));

      root.addItem(algo.getTitle(), mb);
    }
    root.addSeparator(new MenuItemSeparator());
  }

  private void appendChildren(MenuBar parent, t.common.shared.clustering.Algorithm algo,
      Collection<ProbeClustering> clusterings) {
    for (String cl : algo.getClusterings()) {
      MenuBar mb = new MenuBar(true);

      List<String> paramNames = new LinkedList<String>(Arrays.asList(algo.getParams()));
      Collections.sort(paramNames);
      appendChildren(mb, paramNames, ProbeClustering.filterByClustering(clusterings, cl));
      parent.addItem(cl, mb);
    }
  }

  private void appendChildren(MenuBar parent, List<String> paramNames,
      Collection<ProbeClustering> clusterings) {
    // append as leaf
    if (paramNames.size() == 0) {
      List<ProbeClustering> items = new LinkedList<>(clusterings);
      Collections.sort(items, new Comparator<ProbeClustering>() {
        @Override
        public int compare(ProbeClustering o1, ProbeClustering o2) {
          String mine = o1.getClustering().getCluster();
          String theirs = o2.getClustering().getCluster();
          return mine.compareTo(theirs);
        }
      });
      
      if (items.size() > ITEMS_PER_MENU) {
        // split into each ITEMS_PER_MENU items
        List<List<ProbeClustering>> grouped = new LinkedList<>();
        for (int i = 0; i < items.size() / ITEMS_PER_MENU; ++i) {
          int from = ITEMS_PER_MENU * i;
          int to = ITEMS_PER_MENU * (i + 1);
          grouped.add(items.subList(from, to));
        }
        
        for (List<ProbeClustering> g : grouped) {
          if (g.isEmpty()) {
            continue;
          }
          MenuBar mb = new MenuBar(true);
          appendChildren(mb, paramNames, g);
          String title = g.get(0).getClustering().getCluster() + " ~ " + g.get(g.size() - 1).getClustering().getCluster();
          parent.addItem(title, mb);
        }
        return;
      }

      for (ProbeClustering pc : items) {
        parent.addItem(new MenuItem(pc.getClustering().getCluster(), showClustering(pc)));
      }
      return;
    }

    String paramName = paramNames.remove(0);
    List<String> paramValues = ProbeClustering.collectParamValue(clusterings, paramName);
    Collections.sort(paramValues);
    for (String s : paramValues) {
      MenuBar mb = new MenuBar(true);
      appendChildren(mb, paramNames, ProbeClustering.filterByParam(clusterings, paramName, s));
      parent.addItem(s, mb);
    }
  }

  private ScheduledCommand showAll() {
    return new Command() {
      public void execute() {
        screen.geneSetChanged(null);
        screen.probesChanged(new String[0]);
        screen.updateProbes();
      }
    };
  }
  
  /*
   * Commands for User Set
   */
  private ScheduledCommand showUserSet(final StringList sl) {
    return new Command() {
      public void execute() {
        screen.geneSetChanged(sl);
        screen.probesChanged(sl.items());
        screen.updateProbes();
      }
    };
  }

  private ScheduledCommand addNewUserSet() {
    return new Command() {
      public void execute() {
        geneSetEditor(null);
      }
    };
  }

  private ScheduledCommand editUserSet(final StringList sl) {
    return new Command() {
      public void execute() {
        geneSetEditor(sl);
      }
    };
  }

  private ScheduledCommand deleteUserSet(final StringList sl) {
    return new Command() {
      public void execute() {
        if (!Window
            .confirm("About to delete the user set \"" + sl.name() + "\". \nAre you sure?")) {
          return;
        }

        StringListsStoreHelper helper = 
            new StringListsStoreHelper(StringList.PROBES_LIST_TYPE, screen);
        helper.delete(sl.name());
        // If the user deletes chosen gene set, switch to "All probes" automatically.
        if (screen.chosenGeneSet != null && sl.type().equals(screen.chosenGeneSet.type())
            && sl.name().equals(screen.chosenGeneSet.name())) {
          switchToAllProbes();
        }
      }
    };
  }

  /*
   * Commands for User Defined Clustering
   */
  private ScheduledCommand showClustering(final ClusteringList cl, final StringList sl) {
    return new Command() {
      public void execute() {
        screen.geneSetChanged(
            new ClusteringList(ClusteringList.USER_CLUSTERING_TYPE, 
                cl.name(), cl.algorithm(), new StringList[] {sl}));
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
            new ClusteringListsStoreHelper(
                ClusteringList.USER_CLUSTERING_TYPE, screen);
        helper.delete(cl.name());
        // If the user deletes chosen gene set, switch to "All probes" automatically.
        if (screen.chosenGeneSet != null && cl.type().equals(screen.chosenGeneSet.type())
            && cl.name().equals(screen.chosenGeneSet.name())) {
          switchToAllProbes();
        }
      }
    };
  }

  private ScheduledCommand addNewClustering() {
    return new Command() {
      public void execute() {
        HeatmapViewer.show(screen, screen.et.getValueType());
      }
    };
  }

  /*
   * Commands for Pre-Defined Clustering
   */
  private ScheduledCommand showClustering(final ProbeClustering pc) {
    return new Command() {
      public void execute() {
        screen.geneSetChanged(pc.getList());
        screen.probesChanged(pc.getList().items());
        screen.updateProbes();
      }
    };
  }

  private void switchToAllProbes() {
    screen.geneSetChanged(null);
    screen.probesChanged(new String[0]);
    screen.updateProbes();
  }

  private void ensureSorted(List<? extends ItemList> list) {
    // TODO consider ordering
    Collections.sort(list, new Comparator<ItemList>() {
      @Override
      public int compare(ItemList o1, ItemList o2) {
        return o1.name().compareTo(o2.name());
      }
    });

    // new Comparator<ItemList>() {
    // @Override
    // public int compare(ItemList o1, ItemList o2) {
    // String name1 = o1.name();
    // String name2 = o2.name();
    // if (name1.length() == name2.length()) {
    // return name1.compareTo(name2);
    // }
    // return (name1.length() < name2.length() ? -1 : 1);
    // }
    // });
  }

  private void geneSetEditor(@Nullable final StringList list) {
    
    GWT.runAsync(new CodeDownload(logger) {
      public void onSuccess() {
        if (list != null) {
          geneSetEditorAsync().edit(list.name());
        } else {
          geneSetEditorAsync().createNew(screen.displayedAtomicProbes());
        }
      }
    });
  }
  
  private GeneSetEditor geneSetEditorAsync() {
    // TODO same code as GeneSetToolbar
    GeneSetEditor gse = screen.factory().geneSetEditor(screen);
    gse.addSaveActionHandler(new SaveActionHandler() {
      @Override
      public void onSaved(String title, List<String> items) {
        String[] itemsArray = items.toArray(new String[0]);
        screen.geneSetChanged(new StringList(StringList.PROBES_LIST_TYPE, 
            title, itemsArray));
        screen.probesChanged(itemsArray);
        screen.updateProbes();
      }

      @Override
      public void onCanceled() {}
    });
    addListener(gse);
    return gse;
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
   * the DataScreen is activated. [DataScreen#show -> Screen#show -> Screen#loadState ->
   * DataListenerWidget#loadState]
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
