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

package otgviewer.client.components.ranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import otgviewer.client.GeneOracle;
import otgviewer.client.Resources;
import otgviewer.client.StringListsStoreHelper;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListChooser;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.shared.RankRule;
import t.common.shared.DataSchema;
import t.common.shared.ItemList;
import t.common.shared.SampleClass;
import t.viewer.client.Utils;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This widget is an UI for defining compound ranking rules. The actual ranking is requested by a
 * compound selector and performed on the server side.
 */
abstract public class CompoundRanker extends DataListenerWidget {
  protected final Resources resources;
  final RankingCompoundSelector selector;
  protected final Screen screen;
  protected ListChooser listChooser;

  final GeneOracle oracle;
  List<String> availableCompounds = chosenCompounds;

  protected final SparqlServiceAsync sparqlService;

  protected VerticalPanel csVerticalPanel = new VerticalPanel();
  protected List<String> rankProbes = new ArrayList<String>();

  protected List<RuleInputHelper> inputHelpers = new ArrayList<RuleInputHelper>();

  protected Grid grid;

  final DataSchema schema;

  /**
   * 
   * @param selector the selector that this CompoundRanker will communicate with.
   */
  public CompoundRanker(Screen _screen, RankingCompoundSelector selector) {
    this.selector = selector;
    screen = _screen;
    oracle = new GeneOracle(screen);
    schema = screen.schema();
    resources = screen.resources();
    sparqlService = _screen.manager().sparqlService();

    selector.addListener(this);
    listChooser = new ListChooser(screen.appInfo().predefinedProbeLists(), "probes") {
      @Override
      protected void preSaveAction() {
        String[] probes = getProbeList().toArray(new String[0]);
        // We override this to pull in the probes, because they
        // may need to be converted from gene symbols.

        sparqlService.identifiersToProbes(probes, true, false, false, null,
            new PendingAsyncCallback<String[]>(this) {
              public void handleSuccess(String[] resolved) {
                setItems(Arrays.asList(resolved));
                saveAction();
              }
            });
      }

      @Override
      protected void itemsChanged(List<String> items) {
        sparqlService.identifiersToProbes(items.toArray(new String[0]), true, 
            false, false, null,
            new PendingAsyncCallback<String[]>(this) {
              public void handleSuccess(String[] resolved) {
                setProbeList(Arrays.asList(resolved));
              }
            });
      }

      @Override
      protected void listsChanged(List<ItemList> lists) {
        screen.itemListsChanged(lists);
        screen.storeItemLists(getParser(screen));
      }
    };
    listChooser.setStylePrimaryName("colored");
    selector.addListener(listChooser);

    csVerticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    initWidget(csVerticalPanel);

    Button clearBtn = new Button("Clear rules", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        clearRules();
      }
    });
    
    Button setAllBtn = new Button("Set all... ", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (Window.confirm("Overwrite all rule parameters by copying the first rule?")) {
          setAllRules();
        }        
      }
      
    });

    HorizontalPanel hp =
        Utils.mkHorizontalPanel(true, new Label("Gene set: "), listChooser, setAllBtn, clearBtn);
    csVerticalPanel.add(hp);

    grid = new Grid(1, gridColumns()); // Initially space for 1 rule
    csVerticalPanel.add(grid);
    addHeaderWidgets();

    addRule(true);

    hp = Utils.mkHorizontalPanel(true);
    csVerticalPanel.add(hp);

    hp.add(new Button("Rank", new ClickHandler() {
      public void onClick(ClickEvent event) {
        performRanking();
      }
    }));
  }

  protected abstract int gridColumns();

  protected abstract void addHeaderWidgets();

  protected abstract RuleInputHelper makeInputHelper(boolean isLast);

  void addRule(boolean isLast) {
    int ruleIdx = inputHelpers.size();
    grid.resize(ruleIdx + 2, gridColumns());
    RuleInputHelper rih = makeInputHelper(isLast);
    inputHelpers.add(rih);
    rih.populate(grid, ruleIdx);
  }

  private void clearRules() {
    setProbeList(new ArrayList<String>());
  }
  
  private void setAllRules() {
    RuleInputHelper top = inputHelpers.get(0);
    try {
      for (int i = 1; i < inputHelpers.size(); i++) {    
        inputHelpers.get(i).copyFrom(top);
      }
    } catch (RankRuleException rre) {
      Window.alert(rre.getMessage());
    }
  }

  /**
   * Map the current ranking rules to a list of probes and return the result.
   * 
   * @return
   */
  private List<String> getProbeList() {
    List<String> r = new ArrayList<String>();
    for (RuleInputHelper rih : inputHelpers) {
      if (!rih.probeText.getText().equals("")) {
        String probe = rih.probeText.getText();
        r.add(probe);
      }
    }
    return r;
  }

  /**
   * Replace the current ranking rules with new rules generated from a list of probes.
   * 
   * @param probes
   */
  private void setProbeList(List<String> probes) {
    while (inputHelpers.size() > probes.size()) {
      inputHelpers.remove(inputHelpers.size() - 1);
    }
    grid.resize(probes.size() + 1, gridColumns());
    for (RuleInputHelper rih : inputHelpers) {
      rih.reset();
    }

    for (int i = 0; i < probes.size(); ++i) {
      String probe = probes.get(i);
      if (i >= inputHelpers.size()) {
        addRule(true);
      }
      inputHelpers.get(i).probeText.setText(probe);
      inputHelpers.get(i).enabled.setValue(true);
    }
    if (probes.size() == inputHelpers.size()) {
      addRule(true);
    }
  }

  private void performRanking() {
    List<RankRule> rules = new ArrayList<RankRule>();
    rankProbes = new ArrayList<String>();
    for (RuleInputHelper rih : inputHelpers) {
      if (rih.enabled.getValue()) {
        if (rih.probeText.getText().equals("")) {
          Window
              .alert("Empty gene name detected. Please specify a gene/probe for each enabled rule.");
        } else {
          String probe = rih.probeText.getText();
          rankProbes.add(probe);
          try {
            rules.add(rih.getRule());
          } catch (RankRuleException rre) {
            Window.alert(rre.getMessage());
          }
        }
      }
    }

    selector.performRanking(rankProbes, rules);
  }


  @Override
  public void sampleClassChanged(SampleClass sc) {
    super.sampleClassChanged(sc);
    oracle.setFilter(sc);
    for (RuleInputHelper rih : inputHelpers) {
      rih.sampleClassChanged(sc);
    }
  }

  @Override
  public void availableCompoundsChanged(List<String> compounds) {
    super.availableCompoundsChanged(compounds);
    availableCompounds = compounds;
    for (RuleInputHelper rih : inputHelpers) {
      rih.availableCompoundsChanged(compounds);
    }
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    listChooser.setLists(StringListsStoreHelper.compileLists(this));
  }

  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    super.clusteringListsChanged(lists);     
    listChooser.setLists(StringListsStoreHelper.compileLists(this));
  }
  
  
}
