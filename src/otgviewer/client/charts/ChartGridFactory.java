package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.client.rpc.SparqlService;
import otgviewer.client.rpc.SparqlServiceAsync;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.OTGUtils;
import otgviewer.shared.Series;
import otgviewer.shared.ValueType;
import t.viewer.shared.SampleClass;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ChartGridFactory {
	
	public static interface ChartAcceptor {
		void acceptCharts(ChartGrid cg);
	}
	
	public static interface AChartAcceptor {
		void acceptCharts(AdjustableChartGrid cg);
		void acceptBarcodes(Barcode[] barcodes);
	}
	
	private static final SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	
	private SampleClass sampleClass;
	private List<Group> groups;
	public ChartGridFactory(SampleClass sc, List<Group> groups) {
		this.groups = groups;
		this.sampleClass = sc;
	}
	
	public void makeSeriesCharts(final List<Series> series, final boolean rowsAreCompounds,
			final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {
		
		sparqlService.times(sampleClass, new AsyncCallback<String[]>() {
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to obtain sample times");
			}
			@Override
			public void onSuccess(String[] result) {
				finishSeriesCharts(series, result, rowsAreCompounds, highlightDose, acceptor, screen);												
			}			
		});			
	}

	private void finishSeriesCharts(final List<Series> series, final String[] times, 
			final boolean rowsAreCompounds,			
			final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {
		ChartDataSource cds = new ChartDataSource.SeriesSource(series, times);
		final String[] doses = new String[] { "Low", "Middle", "High" };
		
		cds.getSamples(null, null, new TimeDoseColorPolicy(doses[highlightDose], "SkyBlue"), 
				new ChartDataSource.SampleAcceptor() {

			@Override
			public void accept(final List<ChartSample> samples) {
				ChartDataset ct = new ChartDataset(samples, samples, times, true);
				
				List<String> filters = new ArrayList<String>();
				for (Series s: series) {			
					if (rowsAreCompounds && !filters.contains(s.compound())) {
						filters.add(s.compound());
					} else if (!filters.contains(s.probe())){
						filters.add(s.probe());
					}
				}
				
				ChartGrid cg = new GVizChartGrid(screen, ct, groups, filters, rowsAreCompounds, 
						doses, false, 400);
				cg.adjustAndDisplay(cg.getMaxColumnCount(), ct.getMin(), ct.getMax());
				acceptor.acceptCharts(cg);				
			}
			
		});
	}
	
	public void makeRowCharts(final Screen screen, final Barcode[] barcodes, final ValueType vt, final String probe,
			final AChartAcceptor acceptor) {
		if (barcodes == null) {
			sparqlService.samples(sampleClass, OTGUtils.compoundsFor(groups), new AsyncCallback<Barcode[]>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void onSuccess(final Barcode[] barcodes) {
					finishRowCharts(screen, probe, vt, groups, barcodes, acceptor);
					acceptor.acceptBarcodes(barcodes);
				}			
			});
		} else {
			finishRowCharts(screen, probe, vt, groups, barcodes, acceptor);
		}
	}
	
	private void finishRowCharts(Screen screen, String probe, ValueType vt, List<Group> groups, 
			Barcode[] barcodes, AChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.DynamicExpressionRowSource(sampleClass, probe, vt, barcodes, screen);
		AdjustableChartGrid acg = new AdjustableChartGrid(screen, cds, groups, vt);
		acceptor.acceptCharts(acg);
	}
}
