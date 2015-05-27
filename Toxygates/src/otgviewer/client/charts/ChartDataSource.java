package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.FullMatrix;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Series;
import t.common.shared.DataSchema;
import t.common.shared.HasClass;
import t.common.shared.SampleClass;
import t.common.shared.SampleMultiFilter;
import t.common.shared.SharedUtils;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.ExpressionValue;
import t.viewer.client.rpc.MatrixService;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.shared.Unit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

/**
 * This class brings series and row data into a unified interface for the purposes of
 * chart drawing.
 */
abstract class ChartDataSource {
	
	interface SampleAcceptor {
		void accept(List<ChartSample> samples);
	}
	
	private static Logger logger = SharedUtils.getLogger("chartdata");
	
	//TODO consider deprecating/simplifying/replacing this class
	
	static class ChartSample implements HasClass {
		final SampleClass sc;
		final DataSchema schema;
		
		final double value;
		final char call;
		final @Nullable OTGSample barcode; 
		final String probe;
		String color = "grey";
		
		private final static List<String> chartKeys = new ArrayList<String>();
		static {						
			//TODO use DataSchema to add these
			Collections.addAll(chartKeys, "exposure_time", "dose_level", 
					"compound_name", "organism");
		}
		
		ChartSample(SampleClass sc, DataSchema schema,
				double value, OTGSample barcode, String probe, char call) {						
			this.sc = sc.copyOnly(chartKeys);
			this.schema = schema;
			this.value = value;
			this.barcode = barcode;
			this.probe = probe;
			this.call = call;						 
		}

		ChartSample(OTGSample sample, DataSchema schema,
				double value, String probe, char call) {
			this(sample.sampleClass(), schema, value, sample, probe, call);					
		}
		
		ChartSample(Unit u, DataSchema schema, double value, String probe, char call) {
			this(u, schema, value, u.getSamples()[0], //representative sample only
					probe, call);			
		}
		
		public DataSchema schema() { return schema; }
		
		public SampleClass sampleClass() { return sc; }
		
