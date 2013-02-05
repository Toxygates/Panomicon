package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.KCService;
import otgviewer.client.KCServiceAsync;
import otgviewer.client.OwlimService;
import otgviewer.client.OwlimServiceAsync;
import otgviewer.client.Utils;
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
	}
	
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	private final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	
	private DataFilter filter;
	private List<Group> groups;
	public ChartGridFactory(DataFilter filter, List<Group> groups) {
		this.groups = groups;
		this.filter = filter;
	}
	
	public void makeSeriesCharts(final List<Series> series, final boolean rowsAreCompounds,
			final ChartAcceptor acceptor) {
		
		owlimService.times(filter, null, new AsyncCallback<String[]>() {
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to obtain sample times");
			}
			@Override
			public void onSuccess(String[] result) {
				finishSeriesCharts(series, result, rowsAreCompounds, acceptor);												
			}			
		});			
	}
	
	// strategy: 1. Make data source,
			// 2. Make table
			// 3. make chart grid and return
			
	private void finishSeriesCharts(List<Series> series, String[] times, boolean rowsAreCompounds,			
			ChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.SeriesSource(series, times);
		
		ChartTables ct = new ChartTables.PlainChartTable(cds.getSamples(), times, true);
		
		List<String> filters = new ArrayList<String>();
		for (Series s: series) {			
			if (rowsAreCompounds && !filters.contains(s.compound())) {
				filters.add(s.compound());
			} else if (!filters.contains(s.probe())){
				filters.add(s.probe());
			}
		}
		
		ChartGrid cg = new ChartGrid(null, ct, groups, filters, rowsAreCompounds, new String[] { "Low", "Middle", "High" }, false);
		acceptor.acceptCharts(cg);
	}
	
	public void makeRowCharts(final Screen screen, final ValueType vt, final String probe,
			final AChartAcceptor acceptor) {
		owlimService.barcodes(filter, Utils.compoundsFor(groups), null, null, new AsyncCallback<Barcode[]>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to obtain chart data.");
			}

			@Override
			public void onSuccess(final Barcode[] barcodes) {
				final List<String> codes = new ArrayList<String>();
				for (Barcode b: barcodes) {
					codes.add(b.getCode());
				}
				kcService.getFullData(filter, codes, new String[] { probe }, vt, true, new AsyncCallback<List<ExpressionRow>>() {

					@Override
					public void onFailure(Throwable caught) {
						Window.alert("Unable to obtain chart data.");
					}

					@Override
					public void onSuccess(final List<ExpressionRow> rows) {
						// TODO Auto-generated method stub
						finishRowCharts(screen, filter, vt, groups, barcodes, rows, acceptor);										
					}					
				});
			}
			
		});
	}
	
	// strategy: 1. Make data source,
	// 2. Make table
	// 3. make chart grid and return
	
	private void finishRowCharts(Screen screen, DataFilter filter, ValueType vt, List<Group> groups, 
			Barcode[] barcodes, List<ExpressionRow> rows, AChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.ExpressionRowSource(barcodes, rows);
		AdjustableChartGrid acg = new AdjustableChartGrid(screen, cds, groups);
		acceptor.acceptCharts(acg);
	}
}
