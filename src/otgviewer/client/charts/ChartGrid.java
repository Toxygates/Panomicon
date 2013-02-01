package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.OwlimService;
import otgviewer.client.OwlimServiceAsync;
import otgviewer.client.Utils;
import otgviewer.shared.ExpressionValue;
import otgviewer.shared.Group;
import otgviewer.shared.Series;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
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
	
	
	public ChartGrid(ChartTables table, List<Group> groups, final List<String> rowFilters, 
			boolean rowsAreCompounds, 
			String[] timesOrDoses, boolean columnsAreTimes) {
		super();
		this.rowFilters = rowFilters;
		this.rowsAreCompounds = rowsAreCompounds;
		this.timesOrDoses = timesOrDoses;
		this.table = table;
		this.columnsAreTimes = columnsAreTimes;
		
		g = new Grid(rowFilters.size() * 2 + 1, timesOrDoses.length);
		initWidget(g);
		
		for (int c = 0; c < timesOrDoses.length; ++c) {
			String tod = timesOrDoses[c];
			g.setWidget(0, c, Utils.mkEmphLabel(timesOrDoses[c]));				
			for (int r = 0; r < rowFilters.size(); ++r) {
				String filter = rowFilters.get(r);								
				g.setWidget(r * 2 + 1, 0, Utils.mkEmphLabel(rowFilters.get(r)));
				displaySeriesAt(r, c);
			}
		}
	
		g.setWidth("530px");
		g.setHeight(rowFilters.size() * 190 + "px");
		
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

	
	private void displaySeriesAt(int row, int column) {
		Options o = Utils.createChartOptions("LightSkyBlue");
		o.setWidth(170);
		o.setHeight(170);
		AxisOptions ao = AxisOptions.create();
		
		String rf = rowFilters.get(row);
		String tod = timesOrDoses[column];
		DataTable dt = table.makeTable(tod, rf, columnsAreTimes, !rowsAreCompounds);
		ao.setMinValue(table.getMin());
		ao.setMaxValue(table.getMax());
		o.setVAxisOptions(ao);
		
		CoreChart c = new ColumnChart(dt, o);				
		g.setWidget(row * 2 + 2, column, c);
	}
	
}
