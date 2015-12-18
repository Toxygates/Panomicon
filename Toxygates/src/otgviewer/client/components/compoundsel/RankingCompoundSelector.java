package otgviewer.client.components.compoundsel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.client.Resources;
import otgviewer.client.charts.ChartGrid;
import otgviewer.client.charts.Charts;
import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.MatchResult;
import otgviewer.shared.RankRule;
import otgviewer.shared.Series;
import t.common.client.ImageClickCell;
import t.common.shared.SampleClass;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SeriesServiceAsync;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class RankingCompoundSelector extends CompoundSelector {

  private final SeriesServiceAsync seriesService;

  private Map<String, Integer> ranks = new HashMap<String, Integer>(); // for compound ranking
  private Map<String, MatchResult> scores = new HashMap<String, MatchResult>(); // for compound
                                                                                // ranking
  private List<String> rankProbes = new ArrayList<String>();
  private boolean hasRankColumns = false;
  private final Resources resources;
  
  public RankingCompoundSelector(Screen screen, String heading) {
    super(screen, heading, false    , false);
    this.seriesService = screen.seriesService();
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

      ChartClickCell ccc = new ChartClickCell(this);
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

  public void performRanking(List<String> rankProbes, List<RankRule> rules) {
    this.rankProbes = rankProbes;
    addRankColumns();

    if (rules.size() > 0) { // do we have at least 1 rule?
      seriesService.rankedCompounds(chosenDatasets, chosenSampleClass,
          rules.toArray(new RankRule[0]), new PendingAsyncCallback<MatchResult[]>(this) {
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

            public void handleFailure(Throwable caught) {
              Window.alert("Unable to rank compounds: " + caught.getMessage());
            }
          });
    } else {
      Window.alert("Please specify and enable at least one rule to perform the ranking.");
    }
  }

  class ChartClickCell extends ImageClickCell.StringImageClickCell {
    final DataListenerWidget w;

    public ChartClickCell(DataListenerWidget w) {
      super(resources.chart(), false);
      this.w = w;
    }

    public void onClick(final String value) {
      if (rankProbes.size() == 0) {
        Window.alert("These charts can only be displayed if compounds have been ranked.");
      } else {
        seriesService.getSeries(chosenSampleClass, rankProbes.toArray(new String[0]), null,
            new String[] {value}, getSeriesCallback(value));
      }
    }

    private AsyncCallback<List<Series>> getSeriesCallback(final String value) {
      return new PendingAsyncCallback<List<Series>>(w, "Unable to retrieve data.") {
        public void handleSuccess(final List<Series> ss) {
          Utils.ensureVisualisationAndThen(new Runnable() {
            public void run() {
              makeSeriesCharts(value, ss);
            }
          });

        }
      };
    }

    private void makeSeriesCharts(final String value, final List<Series> ss) {
      Charts cgf = new Charts(screen, new SampleClass[] {w.chosenSampleClass});
      cgf.makeSeriesCharts(ss, false, scores.get(value).dose(), new Charts.ChartAcceptor() {

        @Override
        public void acceptCharts(ChartGrid<?> cg) {
          Utils.displayInPopup("Charts", cg, DialogPosition.Side);
        }
      }, screen);
    }
  }


}
