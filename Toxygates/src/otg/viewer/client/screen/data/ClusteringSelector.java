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

package otg.viewer.client.screen.data;

import java.util.*;
import java.util.Map.Entry;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.ListChooser;
import t.viewer.client.Utils;
import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;
import t.viewer.shared.clustering.Algorithm;
import t.viewer.shared.clustering.ProbeClustering;

public abstract class ClusteringSelector extends Composite implements RequiresResize {

  private DockLayoutPanel dp;

  private Collection<ProbeClustering> probeClusterings;

  private ClListBox algorithm;
  private ClListBox clustering;
  private VerticalPanel paramTitlesContainer;
  private VerticalPanel paramListsContainer;
  private ListChooser cluster;

  private Button addButton;

  private Set<String> loadedProbes = new HashSet<String>();
  
  private Algorithm lastAlgorithm;
  private Map<String, ClListBox> params = new HashMap<String, ClListBox>();
  
  class ClListBox extends ListBox {
    ClListBox() {}

    void setItems(List<String> items) {
      String oldSel = getSelected();
      clear();

      Collections.sort(items, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          if (o1.length() == o2.length()) {
            return o1.compareTo(o2);
          }
          return (o1.length() < o2.length() ? -1 : 1);
        }
      });
      for (String i : items) {
        addItem(i);
      }

      if (oldSel != null && items.indexOf(oldSel) != -1) {
        trySelect(oldSel);
      } else if (items.size() > 0) {
        setSelectedIndex(0);
      }
    }

    void trySelect(String item) {
      for (int i = 0; i < getItemCount(); i++) {
        if (getItemText(i).equals(item)) {
          setSelectedIndex(i);
          return;
        }
      }
    }

    String getSelected() {
      int i = getSelectedIndex();
      if (i != -1) {
        return getItemText(i);
      } else {
        return null;
      }
    }
  }

  public ClusteringSelector() {
    this(Collections.<ProbeClustering>emptyList());
  }

  public ClusteringSelector(Collection<ProbeClustering> clusterings) {
    this.dp = new DockLayoutPanel(Unit.PX);
    this.probeClusterings = clusterings;
    
    initWidget(dp);
  }
  
  private void algorithmChanged(String algorithm) {
    if (algorithm == null) {
      return;
    }
    
    Algorithm selected = Algorithm.valueOf(algorithm.toUpperCase());
    lastAlgorithm = selected;
    
    List<String> items = new ArrayList<String>();
    for (String cl : selected.getClusterings()) {
      items.add(cl);
    }
    
    clustering.setItems(items);
    clusteringChanged(clustering.getSelected());
  }
  
  private void clusteringChanged(String clustering) {
    if (clustering == null) {
      return;
    }
    // create components
    params.clear();
    paramTitlesContainer.clear();
    paramListsContainer.clear();
    
    int numParams = lastAlgorithm.getParams().length;
    if (numParams == 0) {
      return;
    }
    
    Grid paramTitles = new Grid(numParams, 1);
    Grid paramLists = new Grid(numParams, 1);
    int i = 0;
    for (String p : lastAlgorithm.getParams()) {
      ClListBox box = new ClListBox();
      box.addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          updateClusteringList();
        }
      });
      
      // set up list box
      Collection<ProbeClustering> filter1 = ProbeClustering.filterByAlgorithm(probeClusterings, lastAlgorithm);
      Collection<ProbeClustering> filter2 = ProbeClustering.filterByClustering(filter1, clustering);
      box.setItems(ProbeClustering.collectParamValue(filter2, p));
      
      params.put(p, box);
      paramTitles.setText(i, 0, p);
      paramLists.setWidget(i, 0, box);
      ++i;
    }
    
    paramTitlesContainer.add(paramTitles);
    paramListsContainer.add(paramLists);
    updateClusteringList();
  }
  
  private void updateClusteringList() {
    // filter probe clusterings with current list selection
    Collection<ProbeClustering> filter1 = ProbeClustering.filterByAlgorithm(probeClusterings, lastAlgorithm);    
    Collection<ProbeClustering> filter2 = ProbeClustering.filterByClustering(filter1, clustering.getSelected());    
    for (Entry<String, ClListBox> e : params.entrySet()) {
      filter2 = ProbeClustering.filterByParam(filter2, e.getKey(), e.getValue().getSelected());
    }
    
    List<ItemList> items = new ArrayList<ItemList>();
    for (ProbeClustering pc : filter2) {
      items.add(pc.getList());
    }
    
    cluster.setLists(items);

    // Reset selection of cluster to avoid loading problem here.
    // Note: even if the clustering changed, if the clustering has the same cluster name
    // as previous clustering, it would not load probes expected.
    cluster.trySelect(null);
    loadedProbes.clear();

    updateAddButton();
  }

  @Override
  protected void initWidget(Widget widget) {
    super.initWidget(widget);

    Grid selector = new Grid(4, 2);
    selector.addStyleName("slightlySpaced");

    algorithm = new ClListBox();
    algorithm.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        algorithmChanged(algorithm.getSelected());
      }
    });
    selector.setText(0, 0, "Algorithm");
    selector.setWidget(0, 1, algorithm);

    clustering = new ClListBox();
    clustering.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        clusteringChanged(clustering.getSelected());
      }
    });
    selector.setText(1, 0, "Clustering");
    selector.setWidget(1, 1, clustering);

    paramTitlesContainer = Utils.mkVerticalPanel();
    paramListsContainer = Utils.mkVerticalPanel();
    selector.setWidget(2, 0, paramTitlesContainer);
    selector.setWidget(2, 1, paramListsContainer);

    cluster = new ListChooser(new ArrayList<StringList>(), ProbeClustering.PROBE_CLUSTERING_TYPE, 
      false) {
      @Override
      protected void itemsChanged(List<String> items) {
        loadedProbes.clear();
        loadedProbes.addAll(items);
        updateAddButton();
      }
    };
    selector.setText(3, 0, "Cluster");
    selector.setWidget(3, 1, cluster);

    addButton = new Button("Add probes >>");
    addButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent e) {
        clusterChanged(new ArrayList<String>(loadedProbes));
      }
    });
    addButton.setEnabled(false);

    VerticalPanel vp = Utils.mkVerticalPanel(true);
    vp.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
    vp.add(selector);
    vp.add(addButton);

    HorizontalPanel hp = Utils.wideCentered(vp);
    hp.addStyleName("colored");

    dp.add(Utils.wideCentered(hp));
    
    // set up algorithm list
    List<String> items = new ArrayList<String>();
    for (Algorithm algo : Algorithm.values()) {
      items.add(algo.getTitle());
    }
    algorithm.setItems(items);
    // force change event to refresh all selector
    algorithmChanged(algorithm.getSelected());
  }

  private void updateAddButton() {
    if (loadedProbes.size() > 0) {
      addButton.setEnabled(true);
    } else {
      addButton.setEnabled(false);
    }
  }

  @Override
  public void onResize() {
    dp.onResize();
  }

  // Expected to be overridden by caller
  public abstract void clusterChanged(List<String> items);
}
