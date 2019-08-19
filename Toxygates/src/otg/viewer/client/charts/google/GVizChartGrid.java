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

package otg.viewer.client.charts.google;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.*;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.ReadyHandler;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.visualizations.corechart.*;

import otg.viewer.client.charts.*;
import otg.viewer.client.components.OTGScreen;
import otg.viewer.client.components.ScreenUtils;
import t.common.shared.sample.Sample;
import t.viewer.client.storage.Packer.UnpackInputException;

/**
 * A ChartGrid that uses the Google Visualization API.
 */
public class GVizChartGrid extends ChartGrid<GDTData> {

  public static final int MAX_WIDTH = 400;

  public GVizChartGrid(OTGScreen screen, GDTDataset table,
      List<String> rowFilters, List<String> rowLabels, List<String> organisms,
      boolean rowsAreMajors,
      String[] timesOrDoses, boolean columnsAreTimes, int totalWidth) {
    super(screen, table, rowFilters, rowLabels, organisms, rowsAreMajors, timesOrDoses,
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
  protected Widget chartFor(final GDTData gdata, ChartStyle style, double minVal, double maxVal,
      int column, int columnCount, String chartTitle) {
    final DataTable dataTable = gdata.data();
    AxisOptions axisOptions = AxisOptions.create();

    while (dataTable.getNumberOfColumns() < columnCount) {
      int newColumnIndex = dataTable.addColumn(ColumnType.NUMBER);
      for (int row = 0; row < dataTable.getNumberOfRows(); ++row) {
        dataTable.setValue(row, newColumnIndex, 0);
      }
    }

    axisOptions.setMinValue(minVal != Double.NaN ? minVal : dataset.getMin());
    axisOptions.setMaxValue(maxVal != Double.NaN ? maxVal : dataset.getMax());

    Options chartOptions = GVizCharts.createChartOptions();

    int width = adjustWidth(style.width, style.bigMode);
    int height = adjustHeight(170, style.bigMode);
    chartOptions.setWidth(width);
    chartOptions.setHeight(height);
    chartOptions.setVAxisOptions(axisOptions);

    if (!style.isSeries) {
      ChartArea chartArea = ChartArea.create();
      chartArea.setWidth(style.bigMode ? width * 0.7 : width - 75);
      chartArea.setHeight(style.bigMode ? height * 0.8 : height - 75);
      chartOptions.setChartArea(chartArea);
    }

    final CoreChart coreChart = new ColumnChart(dataTable, chartOptions);
    if (screen != null) {
      coreChart.addSelectHandler(new SelectHandler() {
        @Override
        public void onSelect(SelectEvent event) {
          JsArray<Selection> allSelections = coreChart.getSelections();
          Selection selection = allSelections.get(0);
          int col = selection.getColumn();
          int row = selection.getRow();
          String sampleId = dataTable.getProperty(row, col, GDTDataset.SAMPLE_ID_PROP);
          if (sampleId != null) {
            try {
              Sample sample = screen.getStorage().samplePacker.unpack(sampleId);
              ScreenUtils.displaySampleDetail(screen, sample);
            } catch (UnpackInputException e) {
              Window.alert("Error unpacking sample: " + e.getMessage());
            }
          }
        }
      });
    }
    coreChart.addReadyHandler(new ReadyHandler() {
      @Override
      public void onReady(ReadyEvent event) {
        String URI = imageURI(coreChart.getJso());
        style.downloadLink.setHTML("<a target=_blank href=\"" + URI + "\" download = \"" 
            + "chart_" + chartTitle + "\">Download</a>");
      }
    });
    return coreChart;
  }

  private static native String imageURI(JavaScriptObject coreChart) /*-{
		return coreChart.getImageURI();
  }-*/;
}
