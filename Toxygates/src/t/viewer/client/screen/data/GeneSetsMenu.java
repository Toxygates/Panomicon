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

package t.viewer.client.screen.data;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import t.viewer.shared.clustering.AlgorithmEnum;
import t.viewer.shared.clustering.Algorithm;
import t.viewer.shared.clustering.ClusteringList;
import t.common.shared.SharedUtils;
import t.viewer.client.Analytics;
import t.viewer.client.components.MenuItemCaptionSeparator;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
import t.viewer.shared.clustering.ProbeClustering;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;

public class GeneSetsMenu {

  protected final Logger logger = SharedUtils.getLogger("GeneSetsMenuItem");

  private final DataScreen screen;

  private MenuBar root;
  private MenuItem mi;
  
  private final int ITEMS_PER_MENU = 20;

  public GeneSetsMenu(DataScreen screen) {
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

    List<StringList> geneSets = screen.geneSets().allObjects(); 
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
        ClusteringList.pickUserClusteringLists(screen.clusteringLists().allObjects(), null);
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
    for (AlgorithmEnum algo : AlgorithmEnum
        .values()) {
      MenuBar mb = new MenuBar(true);

      appendChildren(mb, algo, ProbeClustering.filterByAlgorithm(clusterings, algo));

      root.addItem(algo.getTitle(), mb);
    }
    root.addSeparator(new MenuItemSeparator());
  }

  private void appendChildren(MenuBar parent, AlgorithmEnum algo,
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
    return () -> {
      screen.geneSetChanged(null);
      screen.probesChanged(new String[0]);
      screen.reloadDataIfNeeded();
    };
  }
  
  /*
   * Commands for User Set
   */
  private ScheduledCommand showUserSet(final StringList sl) {
    return () -> {
      screen.geneSetChanged(sl);
      screen.probesChanged(sl.items());
      screen.reloadDataIfNeeded();
    };  
  }

  private ScheduledCommand addNewUserSet() {
    return () -> geneSetEditor(null);      
  }

  private ScheduledCommand editUserSet(final StringList sl) {
    return () -> geneSetEditor(sl);
  }

  private ScheduledCommand deleteUserSet(final StringList sl) {
    return () -> {
      if (!Window.confirm("About to delete the user set \"" + sl.name() + "\". \nAre you sure?")) {
        return;
      }

      ItemList geneSet = screen.geneSet();
      screen.geneSets().remove(sl.name());
      screen.geneSetsChanged();
      Analytics.trackEvent(Analytics.CATEGORY_GENE_SET, Analytics.ACTION_DELETE_GENE_SET);
      // If the user deletes chosen gene set, switch to "All probes" automatically.
      if (geneSet != null && sl.type().equals(geneSet.type())
          && sl.name().equals(geneSet.name())) {
        switchToAllProbes();
      }
    };
  }

  /*
   * Commands for User Defined Clustering
   */
  private ScheduledCommand showClustering(final ClusteringList cl, final StringList sl) {
    return () -> {
      screen.geneSetChanged(new ClusteringList(ClusteringList.USER_CLUSTERING_TYPE, cl.name(), cl
          .algorithm(), new StringList[] {sl}));
      screen.probesChanged(sl.items());
      screen.reloadDataIfNeeded();
    };
  }

  private ScheduledCommand deleteClustering(final ClusteringList cl) {
    return () -> {
      if (!Window.confirm("About to delete the clustering \"" + cl.name() + "\". \nAre you sure?")) {
        return;
      }

      screen.clusteringLists().remove(cl.name());
      screen.clusteringListsChanged();
      
      ItemList geneSet = screen.geneSet();

      // If the user deletes chosen gene set, switch to "All probes" automatically.
      if (geneSet != null && cl.type().equals(geneSet.type()) && cl.name().equals(geneSet.name())) {
        switchToAllProbes();
      }
    };
  }

  private ScheduledCommand addNewClustering() {
    return () ->
    HeatmapViewer.show(screen, screen.dataView(), screen.dataView().chosenValueType());
  }

  /*
   * Commands for Pre-Defined Clustering
   */
  private ScheduledCommand showClustering(final ProbeClustering pc) {
    return () -> {
        screen.geneSetChanged(pc.getList());
        screen.probesChanged(pc.getList().items());
        screen.reloadDataIfNeeded();
    };
  }

  private void switchToAllProbes() {
    screen.geneSetChanged(null);
    screen.probesChanged(new String[0]);
    screen.reloadDataIfNeeded();
  }

  private void ensureSorted(List<? extends ItemList> list) {
    Collections.sort(list, new Comparator<ItemList>() {
      @Override
      public int compare(ItemList o1, ItemList o2) {
        return o1.name().compareTo(o2.name());
      }
    });
  }

  private void geneSetEditor(@Nullable final StringList list) {
    if (list != null) {
      if (! GeneSetEditor.withinEditableSize(list)) {
        Window.alert("That gene set is too large to be edited.");
        return;
      }
    }
    GeneSetEditor.editOrCreateNewGeneSet(screen, list, false);
  }

  /**
   * Refresh menu items on stringListsChanged fired. Note the events would be also
   * fired when the DataScreen is activated. [DataScreen#show -> Screen#show ->
   * Screen#lodaState -> ? ]
   * 
   * @see DataScreen#show()
   */
  public void geneSetsChanged(List<StringList> lists) {
    root.clearItems();
    createMenuItem();
  }

  /**
   * Refresh menu items on clusteringListsChanged fired. Note the events would be
   * also fired when the DataScreen is activated. [DataScreen#show -> Screen#show
   * -> Screen#loadState -> ? ]
   * 
   * @see DataScreen#show()
   */
  public void clusteringListsChanged(List<ItemList> lists) {
    root.clearItems();
    createMenuItem();
  }

}
