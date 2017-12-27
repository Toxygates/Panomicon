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

package otgviewer.client.components.ranking;

import java.util.*;

import otgviewer.client.*;
import otgviewer.client.components.*;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.shared.RankRule;
import t.common.shared.DataSchema;
import t.common.shared.ItemList;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.rpc.ProbeServiceAsync;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

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

  protected final ProbeServiceAsync probeService;
  protected final SampleServiceAsync sampleService;

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
    probeService = _screen.manager().probeService();
    sampleService = _screen.manager().sampleService();

    selector.addListener(this);
    listChooser = new ListChooser(screen.appInfo().predefinedProbeLists(), "probes") {
      @Override
      protected void preSaveAction() {
        String[] probes = getProbeList().toArray(new String[0]);
        // We override this to pull in the probes, because they
        // may need to be converted from gene symbols.

        probeService.identifiersToProbes(probes, true, false, false, null,
            new PendingAsyncCallback<String[]>(this) {
              @Override
              public void handleSuccess(String[] resolved) {
                setItems(Arrays.asList(resolved));
                saveAction();
              }
            });
      }

      @Override
      protected void itemsChanged(List<String> items) {
        probeService.identifiersToProbes(items.toArray(new String[0]), true, 
            false, false, null,
            new PendingAsyncCallback<String[]>(this) {
              @Override
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
    listChooser.addStyleName("colored");
    selector.addListener(listChooser);

    csVerticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    initWidget(csVerticalPanel);

    Button clearBtn = new Button("Clear rules", (ClickHandler) e -> clearRules());      
    
    Button setAllBtn = new Button("Set all... ", (ClickHandler) e -> {
      if (Window.confirm("Overwrite all rule parameters by copying the first rule?")) {
        setAllRules();
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

    hp.add(new Button("Rank", (ClickHandler) e -> performRanking()));      
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
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_COMPOUND_RANKING);
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
    if (!compounds.equals(availableCompounds)) {
      for (RuleInputHelper rih : inputHelpers) {
        rih.availableCompoundsChanged(compounds);
      }
    }
    availableCompounds = compounds;
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    super.itemListsChanged(lists);
    listChooser.setLists(StringListsStoreHelper.compileLists(this.state()));
  }

  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    super.clusteringListsChanged(lists);     
    listChooser.setLists(StringListsStoreHelper.compileLists(this.state()));
  }
  
  
}
