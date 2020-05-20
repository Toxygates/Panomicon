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

package t.viewer.client.charts;

import java.util.List;

import com.google.gwt.user.client.ui.*;

import t.model.sample.OTGAttribute;
import t.viewer.client.charts.google.GVizChartGrid;
import t.viewer.client.screen.Screen;
import t.common.shared.DataSchema;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.viewer.client.Analytics;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;

/**
 * A grid to display time (or dose) series charts for a number of probes and doses (or times).
 */
abstract public class ChartGrid<D extends Data> extends Composite {

  Grid grid;

  List<String> rowLabels;
  List<String> rowFilters;
  List<String> organisms;
  String[] minsOrMeds;
  protected Dataset<D> dataset;
  protected Screen screen;
  protected D[][] tables;
  final int totalWidth;
  final static String NO_ORGANISM = "";

  /**
   * @param rowFilters major parameter values or gene symbols, depending on the chart type.
   * @param rowsAreMajors are rows major parameter values? If not, they are gene symbols.
   * @param rowLabels labels to be displayed at each row in the chart grid. 
   */

  public ChartGrid(Screen screen, Dataset<D> dataset,
                   final List<String> rowFilters, final List<String> rowLabels,
                   final List<String> organisms, boolean rowsAreMajors,
                   String[] minsOrMeds, boolean columnsAreMins, int totalWidth) {
    super();
    this.organisms = organisms;
    this.rowFilters = rowFilters;
    this.rowLabels = rowLabels;
    this.minsOrMeds = minsOrMeds;
    this.dataset = dataset;
    this.totalWidth = totalWidth;
    this.screen = screen;

    if (organisms.size() == 0) {
      organisms.add(NO_ORGANISM);
    }

    final int osize = organisms.size();
    final int rfsize = rowFilters.size();
    grid = new Grid(rfsize * osize * 2 + 1, minsOrMeds.length);
    initWidget(grid);

    DataSchema schema = screen.manager().schema();

    tables = dataset.makeDataArray(rfsize * osize, minsOrMeds.length);

    for (int col = 0; col < minsOrMeds.length; ++col) {
      grid.setWidget(0, col, Utils.mkEmphLabel(minsOrMeds[col]));
      for (int row = 0; row < rfsize; ++row) {
        for (int o = 0; o < osize; ++o) {
          String org = organisms.get(o).equals(NO_ORGANISM) ? null : organisms.get(o);
          SampleClass sc = new SampleClass();
          String probe = null;
          if (rowsAreMajors) {
            sc.put(schema.majorParameter(), rowFilters.get(row));
          } else {
            probe = rowFilters.get(row);
          }
          if (org != null) {
            sc.put(OTGAttribute.Organism, org);
          }
          Attribute colKey = columnsAreMins ? schema.minorParameter() : schema.mediumParameter();
          sc.put(colKey, minsOrMeds[col]);
          tables[row * osize + o][col] = dataset.makeData(sc, probe);
        }
      }
    }

    if (!rowsAreMajors) {
      for (int i = 0; i < rowLabels.size(); ++i) {
        grid.setWidget(i * 2 + 1, 0, Utils.mkEmphLabel(rowLabels.get(i)));
      }
    }
  }

  public int computedTotalWidth() {
    int theoretical = grid.getColumnCount() * GVizChartGrid.MAX_WIDTH;
    if (theoretical > totalWidth) {
      return totalWidth;
    } else {
      return theoretical;
    }
  }

  /**
   * Obtain the largest number of data columns used in any of our backing tables.
   */
  public int getMaxColumnCount() {
    int max = 0;
    for (int row = 0; row < tables.length; ++row) {
      for (int col = 0; col < tables[0].length; ++col) {
        if (tables[row][col].numberOfColumns() > max) {
          max = tables[row][col].numberOfColumns();
        }
      }
    }
    return max;
  }

  void adjustAndDisplay(ChartStyle style, int tableColumnCount, double minVal, double maxVal,
      String gridTitle) {
    int width = totalWidth / minsOrMeds.length; // width of each individual chart
    int osize = organisms.size();
    ChartStyle innerStyle = style.withWidth(width);
    for (int col = 0; col < minsOrMeds.length; ++col) {
      for (int row = 0; row < rowFilters.size(); ++row) {
        for (int org = 0; org < osize; ++org) {
          String fallbackLabel = organisms.get(org) + ":" + rowFilters.get(row);
          displayAt(innerStyle, row * osize + org, col, minVal, maxVal, tableColumnCount, fallbackLabel, gridTitle);
        }
      }
    }
  }

  /**
   * We normalise the column count of each data table when displaying it in order to force the
   * charts to have equally wide bars. (To the greatest extent possible)
   */
  private void displayAt(final ChartStyle style, final int row, final int column,
      final double minVal, final double maxVal, final int columnCount, String fallbackLabel,
      String gridTitle) {
    final D dataTable = tables[row][column];

    if (dataTable.numberOfColumns() == 1) {
      return; // no data columns -> no data to show
    }
    if (grid.getWidget(row * 2 + 1, 0) == null) {
      // add the label if none has been set so far
      // (currently used in the case of orthologous charts only)
      grid.setWidget(row * 2 + 1, 0, Utils.mkEmphLabel(fallbackLabel));
    }
    final HTML downloadLink = new HTML();
    VerticalPanel vp = new VerticalPanel();
    final ChartStyle innerStyle = style.withDownloadLink(downloadLink);

    String chartTitle = gridTitle + "_" + rowLabels.get(row) + "_" + minsOrMeds[column];
    vp.add(chartFor(dataTable, innerStyle.withBigMode(false), minVal, maxVal, column, columnCount, chartTitle));    
    
    Anchor a = new Anchor("Download");
    a.addClickHandler(e -> {
      // Larger chart
      VerticalPanel vpl = new VerticalPanel();
      Widget w =
          chartFor(dataTable, innerStyle.withBigMode(true), minVal, maxVal, column, columnCount, chartTitle);
      vpl.add(w);
      vpl.add(downloadLink);
      Utils.displayInPopup("Large chart", vpl, DialogPosition.Center);
      Analytics.trackEvent(Analytics.CATEGORY_VISUALIZATION, Analytics.ACTION_MAGNIFY_CHART);
    });
    vp.add(a);

    grid.setWidget(row * 2 + 2, column, vp);
  }

  abstract protected Widget chartFor(final D dataTable, ChartStyle style, double minVal,
      double maxVal, int column, int columnCount, String chartTitle);
}
