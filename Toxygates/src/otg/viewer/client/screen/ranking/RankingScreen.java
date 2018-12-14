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

package otg.viewer.client.screen.ranking;

import static t.common.client.Utils.makeScrolled;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.*;
import otg.viewer.client.components.compoundsel.RankingCompoundSelector;
import t.common.shared.Dataset;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Utils;
import t.viewer.shared.ItemList;

public class RankingScreen extends MinimalScreen implements FilterTools.Delegate,
    RankingCompoundSelector.Delegate {

  public static final String key = "rank";

  private RankingCompoundSelector compoundSelector;
  private CompoundRanker compoundRanker;
  private FilterTools filterTools;
  private ScrollPanel sp;

  protected List<Dataset> chosenDatasets;
  private SampleClass chosenSampleClass;

  @Override
  public void loadState(AttributeSet attributes) {
    List<Dataset> newChosenDatasets = getStorage().datasetsStorage.getIgnoringException();
    filterTools.datasetsChanged(newChosenDatasets);
    SampleClass newSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    filterTools.sampleClassChanged(newSampleClass);
    compoundSelector.datasetsChanged(newChosenDatasets);
    compoundSelector.sampleClassChanged(newSampleClass);
    
    if (!newSampleClass.equals(chosenSampleClass) || !newChosenDatasets.equals(chosenDatasets)) {
      compoundSelector.fetchCompounds();
    }
    chosenDatasets = newChosenDatasets;
    chosenSampleClass = newSampleClass;
    
    compoundSelector.setChosenCompounds(getStorage().compoundsStorage.getIgnoringException());
  }

  public RankingScreen(ScreenManager man) {
    super("Compound ranking", key, man,
        man.resources().compoundRankingHTML(),
        man.resources().compoundRankingHelp());
    chosenDatasets = Arrays.asList(appInfo().datasets());
    filterTools = new FilterTools(this);

    compoundRanker = factory().compoundRanker(this);

    compoundSelector = new RankingCompoundSelector(this, man.schema().majorParameter().title()) {
      @Override
      public void itemListsChanged(List<ItemList> lists) {
        super.itemListsChanged(lists);
        compoundRanker.itemListsChanged(lists);
      }

      @Override
      protected void availableCompoundsChanged(List<String> compounds) {
        super.availableCompoundsChanged(compounds);
        compoundRanker.availableCompoundsChanged(compounds);
      }
    };
    compoundSelector.addStyleName("compoundSelector");

    compoundRanker.setSelector(compoundSelector);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    HorizontalPanel hp = Utils.mkHorizontalPanel(false, filterTools);
    addToolbar(hp, 0);
    addLeftbar(compoundSelector, 350);
  }

  @Override
  protected Widget content() {
    sp = makeScrolled(compoundRanker);
    return sp;
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public void resizeInterface() {
    // Test carefully in IE8, IE9 and all other browsers if changing this method
    compoundSelector.resizeInterface();
    super.resizeInterface();
  }

  @Override
  public String getGuideText() {
    return "Specify at least one gene symbol to rank compounds according to their effect.";
  }

  // CompoundSelector.Delegate methods
  @Override
  public void CompoundSelectorItemListsChanged(List<ItemList> itemLists) {
    getStorage().itemListsStorage.store(itemLists);
  }

  @Override
  public void CompoundSelectorCompoundsChanged(List<String> compounds) {
    RankingScreen.this.getStorage().compoundsStorage.store(compounds);
    compoundRanker.compoundsChanged(compounds);
  }

  // FilterTools.Delegate method
  @Override
  public void filterToolsSampleClassChanged(SampleClass sc) {
    getStorage().sampleClassStorage.store(sc);
    compoundSelector.sampleClassChanged(sc);
    compoundSelector.fetchCompounds();
  }

  @Override
  public void filterToolsDatasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
    getStorage().datasetsStorage.store(chosenDatasets);
    compoundSelector.datasetsChanged(chosenDatasets);
    compoundSelector.fetchCompounds();
  }
}
