package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import otgviewer.client.Utils;
import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;
import otgviewer.shared.GroupUtils;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Series;
import otgviewer.shared.ValueType;
import t.common.client.rpc.SeriesService;
import t.common.client.rpc.SeriesServiceAsync;
import t.common.client.rpc.SparqlService;
import t.common.client.rpc.SparqlServiceAsync;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.Unit;

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
	
	private final Logger logger = Utils.getLogger("cgf");
	
	private static final SparqlServiceAsync sparqlService = 
			(SparqlServiceAsync) GWT.create(SparqlService.class);
	private static final SeriesServiceAsync seriesService = 
			(SeriesServiceAsync) GWT.create(SeriesService.class);
	
	
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
		seriesService.expectedTimes(series.get(0), new AsyncCallback<String[]>() {
			@Override
			public void onFailure(Throwable caught) {
				logger.log(Level.WARNING, "Unable to obtain sample times.", caught);
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
		final String majorParam = schema.majorParameter();
		final String[] medVals = schema.sortedValuesForDisplay(null, 
				schema.mediumParameter());
		schema.sort(schema.timeParameter(), times);
		ChartDataSource cds = new ChartDataSource.SeriesSource(
				schema, series, times);
		
		cds.getSamples(null, null, null, new TimeDoseColorPolicy(medVals[highlightMed], "SkyBlue"), 
				new ChartDataSource.SampleAcceptor() {

			@Override
			public void accept(final List<ChartSample> samples) {
				ChartDataset ct = new ChartDataset(samples, samples, times, true);
				List<String> filters = new ArrayList<String>();
				for (Series s: series) {			
					if (rowsAreCompounds && !filters.contains(s.get(majorParam))) {
						filters.add(s.get(majorParam));
					} else if (!filters.contains(s.probe())){
						filters.add(s.probe());
					}
				}
				
				List<String> organisms = 
						new ArrayList<String>(
								SampleClass.collect(Arrays.asList(sampleClasses), "organism")
						);

				ChartGrid cg = new GVizChartGrid(screen, ct, filters, organisms, 
						rowsAreCompounds, medVals, false, 400);
				cg.adjustAndDisplay(cg.getMaxColumnCount(), ct.getMin(), ct.getMax());
				acceptor.acceptCharts(cg);				
			}

		});
		} catch (Exception e) {
			Window.alert("Unable to display charts: " + e.getMessage());
			logger.log(Level.WARNING, "Unable to display charts.", e);
		}
	}
	
	public void makeRowCharts(final Screen screen, final OTGSample[] barcodes, 
			final ValueType vt, final String[] probes,
			final AChartAcceptor acceptor) {
		Set<String> organisms = Group.collectAll(groups, "organism");
		
		String[] majorVals = 
				GroupUtils.collect(groups, schema.majorParameter()).toArray(new String[0]); 
		
		if (organisms.size() > 1) {
			sparqlService.units(sampleClasses, schema.majorParameter(),
			  majorVals, new AsyncCallback<Pair<Unit,Unit>[]>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");	
					logger.log(Level.WARNING, "Unable to obtain chart data.", caught);
				}

				@Override
				public void onSuccess(Pair<Unit, Unit>[] result) {
					finishRowCharts(screen, probes, vt, groups, result, acceptor);							
				}						
			});
		} else if (barcodes == null) {
			sparqlService.samples(sampleClasses, schema.majorParameter(), 
					majorVals,
					new AsyncCallback<OTGSample[]>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
					logger.log(Level.WARNING, "Unable to obtain chart data.", caught);
				}

				@Override
				public void onSuccess(final OTGSample[] barcodes) {
					finishRowCharts(screen, probes, vt, groups, barcodes, acceptor);
					//TODO is this needed/well designed?
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
	
	private void finishRowCharts(Screen screen, String[] probes, ValueType vt, List<Group> groups, 
			Pair<Unit, Unit>[] units, AChartAcceptor acceptor) {
		Set<Unit> treated = new HashSet<Unit>();
		for (Pair<Unit, Unit> u: units) {
			treated.add(u.first());			
		}
		
		ChartDataSource cds = new ChartDataSource.DynamicUnitSource(schema, 
				probes, vt, treated.toArray(new Unit[0]), screen);
		AdjustableChartGrid acg = new AdjustableChartGrid(screen, cds, groups, vt);
		acceptor.acceptCharts(acg);
	}
}
