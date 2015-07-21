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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListChooser;
import t.common.shared.StringList;
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

public abstract class ClusteringSelector extends DataListenerWidget implements
    RequiresResize {

  private DockLayoutPanel dp;

  private List<ProbeClustering> probeClusterings;

  private ClListBox algorithm;
  private ClListBox param;
  private ClListBox clustering;
  private ListChooser cluster;

  private Button addButton;

  private Set<String> loadedProbes = new HashSet<String>();

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
    this.dp = new DockLayoutPanel(Unit.PX);
    
    initWidget(dp);
  }

  public void setAvailable(List<ProbeClustering> probeClusterings) {
    this.probeClusterings = probeClusterings;
    
    initializeList();
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

    param = new ClListBox();
    param.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        paramChanged(param.getSelected());
      }
    });
    selector.setText(1, 0, "K");
    selector.setWidget(1, 1, param);

    clustering = new ClListBox();
    clustering.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        clusteringChanged(clustering.getSelected());
      }
    });
    selector.setText(2, 0, "Clustering");
    selector.setWidget(2, 1, clustering);

    cluster = new ListChooser(new ArrayList<StringList>(), "probes", false) {
      @Override
      protected void itemsChanged(List<String> items) {
        loadedProbes.clear();
        loadedProbes.addAll(items);
        updateAddButton();
      }

      @Override
      protected void onDefaultItemSelected() {
        super.onDefaultItemSelected();
        loadedProbes.clear();
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
  }

  private void updateAddButton() {
    if (loadedProbes.size() > 0) {
      addButton.setEnabled(true);
    } else {
      addButton.setEnabled(false);
    }
  }

  private void initializeList() {
    algorithm.setItems(new ArrayList<String>(ProbeClustering
        .collectAlgorithm(probeClusterings)));

    String a = algorithm.getSelected();
    if (a != null) {
      changeParamsFrom(new ArrayList<ProbeClustering>(
          ProbeClustering.filterByAlgorithm(probeClusterings, a)));
    }
  }

  void changeParamsFrom(List<ProbeClustering> pc) {
    param.setItems(new ArrayList<String>(ProbeClustering.collectParam1(pc)));

    String p = param.getSelected();
    if (p != null) {
      changeClusteringFrom(new ArrayList<ProbeClustering>(
          ProbeClustering.filterByParam1(pc, p)));
    }
  }

  void changeClusteringFrom(List<ProbeClustering> pc) {
    clustering.setItems(new ArrayList<String>(ProbeClustering.collectName(pc)));

    String c = clustering.getSelected();
    if (c != null) {
      changeClusterFrom(new ArrayList<ProbeClustering>(
          ProbeClustering.filterByName(pc, c)));
    }
  }

  void changeClusterFrom(List<ProbeClustering> pc) {
    if (pc.size() != 1) {
      throw new IllegalArgumentException(
          "Only one item is expected, but given list contains " + pc.size()
              + " items");
    }

    cluster.setLists(pc.get(0).getClusters());
  }

  void algorithmChanged(String algorithm) {
    if (algorithm == null) {
      return;
    }
    changeParamsFrom(new ArrayList<ProbeClustering>(
        ProbeClustering.filterByAlgorithm(probeClusterings, algorithm)));
  }

  void paramChanged(String param) {
    if (param == null) {
      return;
    }

    List<ProbeClustering> filtered =
        new ArrayList<ProbeClustering>(ProbeClustering.filterByAlgorithm(
            probeClusterings, algorithm.getSelected()));

    changeClusteringFrom(new ArrayList<ProbeClustering>(
        ProbeClustering.filterByParam1(filtered, param)));
  }

  void clusteringChanged(String clustering) {
    if (clustering == null) {
      return;
    }

    List<ProbeClustering> filtered =
        new ArrayList<ProbeClustering>(ProbeClustering.filterByAlgorithm(
            probeClusterings, algorithm.getSelected()));
    filtered =
        new ArrayList<ProbeClustering>(ProbeClustering.filterByParam1(filtered,
            param.getSelected()));

    changeClusterFrom(new ArrayList<ProbeClustering>(
        ProbeClustering.filterByName(filtered, clustering)));
  }

  @Override
  public void onResize() {
    dp.onResize();
  }

  // Expected to be overridden by caller
  public abstract void clusterChanged(List<String> items);
}
