package otgviewer.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.shared.ExpressionValue;
import otgviewer.shared.Series;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
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
public class SeriesChartGrid extends Composite {
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	
	List<Series> data;
	Grid g;
	List<String> rowKeys;
	DataTable table;
	
	boolean rowsAreCompounds = false; //if false, they are probes
	
	public SeriesChartGrid(List<Series> series, boolean rowsAreCompounds) {
		super();
		data = series;
		this.rowsAreCompounds = rowsAreCompounds;
		
		Set<String> rows = new HashSet<String>();
		
		for (Series s: data) {
			if (rowsAreCompounds) {
				rows.add(s.compound());
			} else {
				rows.add(s.probe());
			}
		}
		this.rowKeys = new ArrayList<String>(rows);
		
		g = new Grid(rows.size() * 2 + 1, 3);		
		initWidget(g);
		
		g.setWidth("470px");
		g.setHeight(rowKeys.size() * 170 + "px");
		
		VisualizationUtils
		.loadVisualizationApi("1.1", new Runnable() {
			public void run() {
				drawCharts();
			}
		}, "corechart");
	}
	
	private void drawCharts() {
		for (Series s: data) {
			int row = rowsAreCompounds ? SharedUtils.indexOf(rowKeys, s.compound()) : SharedUtils.indexOf(rowKeys, s.probe());
			String td = s.timeDose();
			if (td.equals("Low")) {
				displaySeriesAt(row * 2 + 2, 0, s);
			} else if (td.equals("Middle")) {
				displaySeriesAt(row * 2 + 2, 1, s);
			} else if (td.equals("High")) {
				displaySeriesAt(row * 2 + 2, 2, s);
			}
		}
		
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
										Utils.mkEmphLabel(SharedUtils
												.mkString(results[i])));										
							}
						}

						public void onFailure(Throwable caught) {

						}
					});
		}
	}
	
	private void displaySeriesAt(int row, int column, Series s) {
		Options o = Utils.createChartOptions("LightSkyBlue");
		o.setWidth(150);
		o.setHeight(150);
		
		DataTable t = DataTable.create();
		t.addColumn(ColumnType.STRING, "Time");
		t.addColumn(ColumnType.NUMBER, "Value");
		
		t.addRow();
		t.addRow();
		t.addRow();
		t.addRow();
		t.setValue(0, 0, "1");
		t.setValue(1, 0, "2");
		t.setValue(2, 0, "3");
		t.setValue(3, 0, "4");		
		int i = 0;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for (ExpressionValue v : s.values()) {
			double vv = v.getValue();
			t.setValue(i, 1, vv);
			t.setFormattedValue(i, 1, Utils.formatNumber(vv));
			i += 1;
			if (vv < min) {
				min = vv;
			}
			if (vv > max) {
				max = vv;
			}
		}
		if (min > 0) {
			min = 0;
		}
		if (max < 0) {
			max = 0;
		}
		
		AxisOptions ao = AxisOptions.create();
		ao.setMaxValue(max);
		ao.setMinValue(min);
		o.setVAxisOptions(ao);
		
		CoreChart c = new ColumnChart(t, o);				
		g.setWidget(row, column, c);
	}
	
}
