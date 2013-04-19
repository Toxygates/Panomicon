package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.KCService;
import otgviewer.client.KCServiceAsync;
import otgviewer.client.SparqlService;
import otgviewer.client.SparqlServiceAsync;
import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.components.Screen;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.Series;
import otgviewer.shared.ValueType;

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
	
	private static final SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	private static final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	
	private DataFilter filter;
	private List<Group> groups;
	public ChartGridFactory(DataFilter filter, List<Group> groups) {
		this.groups = groups;
		this.filter = filter;
	}
	
	public void makeSeriesCharts(final List<Series> series, final boolean rowsAreCompounds,
			final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {
		
		owlimService.times(filter, null, new AsyncCallback<String[]>() {
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
	
	// strategy: 1. Make data source,
			// 2. Make table
			// 3. make chart grid and return
			
	private void finishSeriesCharts(final List<Series> series, final String[] times, 
			final boolean rowsAreCompounds,			
			final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {
		ChartDataSource cds = new ChartDataSource.SeriesSource(series, times);
		
		cds.getSamples(null, null, new ChartDataSource.SampleAcceptor() {

			@Override
			public void accept(final List<ChartSample> samples) {
				ChartTables ct = new ChartTables.PlainChartTable(samples, samples, times, true);
				
				List<String> filters = new ArrayList<String>();
				for (Series s: series) {			
					if (rowsAreCompounds && !filters.contains(s.compound())) {
						filters.add(s.compound());
					} else if (!filters.contains(s.probe())){
						filters.add(s.probe());
					}
				}
				
				ChartGrid cg = new ChartGrid(screen, ct, groups, filters, rowsAreCompounds, new String[] { "Low", "Middle", "High" }, 
						highlightDose, false, 400);
				cg.adjustAndDisplay(cg.getMaxColumnCount());
				acceptor.acceptCharts(cg);				
			}
			
		});
	}
	
	public void makeRowCharts(final Screen screen, final Barcode[] barcodes, final ValueType vt, final String probe,
			final AChartAcceptor acceptor) {
		if (barcodes == null) {
			owlimService.barcodes(filter, Utils.compoundsFor(groups), null, null, new AsyncCallback<Barcode[]>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void onSuccess(final Barcode[] barcodes) {
					finishRowCharts(screen, filter, probe, vt, groups, barcodes, acceptor);
					acceptor.acceptBarcodes(barcodes);
				}			
			});
		} else {
			finishRowCharts(screen, filter, probe, vt, groups, barcodes, acceptor);
		}
	}
	
	private void finishRowCharts(Screen screen, DataFilter filter, ValueType vt, List<Group> groups, 
			Barcode[] barcodes, List<ExpressionRow> rows, AChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.ExpressionRowSource(barcodes, rows);
		AdjustableChartGrid acg = new AdjustableChartGrid(screen, cds, groups);
		acceptor.acceptCharts(acg);
	}
	
	private void finishRowCharts(Screen screen, DataFilter filter, String probe, ValueType vt, List<Group> groups, 
			Barcode[] barcodes, AChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.DynamicExpressionRowSource(filter, probe, vt, barcodes, screen);
		AdjustableChartGrid acg = new AdjustableChartGrid(screen, cds, groups);
		acceptor.acceptCharts(acg);
	}
}
