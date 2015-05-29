/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otgviewer.client.charts.google;

import java.util.List;

import otgviewer.client.charts.ChartDataset;
import otgviewer.client.charts.ChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.shared.OTGSample;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.ChartArea;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

/**
 * A ChartGrid that uses the Google Visualization API.
 */
public class GVizChartGrid extends ChartGrid {
	
	public static final int MAX_WIDTH = 400;
	
	public GVizChartGrid(Screen screen, ChartDataset table,  
			final List<String> rowFilters, final List<String> organisms,
			boolean rowsAreMajors, 
			String[] timesOrDoses, 			
			boolean columnsAreTimes, int totalWidth) {
		super(screen, table, rowFilters, organisms, rowsAreMajors, timesOrDoses,
				columnsAreTimes, totalWidth);
	}
	
	/**
	 * We normalise the column count of each data table when displaying it
	 * in order to force the charts to have equally wide bars.
	 * (To the greatest extent possible)
	 * @param row
	 * @param column
	 * @param width
	 * @param columnCount
	 */
	@Override
	protected Widget chartFor(final DataTable dt, int width, double minVal, double maxVal, 
			int column, int columnCount) {		
		AxisOptions ao = AxisOptions.create();
		
		while (dt.getNumberOfColumns() < columnCount) {
			int idx = dt.addColumn(ColumnType.NUMBER);
			for (int j = 0; j < dt.getNumberOfRows(); ++j) {
				dt.setValue(j, idx, 0);
			}
		}

		ao.setMinValue(minVal != Double.NaN ? minVal : table.getMin());
		ao.setMaxValue(maxVal != Double.NaN ? maxVal : table.getMax());		
		
		Options o = GVizCharts.createChartOptions();
		final int useWidth = width <= MAX_WIDTH ? width : MAX_WIDTH;
		o.setWidth(useWidth);
		o.setHeight(170);
		o.setVAxisOptions(ao);
		
		//TODO: this is a hack to distinguish between creating series charts or not
		//(if we are, columnCount is 2)
		if (columnCount > 2) {
			ChartArea ca = ChartArea.create();
			ca.setWidth(useWidth - 75);
			ca.setHeight(140);		
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
						OTGSample b = OTGSample.unpack(bc);
						screen.displaySampleDetail(b);
					}
				}
			});
		}
		return c;
	}
}
