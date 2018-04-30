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

package otgviewer.client.charts.google;

import java.util.List;

import otgviewer.client.charts.*;
import otgviewer.client.components.DLWScreen;
import t.common.shared.sample.Sample;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.*;
import com.google.gwt.visualization.client.events.ReadyHandler;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.visualizations.corechart.*;

/**
 * A ChartGrid that uses the Google Visualization API.
 */
public class GVizChartGrid extends ChartGrid<GDTData> {

  public static final int MAX_WIDTH = 400;

  public GVizChartGrid(Factory<GDTData, GDTDataset> factory, DLWScreen screen, GDTDataset table,
      final List<String> rowFilters, final List<String> organisms, boolean rowsAreMajors,
      String[] timesOrDoses, boolean columnsAreTimes, int totalWidth) {
    super(factory, screen, table, rowFilters, organisms, rowsAreMajors, timesOrDoses,
        columnsAreTimes, totalWidth);
  }

  protected int adjustWidth(int width, boolean bigMode) {
    int useWidth = width <= MAX_WIDTH ? width : MAX_WIDTH;
    return bigMode ? useWidth * 2 : useWidth;
  }

  protected int adjustHeight(int height, boolean bigMode) {
    return bigMode ? 170 * 2 : 170;
  }

  /*
   * We normalise the column count of each data table when displaying it in order to force the
   * charts to have equally wide bars. (To the greatest extent possible)
   */
  @Override
  protected Widget chartFor(final GDTData gdt, ChartStyle style,
      double minVal, double maxVal, int column,
      int columnCount) {
    final DataTable dt = gdt.data();
    AxisOptions ao = AxisOptions.create();

    while (dt.getNumberOfColumns() < columnCount) {
      int idx = dt.addColumn(ColumnType.NUMBER);
      for (int j = 0; j < dt.getNumberOfRows(); ++j) {
        dt.setValue(j, idx, 0);
      }
    }

    ao.setMinValue(minVal != Double.NaN ? minVal : dataset.getMin());
    ao.setMaxValue(maxVal != Double.NaN ? maxVal : dataset.getMax());

    Options o = GVizCharts.createChartOptions();

    int fw = adjustWidth(style.width, style.bigMode);
    int fh = adjustHeight(170, style.bigMode);
    o.setWidth(fw);
    o.setHeight(fh);
    o.setVAxisOptions(ao);

    if (!style.isSeries) {
      ChartArea ca = ChartArea.create();
      ca.setWidth(style.bigMode ? fw * 0.7 : fw - 75);
      ca.setHeight(style.bigMode ? fh * 0.8 : fh - 75);
      o.setChartArea(ca);
    }

    final CoreChart c = new ColumnChart(dt, o);
    if (screen != null) {
      c.addSelectHandler(new SelectHandler() {
        @Override
        public void onSelect(SelectEvent event) {
          JsArray<Selection> ss = c.getSelections();
          Selection s = ss.get(0);
          int col = s.getColumn();
          int row = s.getRow();
          String bc = dt.getProperty(row, col, "barcode");
          if (bc != null) {
            Sample b = Sample.unpack(bc, screen.attributes());
            screen.displaySampleDetail(b);
          }
        }
      });
    }
    c.addReadyHandler(new ReadyHandler() {
      @Override
      public void onReady(ReadyEvent event) {
        String URI = imageURI(c.getJso());
        style.downloadLink.setHTML("<a target=_blank href=\"" + URI + "\">Download</a>");
      }
    });
    return c;
  }

  private static native String imageURI(JavaScriptObject coreChart) /*-{
    return coreChart.getImageURI();
  }-*/;
}
