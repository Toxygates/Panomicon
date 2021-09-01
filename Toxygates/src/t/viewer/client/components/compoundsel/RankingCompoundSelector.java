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

package t.viewer.client.components.compoundsel;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import t.viewer.client.screen.Screen;
import t.gwt.common.client.ImageClickCell;
import t.gwt.common.client.Resources;
import t.shared.common.SeriesType;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.charts.SeriesCharts;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.future.Future;
import t.viewer.client.rpc.SeriesServiceAsync;
import t.shared.viewer.MatchResult;
import t.shared.viewer.RankRule;
import t.shared.viewer.Series;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingCompoundSelector extends CompoundSelector {

  private final SeriesServiceAsync seriesService;

  private Map<String, Integer> ranks = new HashMap<String, Integer>(); // for compound ranking
  private Map<String, MatchResult> scores = new HashMap<String, MatchResult>(); // for compound
                                                                                // ranking
  private List<String> rankProbes = new ArrayList<String>();
  private SeriesType rankedType = SeriesType.Time;
  private boolean hasRankColumns = false;
  private final Resources resources;
  
  private Delegate delegate;
  
  public interface Delegate extends CompoundSelector.Delegate {
    Future<MatchResult[]> getRankedCompounds(SeriesType seriesType, RankRule[] rules);
    SampleClass currentSampleClass();
  }
  
  public <T extends Screen & Delegate> RankingCompoundSelector(T screen, String heading) {
    this(screen, screen, heading);
  }

  public RankingCompoundSelector(final Screen screen, Delegate delegate, String heading) {
    super(screen, delegate, heading, false, false);
    this.delegate = delegate;
    this.seriesService = screen.manager().seriesService();
    this.resources = screen.resources();
  }

  private void addRankColumns() {
    if (!hasRankColumns) {
      CellTable<String> table = compoundEditor.table();
      TextColumn<String> textColumn = new TextColumn<String>() {
        @Override
        public String getValue(String object) {
          String r = "";

          if (scores.containsKey(object)) {
            r += Utils.formatNumber(scores.get(object).score());
          } else {
            r += "N/A";
          }
          if (ranks.containsKey(object)) {
            r += " (" + ranks.get(object) + ")";
          }
          return r;
        }
      };
      table.addColumn(textColumn, "Score");

      ChartClickCell ccc = new ChartClickCell();
      IdentityColumn<String> clickCol = new IdentityColumn<String>(ccc);
      clickCol.setCellStyleNames("clickCell");
      table.addColumn(clickCol, "");

      hasRankColumns = true;
    }
  }

  public void removeRankColumns() {
    if (hasRankColumns) {
      CellTable<String> table = compoundEditor.table();
      table.removeColumn(3); // chart icons
      table.removeColumn(2); // score
      rankProbes.clear();
      scores.clear();
      ranks.clear();
      hasRankColumns = false;
    }
  }
  
  public void acceptRankedCompounds(MatchResult[] result) {
    ranks.clear();
    int rnk = 1;
    List<String> sortedCompounds = new ArrayList<String>();
    for (MatchResult r : result) {
      scores.put(r.compound(), r);
      sortedCompounds.add(r.compound());
      ranks.put(r.compound(), rnk);
      rnk++;
    }
    compoundEditor.setItems(sortedCompounds, false, false);
    compoundEditor.scrollBrowseCheckToTop();
  }

  public void performRanking(SeriesType seriesType, List<String> rankProbes, List<RankRule> rules) {
    if (rules.size() > 0) {
      this.rankProbes = rankProbes;
      this.rankedType = seriesType;
      addRankColumns();
      delegate.getRankedCompounds(seriesType, rules.toArray(new RankRule[0]));
    } else {
      Window.alert("Please specify and enable at least one rule to perform the ranking.");
    }
  }

  class ChartClickCell extends ImageClickCell.StringImageClickCell {

    public ChartClickCell() {
      super(resources.chart(), "charts", false);
    }

    @Override
    public void onClick(final String compoundName) {
      if (rankProbes.size() == 0) {
        Window.alert("These charts can only be displayed if compounds have been ranked.");
      } else {
        seriesService.getSeries(rankedType, delegate.currentSampleClass(), rankProbes.toArray(new String[0]),
            null, new String[] {compoundName}, getSeriesCallback(compoundName));
      }
      Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_COMPOUND_RANKING_CHARTS);
    }

    private AsyncCallback<List<Series>> getSeriesCallback(final String value) {
      return new PendingAsyncCallback<List<Series>>(screen.manager(), "Unable to retrieve data.") {
        @Override
        public void handleSuccess(final List<Series> series) {
          Utils.ensureVisualisationAndThen(new Runnable() {
            @Override
            public void run() {
              makeSeriesCharts(rankedType, value, series);
            }
          });

        }
      };
    }

    private void makeSeriesCharts(SeriesType seriesType, String compoundName, List<Series> ss) {
      SeriesCharts cgf = new SeriesCharts(screen, new SampleClass[] {
          screen.manager().getStorage().sampleClassStorage.getIgnoringException()});
      cgf.make(seriesType, ss, scores.get(compoundName).fixedValue(),
          cg -> Utils.displayInPopup("Charts", cg, DialogPosition.Side), screen, compoundName);
    }
  }
}
