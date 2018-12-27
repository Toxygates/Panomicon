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

package otg.viewer.client.charts;

import java.util.List;

import com.google.gwt.user.client.ui.*;

import otg.model.sample.OTGAttribute;
import otg.viewer.client.charts.google.GVizChartGrid;
import otg.viewer.client.components.OTGScreen;
import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.components.PendingAsyncCallback;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.ProbeServiceAsync;

/**
 * A grid to display time (or dose) series charts for a number of probes and doses (or times).
 */
abstract public class ChartGrid<D extends Data> extends Composite {

  protected final ProbeServiceAsync probeService;

  Grid g;

  List<String> rowFilters;
  List<String> organisms;
  String[] minsOrMeds;
  protected Dataset<D> dataset;
  protected Factory<?, ?> factory;
  protected OTGScreen screen;
  protected D[][] tables;
  final int totalWidth;
  final static String NO_ORGANISM = "";

  /**
   * @param rowFilters major parameter values or gene symbols, depending on the chart type.
   * @param rowsAreMajors are rows major parameter values? If not, they are gene symbols.
   */
  public ChartGrid(Factory<D, ?> factory, OTGScreen screen, Dataset<D> dataset,
      final List<String> rowFilters, final List<String> organisms, boolean rowsAreMajors,
      String[] minsOrMeds, boolean columnsAreMins, int totalWidth) {
    super();
    this.factory = factory;
    this.organisms = organisms;
    this.rowFilters = rowFilters;
    this.minsOrMeds = minsOrMeds;
    this.dataset = dataset;
    this.totalWidth = totalWidth;
    this.screen = screen;
    probeService = screen.manager().probeService();

    if (organisms.size() == 0) {
      organisms.add(NO_ORGANISM);
    }

    final int osize = organisms.size();
    final int rfsize = rowFilters.size();
    g = new Grid(rfsize * osize * 2 + 1, minsOrMeds.length);
    initWidget(g);

    DataSchema schema = screen.manager().schema();


    tables = factory.dataArray(rfsize * osize, minsOrMeds.length);
    for (int c = 0; c < minsOrMeds.length; ++c) {
      g.setWidget(0, c, Utils.mkEmphLabel(minsOrMeds[c]));
      for (int r = 0; r < rfsize; ++r) {
        for (int o = 0; o < osize; ++o) {
          String org = organisms.get(o).equals(NO_ORGANISM) ? null : organisms.get(o);
          SampleClass sc = new SampleClass();
          String probe = null;
          if (rowsAreMajors) {
            sc.put(schema.majorParameter(), rowFilters.get(r));
          } else {
            probe = rowFilters.get(r);
          }
          if (org != null) {
            sc.put(OTGAttribute.Organism, org);
          }
          Attribute colKey = columnsAreMins ? schema.minorParameter() : schema.mediumParameter();
          sc.put(colKey, minsOrMeds[c]);
          tables[r * osize + o][c] = dataset.makeData(sc, probe);
        }
      }
    }

    if (!rowsAreMajors) {
      probeService.geneSyms(rowFilters.toArray(new String[0]),
          new PendingAsyncCallback<String[][]>(screen) {
            @Override
            public void handleSuccess(String[][] results) {
              for (int i = 0; i < results.length; ++i) {
                g.setWidget(i * 2 + 1, 0,
                    Utils.mkEmphLabel(SharedUtils.mkString(results[i]) + "/" + rowFilters.get(i)));
              }
            }
          });
    }

  }

  public int computedTotalWidth() {
    int theoretical = g.getColumnCount() * GVizChartGrid.MAX_WIDTH;
    if (theoretical > totalWidth) {
      return totalWidth;
    } else {
      return theoretical;
    }
  }

  /**
   * Obtain the largest number of data columns used in any of our backing tables.@return
   */
  public int getMaxColumnCount() {
    int max = 0;
    for (int r = 0; r < tables.length; ++r) {
      for (int c = 0; c < tables[0].length; ++c) {
        if (tables[r][c].numberOfColumns() > max) {
          max = tables[r][c].numberOfColumns();
        }
      }
    }
    return max;
  }

  void adjustAndDisplay(ChartStyle style, int tableColumnCount, double minVal, double maxVal) {
    int width = totalWidth / minsOrMeds.length; // width of each individual chart
    int osize = organisms.size();
    ChartStyle innerStyle = style.withWidth(width);
    for (int c = 0; c < minsOrMeds.length; ++c) {
      for (int r = 0; r < rowFilters.size(); ++r) {
        for (int o = 0; o < osize; ++o) {
          String label = organisms.get(o) + ":" + rowFilters.get(r);
          displayAt(innerStyle, r * osize + o, c, minVal, maxVal, tableColumnCount, label);
        }
      }
    }
  }

  /**
   * We normalise the column count of each data table when displaying it in order to force the
   * charts to have equally wide bars. (To the greatest extent possible)
   */
  private void displayAt(final ChartStyle style, final int row, final int column, final double minVal,
      final double maxVal, final int columnCount, String label) {
    final D dt = tables[row][column];

    if (dt.numberOfColumns() == 1) {
      return; // no data columns -> no data to show
    }
    if (g.getWidget(row * 2 + 1, 0) == null) {
      // add the label if this is the first chart for the rowFilter
      g.setWidget(row * 2 + 1, 0, Utils.mkEmphLabel(label));
    }
    final HTML downloadLink = new HTML();
    VerticalPanel vp = new VerticalPanel();
    final ChartStyle innerStyle = style.withDownloadLink(downloadLink);
    
    vp.add(chartFor(dt, innerStyle.withBigMode(false), minVal, maxVal, column, columnCount));
    Anchor a = new Anchor("Download");
    a.addClickHandler(e -> {
      // Larger chart
      VerticalPanel vpl = new VerticalPanel();
      Widget w = chartFor(dt, innerStyle.withBigMode(true), minVal, maxVal, column, columnCount);
      vpl.add(w);
      vpl.add(downloadLink);
      Utils.displayInPopup("Large chart", vpl, DialogPosition.Center);
      Analytics.trackEvent(Analytics.CATEGORY_VISUALIZATION, Analytics.ACTION_MAGNIFY_CHART);
    });
    vp.add(a);

    g.setWidget(row * 2 + 2, column, vp);
  }

  abstract protected Widget chartFor(final D dt, ChartStyle style,
                                     double minVal, double maxVal,
                                     int column, int columnCount);
}