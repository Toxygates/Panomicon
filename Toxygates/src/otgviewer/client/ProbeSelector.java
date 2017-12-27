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

import otgviewer.client.components.*;
import t.common.client.components.ResizingListBox;
import t.common.shared.SharedUtils;
import t.common.shared.Term;
import t.viewer.client.Utils;
import t.viewer.client.rpc.ProbeServiceAsync;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

/**
 * An interface component that helps users to select probes using some kind of higher level concept
 * (pathway, GO term etc)
 * 
 * Probe selection is a two-step process. First the user enters a partial name. A RPC call will then
 * search for items matching that name (for example pathways). The hits will be displayed. Next,
 * when the user selects one such object, the corresponding probes will be obtained.
 */
abstract public class ProbeSelector extends DataListenerWidget implements
    RequiresResize {

  private String[] loadedProbes;

  private boolean withButton;

  private DockLayoutPanel dp;
  private TermSuggestBox searchBox;

  private ListBox itemList;
  private Button addButton;

  private final ProbeServiceAsync probeService;

  private final static String CHILD_WIDTH = "100%";

  public ProbeSelector(Screen screen, String label, boolean wb) {
    this.probeService = screen.manager().probeService();
    this.withButton = wb;
    this.dp = new DockLayoutPanel(Unit.PX);
    initWidget(dp);

    VerticalPanel topVp = new VerticalPanel();
    topVp.setWidth(CHILD_WIDTH);
    topVp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    topVp.addStyleName("slightlySpaced");

    Label searchLabel = new Label(label);
    searchLabel.addStyleName("slightlySpaced");
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

    Button btnLoad = new Button("Load", (ClickHandler) ev -> {      
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
      });

    searchBox.addSelectionHandler(ev -> {           
        Term selected = searchBox.getSelected();
        if (selected != null) {
          getProbes(selected);
        }        
      });
    
    wrap = new FlowPanel();
    wrap.addStyleName("table-cell");
    wrap.add(btnLoad);
    fp.add(wrap);

    topVp.add(fp);

    dp.addNorth(topVp, 90);

    itemList = new ResizingListBox(135);
    itemList.setWidth(CHILD_WIDTH);

    if (withButton) {
      addButton = new Button("Add probes >>");
      addButton.addClickHandler(ev -> probesChanged(loadedProbes));        
      addButton.setEnabled(false);
      HorizontalPanel hp = Utils.wideCentered(addButton);
      hp.addStyleName("slightlySpaced");
      hp.setWidth(CHILD_WIDTH);
      dp.addSouth(hp, 35);
    }

    dp.add(itemList);
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
    dp.onResize();
  }

  /**
   * This callback should be supplied to the RPC methd that retrieves probes for a selection.
   * 
   * @return
   */
  public AsyncCallback<String[]> retrieveProbesCallback() {
    return new PendingAsyncCallback<String[]>(this) {
      @Override
      public void handleFailure(Throwable caught) {
        Window.alert("Unable to get probes.");
        // itemHandler.clear();
        addButton.setEnabled(false);
      }

      @Override
      public void handleSuccess(String[] probes) {
        if (!withButton) {
          probesChanged(probes);
        } else {        
          addButton.setEnabled(probes.length > 0); 
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
      probeService.geneSyms(probes, new PendingAsyncCallback<String[][]>(this, 
          "Unable to get gene symbols for probes") {
        @Override
        public void handleSuccess(String[][] syms) {
          deferredAddProbes(probes, syms);
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
