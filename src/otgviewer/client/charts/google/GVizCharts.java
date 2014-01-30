package otgviewer.client.charts.google;

import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.CoreChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

public class GVizCharts {
	public static void loadAPIandThen(final Runnable r) {
		VisualizationUtils.loadVisualizationApi(r, CoreChart.PACKAGE);
	}
	
	public static Options createChartOptions() {
		Options o = Options.create();
		o.set("legend.position", "none");
		o.setLegend(LegendPosition.NONE);
		return o;
	}
}
