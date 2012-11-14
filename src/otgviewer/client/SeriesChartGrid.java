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
	List<String> probes;
	DataTable table;
	public SeriesChartGrid(List<Series> series) {
		super();
		
		VisualizationUtils
		.loadVisualizationApi("1.1", new Runnable() {
			public void run() {
				drawCharts();
			}
		}, "corechart");
		
		data = series;
		Set<String> probes = new HashSet<String>();
		
		for (Series s: data) {
			probes.add(s.probe());
		}
		this.probes = new ArrayList<String>(probes);
		
		g = new Grid(probes.size() + 1, 4);
		initWidget(g);
	}
	
	private void drawCharts() {
		for (Series s: data) {
			int row = SharedUtils.indexOf(probes, s.probe());
			String td = s.timeDose();
			if (td.equals("Low")) {
				displaySeriesAt(row + 1, 1, s);
			} else if (td.equals("Middle")) {
				displaySeriesAt(row + 1, 2, s);
			} else if (td.equals("High")) {
				displaySeriesAt(row + 1, 3, s);
			}
		}
		
		int i = 1;
		for (String p: probes) {
			g.setWidget(i, 0, new Label(p));
			i += 1;
		}
		g.setWidget(0, 1, new Label("Low"));
		g.setWidget(0, 2, new Label("Middle"));
		g.setWidget(0, 3, new Label("High"));

		owlimService.geneSyms(probes.toArray(new String[0]),
			new AsyncCallback<String[][]>() {
				public void onSuccess(String[][] results) {
					for (int i = 0; i < results.length; ++i) {
						g.setWidget(i + 1, 0, new Label(SharedUtils.mkString(results[i])));
					}
				}

				public void onFailure(Throwable caught) {

				}
			});

	}
	
	private void displaySeriesAt(int row, int column, Series s) {
		Options o = Utils.createChartOptions();
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
		for (ExpressionValue v : s.values()) {
			t.setValue(i, 1, v.getValue());
			i += 1;
		}
		
		CoreChart c = new ColumnChart(t, o);
		c.setPixelSize(150, 150);
		g.setWidget(row, column, c);
	}
	
}
