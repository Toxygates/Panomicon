/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.client.charts.ColorPolicy.TimeDoseColorPolicy;
import otgviewer.client.charts.google.GVizChartGrid;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.Group;
import otgviewer.shared.GroupUtils;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Series;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.SampleMultiFilter;
import t.common.shared.SharedUtils;
import t.common.shared.ValueType;
import t.viewer.client.rpc.SeriesServiceAsync;
import t.viewer.client.rpc.SparqlServiceAsync;
import t.viewer.shared.Unit;

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
	
	private final Logger logger = SharedUtils.getLogger("cgf");
	
	protected final SparqlServiceAsync sparqlService;  			
	protected final SeriesServiceAsync seriesService; 
	
	
	private SampleClass[] sampleClasses;
	private List<Group> groups;
	final private DataSchema schema;
	
	private ChartGridFactory(Screen screen) {
		this.schema = screen.schema();
		this.sparqlService = screen.sparqlService();
		this.seriesService = screen.seriesService();
	}
	
	public ChartGridFactory(Screen screen, List<Group> groups) {
		this(screen);
		this.groups = groups;
		
		List<SampleClass> scs = new ArrayList<SampleClass>();
		for (Group g: groups) {
			SampleClass sc = g.getSamples()[0].
					sampleClass().asMacroClass(schema);
			scs.add(sc);
		}		

		this.sampleClasses = scs.toArray(new SampleClass[0]);
		
	}
	
	public ChartGridFactory(Screen screen, SampleClass[] sampleClasses) {
		this(screen);
		this.sampleClasses = sampleClasses;		
		groups = new ArrayList<Group>();
	}
	
	public void makeSeriesCharts(final List<Series> series, final boolean rowsAreCompounds,
			final int highlightDose, final ChartAcceptor acceptor, final Screen screen) {
		seriesService.expectedTimes(series.get(0), 
				new PendingAsyncCallback<String[]>(screen, "Unable to obtain sample times.") {

			@Override
			public void handleSuccess(String[] result) {				
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
		
		cds.getSamples(new SampleMultiFilter(), new TimeDoseColorPolicy(medVals[highlightMed], "SkyBlue"), 
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
