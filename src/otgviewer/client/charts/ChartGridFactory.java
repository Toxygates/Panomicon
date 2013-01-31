package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.OwlimService;
import otgviewer.client.OwlimServiceAsync;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.Series;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ChartGridFactory {
	
	public static interface ChartAcceptor {
		void acceptCharts(ChartGrid cg);
	}
	
	private final OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);
	
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
	
	private void finishSeriesCharts(List<Series> series, String[] times, boolean rowsAreCompounds, ChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.SeriesSource(series, times);
		// strategy: 1. Make data source,
		// 2. Make table
		// 3. make chart grid and return
		
		ChartTables ct = new ChartTables.GroupedChartTable(cds.getSamples(), groups);
		
		List<String> filters = new ArrayList<String>();
		for (Series s: series) {			
			if (rowsAreCompounds && !filters.contains(s.compound())) {
				filters.add(s.compound());
			} else if (!filters.contains(s.probe())){
				filters.add(s.probe());
			}
		}
		
		ChartGrid cg = new ChartGrid(ct, groups, filters, rowsAreCompounds);
		acceptor.acceptCharts(cg);
	}
}
