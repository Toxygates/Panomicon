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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListChooser;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.clustering.Algorithm;
import t.common.shared.clustering.ProbeClustering;
import t.viewer.client.Utils;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class ClusteringSelector extends DataListenerWidget implements RequiresResize {

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
    
    for (ProbeClustering pc : clusterings) {
      logger.warning("Recieved : " + pc.getClustering().getTitle());
    }

    initWidget(dp);
  }
  
  private void algorithmChanged(String algorithm) {
    logger.info("algorithmChanged");

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
    logger.info("clusteringChanged");
    
    if (clustering == null) {
      logger.info("clustering == null");
      return;
    }
    // create components
    params.clear();
    paramTitlesContainer.clear();
    paramListsContainer.clear();
    
    int numParams = lastAlgorithm.getParams().length;
    if (numParams == 0) {
      logger.info("numParams == 0");
      return;
    }
    
    Grid paramTitles = new Grid(numParams, 1);
    Grid paramLists = new Grid(numParams, 1);
    int i = 0;
    for (String p : lastAlgorithm.getParams()) {
      logger.info("param: " + p);
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
    // TODO consider behavior of ListChooser
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

    cluster = new ListChooser(new ArrayList<StringList>(), "probes", false) {
      @Override
      protected void itemsChanged(List<String> items) {
        loadedProbes.clear();
        loadedProbes.addAll(items);
        updateAddButton();
      }
    };
    addListener(cluster);
    selector.setText(3, 0, "Cluster");
    selector.setWidget(3, 1, cluster);

    addButton = new Button("Add probes >>");
    addButton.addClickHandler(new ClickHandler() {
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
    hp.setStylePrimaryName("colored");

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
