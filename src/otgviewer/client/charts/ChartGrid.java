package otgviewer.client.charts;

import java.util.List;

import otgviewer.client.SparqlService;
import otgviewer.client.SparqlServiceAsync;
import otgviewer.client.Utils;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import otgviewer.shared.Group;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
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
 * A grid to display time series charts for a number of probes and doses.
 * @author johan
 *
 */
public class ChartGrid extends Composite {
	
	private final SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	
	Grid g;
	
	boolean rowsAreCompounds, columnsAreTimes;
	List<String> rowFilters;
	String[] timesOrDoses;
	ChartTables table;
	Screen screen;	
	DataTable[][] tables;
	final int highlightColumn;
	final int totalWidth;
	
	public ChartGrid(Screen screen, ChartTables table, List<Group> groups, 
			final List<String> rowFilters, 
			boolean rowsAreCompounds, 
			String[] timesOrDoses, int highlightColumn,			
			boolean columnsAreTimes, int totalWidth) {
		super();
		this.rowFilters = rowFilters;
		this.rowsAreCompounds = rowsAreCompounds;
		this.timesOrDoses = timesOrDoses;
		this.table = table;
		this.columnsAreTimes = columnsAreTimes;
		this.highlightColumn = highlightColumn;
		this.totalWidth = totalWidth;
		this.screen = screen;
		
		g = new Grid(rowFilters.size() * 2 + 1, timesOrDoses.length);		
		initWidget(g);
		
		for (int r = 0; r < rowFilters.size(); ++r) {
			g.setWidget(r * 2 + 1, 0, Utils.mkEmphLabel(rowFilters.get(r)));
		}
		
		tables = new DataTable[rowFilters.size()][timesOrDoses.length];
		for (int c = 0; c < timesOrDoses.length; ++c) {
			g.setWidget(0, c, Utils.mkEmphLabel(timesOrDoses[c]));				
			for (int r = 0; r < rowFilters.size(); ++r) {
				tables[r][c] = table.makeTable(timesOrDoses[c], rowFilters.get(r), 
						columnsAreTimes, !rowsAreCompounds);
				
			}
		}
	
		if (!rowsAreCompounds) {
			owlimService.geneSyms(rowFilters.toArray(new String[0]),
				new PendingAsyncCallback<String[][]>(screen) {
					public void handleSuccess(String[][] results) {
						for (int i = 0; i < results.length; ++i) {
							g.setWidget(
									i * 2 + 1, 0,
									Utils.mkEmphLabel(SharedUtils.mkString(results[i]) + "/" + rowFilters.get(i)));										
						}
					}

				});
		}

	}
	
	/**
	 * Obtain the largest number of data columns used in any of our backing tables.
	 * @return
	 */
	public int getMaxColumnCount() {
		int max = 0;
		for (int c = 0; c < timesOrDoses.length; ++c) {						
			for (int r = 0; r < rowFilters.size(); ++r) {
				if (tables[r][c].getNumberOfColumns() > max) {
					max = tables[r][c].getNumberOfColumns();
				}				
			}
		}
		return max;
	}

	public void adjustAndDisplay(int tableColumnCount) {
		final int width = totalWidth / timesOrDoses.length; //width of each individual chart 		
		for (int c = 0; c < timesOrDoses.length; ++c) {						
			for (int r = 0; r < rowFilters.size(); ++r) {							
				displaySeriesAt(r, c, width, tableColumnCount);
			}
		}
	}
	
	/**
	 * We normalise the column count of each data table when displaying it
	 * in order to force the charts to have equally wide bars.
	 * @param row
	 * @param column
	 * @param width
	 * @param columnCount
	 */
	private void displaySeriesAt(int row, int column, int width, int columnCount) {		
		AxisOptions ao = AxisOptions.create();
		
		final DataTable dt = tables[row][column];
		
		String[] colors = new String[columnCount];
		for (int i = 1; i < dt.getNumberOfColumns(); ++i) {
			if (column == highlightColumn) {
				colors[i - 1] = "LightSkyBlue"; 
			} else {
				colors[i - 1] = dt.getProperty(0, i, "color");
			}
		}
		while (dt.getNumberOfColumns() < columnCount) {
			int idx = dt.addColumn(ColumnType.NUMBER);
			for (int j = 0; j < dt.getNumberOfRows(); ++j) {
				dt.setValue(j, idx, 0);
			}
			colors[idx - 1] = "DarkGrey";

		}

		ao.setMinValue(table.getMin());
		ao.setMaxValue(table.getMax());		
		
		Options o = Utils.createChartOptions(colors);
		final int useWidth = width <= 400 ? width : 400;
		o.setWidth(useWidth);
		o.setHeight(170);
		o.setVAxisOptions(ao);
		
		//TODO: this is a hack to distinguish between creating series charts or not
		//(if we are, columnCount is 2)
		if (columnCount > 2) {
			ChartArea ca = ChartArea.create();
			ca.setWidth(useWidth-50);
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
					Barcode b = Barcode.unpack(dt.getProperty(row, col,
							"barcode"));
					screen.displaySampleDetail(b);
				}
			});
		}
		g.setWidget(row * 2 + 2, column, c);
	}
	
}
