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

package otgviewer.client.charts;

import java.util.List;

import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.ProbeServiceAsync;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

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
  protected Screen screen;
  // Map<String, DataTable> tables = new HashMap<String, DataTable>(); //TODO
  protected D[][] tables;
  final int totalWidth;

  /**
   * 
   * @param screen
   * @param dataset
   * @param rowFilters major parameter values or gene symbols.
   * @param rowsAreMajors are rows major parameter values? If not, they are gene symbols.
   * @param minsOrMeds
   * @param columnsAreMins
   * @param totalWidth
   */
  public ChartGrid(Factory<D, ?> factory, Screen screen, Dataset<D> dataset,
      final List<String> rowFilters, final List<String> organisms, boolean rowsAreMajors,
      String[] minsOrMeds, boolean columnsAreMins, int totalWidth) {
    super();
    this.factory = factory;

    // TODO use something like SampleClass or MultiFilter instead if possible
    this.rowFilters = rowFilters;
    // TODO defer organism handling to schema, if it's needed
    this.organisms = organisms;
    this.minsOrMeds = minsOrMeds;
    this.dataset = dataset;
    this.totalWidth = totalWidth;
    this.screen = screen;
    probeService = screen.manager().probeService();

    if (organisms.size() == 0) {
      organisms.add(""); // TODO
    }

    final int osize = organisms.size();
    final int rfsize = rowFilters.size();
    g = new Grid(rfsize * osize * 2 + 1, minsOrMeds.length);
    initWidget(g);

    DataSchema schema = screen.schema();


    tables = factory.dataArray(rfsize * osize, minsOrMeds.length);
    for (int c = 0; c < minsOrMeds.length; ++c) {
      g.setWidget(0, c, Utils.mkEmphLabel(minsOrMeds[c]));
      for (int r = 0; r < rfsize; ++r) {
        for (int o = 0; o < osize; ++o) {
          String org = organisms.get(0).equals("") ? null : organisms.get(o);
          SampleClass sc = new SampleClass();
          String probe = null;
          if (rowsAreMajors) {
            sc.put(schema.majorParameter(), rowFilters.get(r));
          } else {
            probe = rowFilters.get(r);
          }
          if (org != null) {
            sc.put("organism", org);
          }
          String colKey = columnsAreMins ? schema.minorParameter() : schema.mediumParameter();
          sc.put(colKey, minsOrMeds[c]);
          tables[r * osize + o][c] = dataset.makeData(sc, probe);
        }
      }
    }

    if (!rowsAreMajors) {
      probeService.geneSyms(rowFilters.toArray(new String[0]),
          new PendingAsyncCallback<String[][]>(screen) {
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
   * Obtain the largest number of data columns used in any of our backing tables.
   * 
   * @return
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

  void adjustAndDisplay(int tableColumnCount, double minVal, double maxVal) {
    final int width = totalWidth / minsOrMeds.length; // width of each individual chart
    final int osize = organisms.size();
    for (int c = 0; c < minsOrMeds.length; ++c) {
      for (int r = 0; r < rowFilters.size(); ++r) {
        for (int o = 0; o < osize; ++o) {
          String label = organisms.get(o) + ":" + rowFilters.get(r);
          displayAt(r * osize + o, c, width, minVal, maxVal, tableColumnCount, label);
        }
      }
    }
  }

  /**
   * We normalise the column count of each data table when displaying it in order to force the
   * charts to have equally wide bars. (To the greatest extent possible)
   * 
   * @param row
   * @param column
   * @param width
   * @param columnCount
   */
  private void displayAt(final int row, final int column, final int width, final double minVal,
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
    vp.add(chartFor(dt, width, minVal, maxVal, column, columnCount, downloadLink, false));
    Anchor a = new Anchor("Download");
    a.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // Larger chart
        VerticalPanel vp = new VerticalPanel();
        Widget w = chartFor(dt, width, minVal, maxVal, column, columnCount, downloadLink, true);
        vp.add(w);
        vp.add(downloadLink);
        Utils.displayInPopup("Large chart", vp, DialogPosition.Center);
      }
    });
    vp.add(a);

    g.setWidget(row * 2 + 2, column, vp);
  }

  abstract protected Widget chartFor(final D dt, int width, double minVal, double maxVal,
      int column, int columnCount, HTML downloadLink, boolean bigMode);
}
