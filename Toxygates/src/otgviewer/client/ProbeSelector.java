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

import java.util.Arrays;
import java.util.List;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListSelectionHandler;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.TermSuggestBox;
import t.common.client.components.ResizingListBox;
import t.common.shared.SharedUtils;
import t.common.shared.Term;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * An interface component that helps users to select probes using some kind of higher level concept
 * (pathway, GO term etc)
 * 
 * Probe selection is a two-step process. First the user enters a partial name. A RPC call will then
 * search for items matching that name (for example pathways). The hits will be displayed. Next,
 * when the user selects one such object, the corresponding probes will be obtained.
 * 
 * @author johan
 *
 */
abstract public class ProbeSelector extends DataListenerWidget implements
    RequiresResize {

  private String[] loadedProbes;

  private boolean withButton;

  private DockLayoutPanel lp;
  private TermSuggestBox searchBox;

  private ListBox itemList;
  private Button addButton;

  private final SparqlServiceAsync sparqlService;

  private final static String CHILD_WIDTH = "100%";

  public ProbeSelector(Screen screen, String label, boolean wb) {
    this.sparqlService = screen.sparqlService();
    this.withButton = wb;
    this.lp = new DockLayoutPanel(Unit.PX);
    initWidget(lp);

    VerticalPanel topVp = new VerticalPanel();
    topVp.setWidth(CHILD_WIDTH);
    topVp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    topVp.setStylePrimaryName("slightlySpaced");

    Label searchLabel = new Label(label);
    searchLabel.setStylePrimaryName("slightlySpaced");
    searchLabel.setWidth("95%");
    topVp.add(searchLabel);

    searchBox = new TermSuggestBox(new TermSuggestOracle(screen));
    searchBox.setWatermark("KEGG Pathway, GO term...");
    searchBox.setWidth("100%");

    FlowPanel fp = new FlowPanel();
    fp.setWidth("100%");

    FlowPanel wrap = new FlowPanel();
    wrap.addStyleName("table-cell width-fix");
    wrap.setWidth("100%");
    wrap.add(searchBox);
    fp.add(wrap);

    Button btnLoad = new Button("Load");
    btnLoad.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        itemList.clear();
        loadedProbes = new String[0];
        if (withButton) {
          addButton.setEnabled(false);
        }
        Term selected = searchBox.getSelected();
        if (selected != null) {
          getProbes(selected);
        } else {
          searchBox.showSuggestionList();
        }
      }
    });
    wrap = new FlowPanel();
    wrap.addStyleName("table-cell");
    wrap.add(btnLoad);
    fp.add(wrap);

    topVp.add(fp);

    lp.addNorth(topVp, 90);

    itemList = new ResizingListBox(135);
    itemList.setWidth(CHILD_WIDTH);

    if (withButton) {
      addButton = new Button("Add probes >>");
      addButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent e) {
          probesChanged(loadedProbes);
        }
      });
      addButton.setEnabled(false);
      HorizontalPanel hp = Utils.wideCentered(addButton);
      hp.setStylePrimaryName("slightlySpaced");
      hp.setWidth(CHILD_WIDTH);
      lp.addSouth(hp, 35);
    }

    lp.add(itemList);
  }

  /**
   * This method should obtain the probes that correspond to the exactly named high level object.
   * (Will be invoked after the user selects one)
   * 
   * @param item
   */
  abstract protected void getProbes(Term term);

  @Override
  public void onResize() {
    lp.onResize();
  }

  /**
   * This callback should be supplied to the RPC method that retrieves high level objects for a
   * partial name.
   * 
   * @return
   */
  // public AsyncCallback<String[]> retrieveMatchesCallback() {
  // return itemHandler.retrieveCallback(this, true);
  // }

  /**
   * This method should obtain the high level objects that correspond to the partial name. It will
   * be invoked after the user types a partial name and presses enter.
   * 
   * @param key
   */
  // abstract protected void getMatches(String key);

  /**
   * This callback should be supplied to the RPC methd that retrieves probes for a selection.
   * 
   * @return
   */
  public AsyncCallback<String[]> retrieveProbesCallback() {
    return new PendingAsyncCallback<String[]>(this) {
      public void handleFailure(Throwable caught) {
        Window.alert("Unable to get probes.");
        // itemHandler.clear();
        addButton.setEnabled(false);
      }

      public void handleSuccess(String[] probes) {
        if (!withButton) {
          probesChanged(probes);
        } else if (probes.length > 0) {
          addButton.setEnabled(true);
          loadedProbes = probes;
          probesLoaded(loadedProbes);
        }
      }
    };
  }

  protected void probesLoaded(final String[] probes) {
    if (probes.length > 0) {
      Arrays.sort(probes);
      // TODO reduce the number of ajax calls done by this screen by
      // collapsing them
      sparqlService.geneSyms(probes, new AsyncCallback<String[][]>() {
        public void onSuccess(String[][] syms) {
          deferredAddProbes(probes, syms);
        }

        public void onFailure(Throwable caught) {
          Window.alert("Unable to get gene symbols for probes.");
        }
      });
    }
  }

  /**
   * Display probes with gene symbols. Probes must be unique.
   * 
   * @param probes
   * @param syms
   */
  private void deferredAddProbes(String[] probes, String[][] syms) {
    itemList.clear();
    for (int i = 0; i < probes.length; ++i) {
      if (syms[i].length > 0) {
        itemList.addItem(SharedUtils.mkString(syms[i], "/") + " (" + probes[i]
            + ")");
      } else {
        itemList.addItem(probes[i]);
      }
    }
  }

  void clear() {
    searchBox.setText("");
    itemList.clear();
    loadedProbes = new String[0];
  }
}
