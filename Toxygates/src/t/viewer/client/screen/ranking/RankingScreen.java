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

package t.viewer.client.screen.ranking;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import t.shared.common.Dataset;
import t.shared.common.SeriesType;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.Utils;
import t.viewer.client.components.FilterTools;
import t.viewer.client.components.compoundsel.RankingCompoundSelector;
import t.viewer.client.future.Future;
import t.viewer.client.future.FutureUtils;
import t.viewer.client.screen.FilterAndSelectorScreen;
import t.viewer.client.screen.ScreenManager;
import t.shared.viewer.MatchResult;
import t.shared.viewer.RankRule;

import java.util.List;

import static t.common.client.Utils.makeScrolled;

public class RankingScreen extends FilterAndSelectorScreen implements FilterTools.Delegate,
    RankingCompoundSelector.Delegate {

  public static final String key = "rank";

  private RankingCompoundSelector rankingSelector;
  private CompoundRanker compoundRanker;
  private ScrollPanel sp;

  @Override
  public void loadState(AttributeSet attributes) {
    compoundRanker.geneSetsChanged(getStorage().geneSetsStorage.getIgnoringException());
    compoundRanker.clusteringListsChanged(getStorage().clusteringListsStorage.getIgnoringException());
    // Clear the rankings if we get new compounds as a result of dataset/sampleclass changes
    loadDatasetsAndSampleClass(attributes).addSuccessCallback(c -> {
      rankingSelector.removeRankColumns();
    });
  }

  public RankingScreen(ScreenManager man) {
    super("Compound ranking", key, man,
        man.resources().compoundRankingHTML(),
        man.resources().compoundRankingHelp());
    filterTools = new FilterTools(this);

    compoundRanker = factory().compoundRanker(this);

    rankingSelector = new RankingCompoundSelector(this, man.schema().majorParameter().title()) {
      @Override
      protected void availableCompoundsChanged(List<String> compounds) {
        super.availableCompoundsChanged(compounds);
        compoundRanker.availableCompoundsChanged(compounds, chosenSampleClass);
      }
    };
    compoundSelector = rankingSelector;
    compoundSelector.addStyleName("compoundSelector");

    compoundRanker.setSelector(rankingSelector);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    HorizontalPanel hp = Utils.mkHorizontalPanel(false, filterTools);
    addToolbar(hp, 45);
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
  
  
  // RankingCompoundSelector.Delegate methods
  @Override
  public Future<MatchResult[]> getRankedCompounds(SeriesType seriesType, 
      RankRule[] rules) {
    Future<MatchResult[]> future = new Future<MatchResult[]>();
    manager().seriesService().rankedCompounds(seriesType, 
        chosenDatasets.toArray(new Dataset[0]),
        filterTools.dataFilterEditor.currentSampleClassShowing(), rules, future);
    FutureUtils.beginPendingRequestHandling(future, manager(), "Unable to rank compounds");
    
    future.addSuccessCallback(result -> {
      rankingSelector.acceptRankedCompounds(result);
    });
    return future;
  }
  
  @Override
  public SampleClass currentSampleClass() {
    return chosenSampleClass;
  }

  // CompoundSelector.Delegate methods
  @Override
  public void compoundSelectorCompoundsChanged(List<String> compounds) {
    super.compoundSelectorCompoundsChanged(compounds);
    compoundRanker.compoundsChanged(compounds);
  }

  // FilterTools.Delegate methods
  @Override
  public void filterToolsSampleClassChanged(SampleClass sampleClass) {
    super.filterToolsSampleClassChanged(sampleClass);
    rankingSelector.removeRankColumns();
  }
}