		//TODO do we need hashCode and equals for this class?
		//See getSamples below where we use a Set<ChartSample>
		@Override
		public int hashCode() {
			int r = 0;			
			if (barcode != null) {
				r = barcode.hashCode();
			} else {
				r = r * 41 + sc.hashCode();				
				r = r * 41 + ((Double) value).hashCode();
			}
			return r;
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof ChartSample) {
				if (barcode != null) {
					return (barcode == ((ChartSample) other).barcode);
				} else {
					ChartSample ocs = (ChartSample) other;
					return sc.equals(((ChartSample) other).sampleClass()) &&
							ocs.value == value && ocs.call == call;
				}
			} else {
				return false;
			}
		}
	}
	
	protected List<ChartSample> chartSamples = new ArrayList<ChartSample>();

	protected String[] _minorVals;
	protected String[] _mediumVals;
	
	String[] minorVals() { return _minorVals; }
	String[] mediumVals() { return _mediumVals; }
	
	protected DataSchema schema;
	protected boolean controlMedVals = false;
	
	ChartDataSource(DataSchema schema) {
		this.schema = schema;				
	}
	
	protected void initParams(boolean controlMedVals) {
		try {
			String minorParam = schema.minorParameter();
			String medParam = schema.mediumParameter();
			Set<String> minorVals = SampleClass.collectInner(chartSamples, 
					minorParam);			
			_minorVals = minorVals.toArray(new String[0]);
			schema.sort(minorParam, _minorVals);
			
			Set<String> medVals = new HashSet<String>();
			for (ChartDataSource.ChartSample s : chartSamples) {
				//TODO generalise control-check better
				if (controlMedVals || !schema.isControlValue(schema.getMedium(s))) {
					medVals.add(schema.getMedium(s));
				}
			}
			_mediumVals = medVals.toArray(new String[0]);
			schema.sort(medParam, _mediumVals);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to sort chart data", e);
		}
	}

	private void applyPolicy(ColorPolicy policy, List<ChartSample> samples) {
		for (ChartSample s: samples) {
			s.color = policy.colorFor(s);
		}
	}
	
	/**
	 * Obtain samples, making an asynchronous call if necessary, and pass them on to the
	 * sample acceptor when they are available.
	 */
	void getSamples(SampleMultiFilter smf, ColorPolicy policy, SampleAcceptor acceptor) {
		if (!smf.contains(schema.majorParameter())) {
			//TODO why is this needed?
			applyPolicy(policy, chartSamples);
			acceptor.accept(chartSamples);			
		} else {
			//We store these in a set since we may be getting the same samples several times
			Set<ChartSample> r = new HashSet<ChartSample>();
			for (ChartSample s: chartSamples) {
				if (smf.accepts(s)) {				
					r.add(s);			
					s.color = policy.colorFor(s);					
				}
			}
			acceptor.accept(new ArrayList<ChartSample>(r));			
		}		 
	}
	
	static class SeriesSource extends ChartDataSource {
		SeriesSource(DataSchema schema, List<Series> series, String[] times) {
			super(schema);
			for (Series s: series) {
				for (int i = 0; i < s.values().length; ++i) {
					ExpressionValue ev = s.values()[i];			
					String time = times[i];
					SampleClass sc = s.sampleClass().
							copyWith(schema.timeParameter(), time);
					ChartSample cs = new ChartSample(sc, schema,
							ev.getValue(), null, s.probe(), ev.getCall());
					chartSamples.add(cs);
				}
			}
			initParams(false);
		}		
	}
	
	/**
	 * An expression row source with a fixed dataset.
	 */
	static class ExpressionRowSource extends ChartDataSource {
		protected OTGSample[] samples;
		
		ExpressionRowSource(DataSchema schema, OTGSample[] samples, List<ExpressionRow> rows) {
			super(schema);
			this.samples = samples;
			logger.info("ER source: " + samples.length + " samples");
			
			addSamplesFromBarcodes(samples, rows);
			initParams(true);
		}
		
		protected void addSamplesFromBarcodes(OTGSample[] samples, List<ExpressionRow> rows) {
			logger.info("Add samples from " + samples.length + " samples and " + rows.size() + " rows");
			for (int i = 0; i < samples.length; ++i) {
				for (ExpressionRow er : rows) {
					ExpressionValue ev = er.getValue(i);
					ChartSample cs = new ChartSample(samples[i], schema,
							ev.getValue(), er.getProbe(), ev.getCall());
					chartSamples.add(cs);
				}
			}		
		}
	}
	
	/**
	 * An expression row source that dynamically loads data.
	 * @author johan
	 *
	 */
	static class DynamicExpressionRowSource extends ExpressionRowSource {
		protected static final MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
				.create(MatrixService.class);
		
		protected String[] probes;
		protected ValueType type;
		protected Screen screen;
		
		DynamicExpressionRowSource(DataSchema schema, String[] probes, 
				ValueType vt, OTGSample[] barcodes, Screen screen) {
			super(schema, barcodes, new ArrayList<ExpressionRow>());			
			this.probes = probes;
			this.type = vt;		
			this.screen = screen;
		}
		
		void loadData(final SampleMultiFilter smf, final ColorPolicy policy, final SampleAcceptor acceptor) {
			logger.info("Dynamic source: load for " + smf);
			
			final List<OTGSample> useSamples = new ArrayList<OTGSample>();
			for (OTGSample b: samples) {
				if (smf.accepts(b)) {				
					useSamples.add(b);
				}
			}
			
			chartSamples.clear();
			Group g = new Group(schema, "temporary", 
					useSamples.toArray(new OTGSample[0]));
			List<Group> gs = new ArrayList<Group>();
			gs.add(g);
			matrixService.getFullData(gs, 
					probes, true, false, type,  
					new PendingAsyncCallback<FullMatrix>(screen, "Unable to obtain chart data.") {

				@Override
				public void handleSuccess(final FullMatrix mat) {
					addSamplesFromBarcodes(useSamples.toArray(new OTGSample[0]), mat.rows());	
					getSSamples(smf, policy, acceptor);
				}					
			});
			
		}

		@Override
		void getSamples(SampleMultiFilter smf, ColorPolicy policy, SampleAcceptor acceptor) {
			loadData(smf, policy, acceptor);			
		}
		
		void getSSamples(SampleMultiFilter smf, ColorPolicy policy, SampleAcceptor acceptor) {
			super.getSamples(smf, policy, acceptor);
		}		
	}	
	
	/**
	 * A dynamic source that makes requests based on a list of units.
	 * @author johan
	 *
	 */
	static class DynamicUnitSource extends DynamicExpressionRowSource {				
		private Unit[] units;
		
		DynamicUnitSource(DataSchema schema, String[] probes, 
				ValueType vt, Unit[] units, Screen screen) {
			super(schema, probes, vt, Unit.collectBarcodes(units), screen);
			this.units = units;
		}
		
		@Override
		void loadData(final SampleMultiFilter smf, final ColorPolicy policy, final SampleAcceptor acceptor) {
			logger.info("Dynamic unit source: load for " + smf);
			
			final List<Group> groups = new ArrayList<Group>();
			final List<Unit> useUnits = new ArrayList<Unit>();
			int i = 0;
			for (Unit u: units) {
				if (smf.accepts(u)) {				
					Group g = new Group(schema, "g" + i, u.getSamples());
					i++;
					groups.add(g);
					useUnits.add(u);
				}				
			}
			
			chartSamples.clear();						
			matrixService.getFullData(groups, 
					probes, true, false, type,  
					new PendingAsyncCallback<FullMatrix>(screen, "Unable to obtain chart data") {				

				@Override
				public void handleSuccess(final FullMatrix mat) {
					addSamplesFromUnits(useUnits, mat.rows());	
					getSSamples(smf, policy, acceptor);
				}					
			});			
		}
		
		protected void addSamplesFromUnits(List<Unit> units, List<ExpressionRow> rows) {
			logger.info("Add samples from " + units.size() + " units and " + rows.size() + " rows");
			for (int i = 0; i < units.size(); ++i) {		
				for (ExpressionRow er : rows) {
					ExpressionValue ev = er.getValue(i);
					ChartSample cs = new ChartSample(units.get(i), schema,
							ev.getValue(), er.getProbe(), ev.getCall());
					chartSamples.add(cs);
				}
			}		
		}
	}
		
}
