package otgviewer.client.charts.google;

import java.util.List;

import otgviewer.client.charts.ChartDataset;
import otgviewer.client.charts.ChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;
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
			final List<String> rowFilters, 
			boolean rowsAreCompounds, 
			String[] timesOrDoses, 			
			boolean columnsAreTimes, int totalWidth) {
		super(screen, table, rowFilters, rowsAreCompounds, timesOrDoses,
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
