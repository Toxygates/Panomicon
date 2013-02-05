package otgviewer.client.charts;

import java.util.List;

import otgviewer.client.OwlimService;
import otgviewer.client.OwlimServiceAsync;
import otgviewer.client.Utils;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import otgviewer.shared.Group;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
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
	
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	
	Grid g;
	
	boolean rowsAreCompounds, columnsAreTimes;
	List<String> rowFilters;
	String[] timesOrDoses;
	ChartTables table;
	Screen screen;	
	
	public ChartGrid(Screen screen, ChartTables table, List<Group> groups, 
			final List<String> rowFilters, 
			boolean rowsAreCompounds, 
			String[] timesOrDoses, boolean columnsAreTimes) {
		super();
		this.rowFilters = rowFilters;
		this.rowsAreCompounds = rowsAreCompounds;
		this.timesOrDoses = timesOrDoses;
		this.table = table;
		this.columnsAreTimes = columnsAreTimes;
		this.screen = screen;
		final int width = 780 / timesOrDoses.length; //width of each individual chart 
		
		g = new Grid(rowFilters.size() * 2 + 1, timesOrDoses.length);
		initWidget(g);
		
		for (int r = 0; r < rowFilters.size(); ++r) {
			g.setWidget(r * 2 + 1, 0, Utils.mkEmphLabel(rowFilters.get(r)));
		}
		
		for (int c = 0; c < timesOrDoses.length; ++c) {
			g.setWidget(0, c, Utils.mkEmphLabel(timesOrDoses[c]));				
			for (int r = 0; r < rowFilters.size(); ++r) {									
				displaySeriesAt(r, c, width);
			}
		}
	
		if (!rowsAreCompounds) {
			owlimService.geneSyms(rowFilters.toArray(new String[0]),
				new AsyncCallback<String[][]>() {
					public void onSuccess(String[][] results) {
						for (int i = 0; i < results.length; ++i) {
							g.setWidget(
									i * 2 + 1, 0,
									Utils.mkEmphLabel(SharedUtils.mkString(results[i]) + "/" + rowFilters.get(i)));										
						}
					}

					public void onFailure(Throwable caught) {

					}
				});
		}

//		//TODO move this
//		VisualizationUtils
//		.loadVisualizationApi("1.1", new Runnable() {
//			public void run() {
//				drawCharts();
//			}
//		}, "corechart");
	}

	
	private void displaySeriesAt(int row, int column, int width) {
		
		AxisOptions ao = AxisOptions.create();
		
		String rf = rowFilters.get(row);
		String tod = timesOrDoses[column];
		final DataTable dt = table.makeTable(tod, rf, columnsAreTimes, !rowsAreCompounds);
		ao.setMinValue(table.getMin());
		ao.setMaxValue(table.getMax());
		
		Options o = Utils.createChartOptions(table.getColumnColors());
		o.setWidth(width <= 400 ? width : 400);
		o.setHeight(170);
		o.setVAxisOptions(ao);
		
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
