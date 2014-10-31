package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.client.rpc.SparqlService;
import otgviewer.client.rpc.SparqlServiceAsync;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import otgviewer.shared.GroupUtils;
import otgviewer.shared.Series;
import otgviewer.shared.TimesDoses;
import otgviewer.shared.ValueType;
import t.common.shared.DataSchema;
import t.common.shared.SampleClass;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ChartGridFactory {
	
	public static interface ChartAcceptor {
		void acceptCharts(ChartGrid cg);
	}
	
	public static interface AChartAcceptor {
		void acceptCharts(AdjustableChartGrid cg);
		void acceptBarcodes(OTGSample[] barcodes);
	}
	
	private static final SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	
	private SampleClass[] sampleClasses;
	private List<Group> groups;
	final private DataSchema schema;
	
	public ChartGridFactory(DataSchema schema, List<Group> groups) {
		this.groups = groups;

		List<SampleClass> scs = new ArrayList<SampleClass>();
		for (Group g: groups) {
			SampleClass sc = g.getSamples()[0].
					sampleClass().asMacroClass(schema);
			scs.add(sc);
		}		

		this.sampleClasses = scs.toArray(new SampleClass[0]);
		this.schema = schema;		
	}
	
	public ChartGridFactory(DataSchema schema, SampleClass[] sampleClasses) {
		this.sampleClasses = sampleClasses;
		this.schema = schema;
		groups = new ArrayList<Group>();
	}
	
	public void makeSeriesCharts(final List<Series> series, final boolean rowsAreCompounds,
			final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {		
		sparqlService.parameterValues(sampleClasses, 
				schema.timeParameter(), new AsyncCallback<String[]>() {
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
			final int highlightMed, final ChartAcceptor acceptor, final Screen screen) {		
		//TODO get from schema or data
		try {
		final String[] medVals = schema.sortedValuesForDisplay(null, 
				schema.mediumParameter());
		schema.sort(schema.timeParameter(), times);
		ChartDataSource cds = new ChartDataSource.SeriesSource(
				schema, series, times);
		
		cds.getSamples(null, null, new TimeDoseColorPolicy(medVals[highlightMed], "SkyBlue"), 
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

				ChartGrid cg = new GVizChartGrid(screen, ct, filters, rowsAreCompounds, 
						medVals, false, 400);
				cg.adjustAndDisplay(cg.getMaxColumnCount(), ct.getMin(), ct.getMax());
				acceptor.acceptCharts(cg);				
			}

		});
		} catch (Exception e) {
			Window.alert("Unable to display charts: " + e.getMessage());
		}
	}
	
	public void makeRowCharts(final Screen screen, final OTGSample[] barcodes, 
			final ValueType vt, final String[] probes,
			final AChartAcceptor acceptor) {
		if (barcodes == null) {
			//TODO
			sparqlService.samples(sampleClasses, schema.majorParameter(), 
					GroupUtils.collect(groups, schema.majorParameter()).toArray(new String[0]),
					new AsyncCallback<OTGSample[]>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void onSuccess(final OTGSample[] barcodes) {
					finishRowCharts(screen, probes, vt, groups, barcodes, acceptor);
					acceptor.acceptBarcodes(barcodes);
				}			
			});
		} else {
			finishRowCharts(screen, probes, vt, groups, barcodes, acceptor);
		}
	}
	
	private void finishRowCharts(Screen screen, String[] probes, ValueType vt, List<Group> groups, 
			OTGSample[] barcodes, AChartAcceptor acceptor) {
		ChartDataSource cds = new ChartDataSource.DynamicExpressionRowSource(schema, 
				probes, vt, barcodes, screen);
		AdjustableChartGrid acg = new AdjustableChartGrid(screen, cds, groups, vt);
		acceptor.acceptCharts(acg);
	}
}
