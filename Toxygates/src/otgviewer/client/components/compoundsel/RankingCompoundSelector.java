/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package otgviewer.client.components.compoundsel;

import java.util.*;

import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import otgviewer.client.charts.ChartGrid;
import otgviewer.client.charts.Charts;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.*;
import t.common.client.ImageClickCell;
import t.common.client.Resources;
import t.common.shared.SeriesType;
import t.common.shared.SharedUtils;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SeriesServiceAsync;

public class RankingCompoundSelector extends CompoundSelector {

  private final SeriesServiceAsync seriesService;

  private Map<String, Integer> ranks = new HashMap<String, Integer>(); // for compound ranking
  private Map<String, MatchResult> scores = new HashMap<String, MatchResult>(); // for compound
                                                                                // ranking
  private List<String> rankProbes = new ArrayList<String>();
  private SeriesType rankedType = SeriesType.Time;
  private boolean hasRankColumns = false;
  private final Resources resources;

  public <T extends Screen & Delegate> RankingCompoundSelector(T screen,
      String heading) {
    this(screen, screen, heading);
  }

  public RankingCompoundSelector(final Screen screen, Delegate delegate,
      String heading) {
    super(screen, delegate, heading, false, false);
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

      ChartClickCell ccc = new ChartClickCell(screen);
      IdentityColumn<String> clickCol = new IdentityColumn<String>(ccc);
      clickCol.setCellStyleNames("clickCell");
      table.addColumn(clickCol, "");

      hasRankColumns = true;
    }
  }

  private void removeRankColumns() {
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
  
  private SampleClass lastClass;
  
  @Override
  public void sampleClassChanged(SampleClass sc) {
      super.sampleClassChanged(sc);               
      if (lastClass == null || !sc.equals(lastClass)) {
          removeRankColumns();
      }
      lastClass = sc;          
  }

  public void performRanking(SeriesType seriesType, 
      List<String> rankProbes, List<RankRule> rules) {
    this.rankProbes = rankProbes;
    this.rankedType = seriesType;
    addRankColumns();
    logger.info("Ranking compounds for datasets: " + 
        SharedUtils.mkString(Arrays.asList(chosenDatasets), " "));

    if (rules.size() > 0) { // do we have at least 1 rule?
      seriesService.rankedCompounds(seriesType, chosenDatasets, chosenSampleClass,
          rules.toArray(new RankRule[0]), new PendingAsyncCallback<MatchResult[]>(screen) {
            @Override
            public void handleSuccess(MatchResult[] res) {
              ranks.clear();
              int rnk = 1;
              List<String> sortedCompounds = new ArrayList<String>();
              for (MatchResult r : res) {
                scores.put(r.compound(), r);
                sortedCompounds.add(r.compound());
                ranks.put(r.compound(), rnk);
                rnk++;
              }
              compoundEditor.setItems(sortedCompounds, false, false);
              compoundEditor.displayPicker();
            }

            @Override
            public void handleFailure(Throwable caught) {
              Window.alert("Unable to rank compounds: " + caught.getMessage());
            }
          });
    } else {
      Window.alert("Please specify and enable at least one rule to perform the ranking.");
    }
  }

  class ChartClickCell extends ImageClickCell.StringImageClickCell {
    final Screen w;

    public ChartClickCell(Screen w) {
      super(resources.chart(), "charts", false);
      this.w = w;
    }

    @Override
    public void onClick(final String value) {
      if (rankProbes.size() == 0) {
        Window.alert("These charts can only be displayed if compounds have been ranked.");
      } else {
        seriesService.getSeries(rankedType,
            chosenSampleClass, rankProbes.toArray(new String[0]), null,
            new String[] {value}, getSeriesCallback(value));
      }
      Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_COMPOUND_RANKING_CHARTS);
    }

    private AsyncCallback<List<Series>> getSeriesCallback(final String value) {
      return new PendingAsyncCallback<List<Series>>(w, "Unable to retrieve data.") {
        @Override
        public void handleSuccess(final List<Series> ss) {
          Utils.ensureVisualisationAndThen(new Runnable() {
            @Override
            public void run() {
              makeSeriesCharts(rankedType, value, ss);
            }
          });

        }
      };
    }

    private void makeSeriesCharts(SeriesType seriesType, String value, List<Series> ss) {
      Charts cgf = new Charts(screen, new SampleClass[] { w.manager().getParser().getSampleClass(w.attributes()) });
      cgf.makeSeriesCharts(seriesType, ss, scores.get(value).fixedValue(), 
          new Charts.ChartAcceptor() {
        @Override
        public void acceptCharts(ChartGrid<?> cg) {
          Utils.displayInPopup("Charts", cg, DialogPosition.Side);
        }
      }, screen);
    }
  }
}