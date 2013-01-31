package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.Utils;
import otgviewer.shared.ExpressionValue;
import otgviewer.shared.Group;
import otgviewer.shared.Series;
import otgviewer.shared.SharedUtils;

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
	
	Grid g;
	List<String> rowKeys;
	
	public ChartGrid(ChartTables table, List<Group> groups, List<String> rowFilters, boolean rowsAreCompounds, 
			String[] timesOrDoses, boolean isTimes) {
		super();
		
		g = new Grid(rowFilters.size() * 2 + 1, 3);
		initWidget(g);
		
		for (int c = 0; c < timesOrDoses.length; ++c) {
			String tod = timesOrDoses[c];
			for (int r = 0; r < rowFilters.size(); ++r) {
				String filter = rowFilters.get(r);
				DataTable dt = table.makeTable(tod, filter, isTimes, !rowsAreCompounds);
			}
		}
		//... work in progress ends here
		
		Set<String> rows = new HashSet<String>();
		
		for (Series s: data) {
			if (rowsAreCompounds) {
				rows.add(s.compound());				
			} else {
				rows.add(s.probe());			
			}
		}
		this.rowKeys = new ArrayList<String>(rows);
		
		g.setWidth("530px");
		g.setHeight(rowKeys.size() * 190 + "px");
		
		VisualizationUtils
		.loadVisualizationApi("1.1", new Runnable() {
			public void run() {
				drawCharts();
			}
		}, "corechart");
	}
	
	private void drawCharts() {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (Series s: data) {
			for (ExpressionValue ev: s.values()) {
				if (ev.getValue() < min) {
					min = ev.getValue();
				}
				if (ev.getValue() > max) {
					max = ev.getValue();
				}
			}
		}
		
		final double fmin = min;
		final double fmax = max;
		
		int i = 1;
		for (String p: rowKeys) {			
			g.setWidget(i, 0, Utils.mkEmphLabel(p));
			i += 2;
		}
		g.setWidget(0, 0, Utils.mkEmphLabel("Low"));
		g.setWidget(0, 1, Utils.mkEmphLabel("Middle"));
		g.setWidget(0, 2, Utils.mkEmphLabel("High"));

		if (!rowsAreCompounds) {
			owlimService.geneSyms(rowKeys.toArray(new String[0]),
					new AsyncCallback<String[][]>() {
						public void onSuccess(String[][] results) {
							for (int i = 0; i < results.length; ++i) {
								g.setWidget(
										i * 2 + 1, 0,
										Utils.mkEmphLabel(SharedUtils.mkString(results[i]) + "/" + rowKeys.get(i)));										
							}
						}

						public void onFailure(Throwable caught) {

						}
					});
		}
		
		
	}
	
	private void displaySeriesAt(String[] times, int row, int column, Series s, double minVal, double maxVal) {
		Options o = Utils.createChartOptions("LightSkyBlue");
		o.setWidth(170);
		o.setHeight(170);
		AxisOptions ao = AxisOptions.create();
		ao.setMinValue(minVal);
		ao.setMaxValue(maxVal);
		o.setVAxisOptions(ao);
		
		DataTable t = DataTable.create();
		t.addColumn(ColumnType.STRING, "Time");
		t.addColumn(ColumnType.NUMBER, "Value");
		
		for (int i = 0; i < times.length; ++i) {
			t.addRow();
			t.setValue(i, 0, times[i]);			
		}		
				
		int i = 0;

		for (ExpressionValue v : s.values()) {
			double vv = v.getValue();
			t.setValue(i, 1, vv);
			t.setFormattedValue(i, 1, Utils.formatNumber(vv));
			i += 1;
		}

		CoreChart c = new ColumnChart(t, o);				
		g.setWidget(row, column, c);
	}
	
}
