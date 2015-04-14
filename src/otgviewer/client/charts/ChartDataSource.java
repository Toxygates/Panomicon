package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.shared.FullMatrix;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Series;
import otgviewer.shared.ValueType;
import t.common.client.rpc.MatrixService;
import t.common.client.rpc.MatrixServiceAsync;
import t.common.shared.DataSchema;
import t.common.shared.HasClass;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.Unit;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.ExpressionValue;

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
	
	private static Logger logger = Utils.getLogger("chartdata");
	
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
		
		@Deprecated
		public String major() { return schema.getMajor(this); }
		
		@Deprecated
		public String medium() { return schema.getMedium(this); }
		
		@Deprecated
		public String minor() { return schema.getMinor(this); }
		
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
	
	private void applyPolicy(ColorPolicy policy, List<ChartSample> samples) {
		for (ChartSample s: samples) {
			s.color = policy.colorFor(s);
		}
	}
	
	void getSamples(String[] compounds, String[] dosesOrTimes, String[] organisms,
			ColorPolicy policy, SampleAcceptor acceptor) {
		if (compounds == null) {
			applyPolicy(policy, chartSamples);
			acceptor.accept(chartSamples);			
		} else {
			//We store these in a set since we may be getting the same samples several times
			Set<ChartSample> r = new HashSet<ChartSample>();
			for (ChartSample s: chartSamples) {
				if (SharedUtils.indexOf(compounds, schema.getMajor(s)) != -1 &&
						(organisms == null || 
							SharedUtils.indexOf(organisms, s.sampleClass().get("organism")) != 1) &&
					(dosesOrTimes == null || SharedUtils.indexOf(dosesOrTimes, schema.getMedium(s)) != -1 || 
							SharedUtils.indexOf(dosesOrTimes, schema.getMinor(s)) != -1)) {
						r.add(s);			
						s.color = policy.colorFor(s);					
				}
			}
			acceptor.accept(new ArrayList<ChartSample>(r));			
		}		 
	}
	
	protected List<ChartSample> chartSamples = new ArrayList<ChartSample>();

	protected String[] _minorVals;
	protected String[] _mediumVals;
	
	String[] minorVals() { return _minorVals; }
	String[] mediumVals() { return _mediumVals; }
	
	protected DataSchema schema;
	final protected String minorParam, medParam, majorParam;
	
	ChartDataSource(DataSchema schema) {
		this.schema = schema;		
		minorParam = schema.minorParameter();
		medParam = schema.mediumParameter();
		majorParam = schema.majorParameter();
	}
	
	protected void init() {
		try {
			Set<String> minorVals = SampleClass.collectInner(chartSamples, 
					schema.minorParameter());
			
			_minorVals = minorVals.toArray(new String[0]);
			// TODO avoid magic constants
			schema.sort(schema.minorParameter(), _minorVals);

			
			List<String> medVals = new ArrayList<String>();
			for (ChartDataSource.ChartSample s : chartSamples) {
				//TODO generalise control-check better
				if (!schema.isControlValue(schema.getMedium(s)) && 
						!medVals.contains(schema.getMedium(s))) {
					medVals.add(schema.getMedium(s));
				}
			}
			_mediumVals = medVals.toArray(new String[0]);
			schema.sort(schema.mediumParameter(), _mediumVals);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to sort chart data", e);
		}
	}
	
	static class SeriesSource extends ChartDataSource {
		SeriesSource(DataSchema schema, List<Series> series, String[] times) {
			super(schema);
			for (Series s: series) {				
				for (int i = 0; i < s.values().length; ++i) {
					ExpressionValue ev = s.values()[i];					
					ChartSample cs = new ChartSample(s.sampleClass(), schema,
							ev.getValue(), null, s.probe(), ev.getCall());
					chartSamples.add(cs);
				}
			}
			init();
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
			init();
		}
		
		@Override
		protected void init() {
			final List<OTGSample> sampleList = Arrays.asList(samples);
			Set<String> times = SampleClass.collectInner(sampleList, minorParam);
			
			try {
				//TODO code duplication with above
				_minorVals = times.toArray(new String[0]);
				schema.sort(schema.minorParameter(), _minorVals);
				Set<String> doses = SampleClass.collectInner(sampleList, medParam);
				
				_mediumVals = doses.toArray(new String[0]);				
				schema.sort(schema.mediumParameter(), _mediumVals);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Unable to sort chart data", e);
			}
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
		
		void loadData(final String[] majors, final String[] medsOrMins, final String[] organisms,
				final ColorPolicy policy, final SampleAcceptor acceptor) {
			logger.info("Dynamic source: load for " + majors.length + " majors");
			
			final List<OTGSample> useSamples = new ArrayList<OTGSample>();
			for (OTGSample b: samples) {
				if (
						(majors == null || SharedUtils.indexOf(majors, b.get(majorParam)) != -1) &&
						(medsOrMins == null || SharedUtils.indexOf(medsOrMins, b.get(minorParam)) != -1 || 					
						SharedUtils.indexOf(medsOrMins, b.get(medParam)) != -1) // &&
						//!schema.isControlValue(b.get(medParam))
						//TODO generalise the control-check better
					) {
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
					new PendingAsyncCallback<FullMatrix>(screen) {
				@Override
				public void handleFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void handleSuccess(final FullMatrix mat) {
					addSamplesFromBarcodes(useSamples.toArray(new OTGSample[0]), mat.rows());	
					getSSamples(majors, medsOrMins, organisms, policy, acceptor);
				}					
			});
			
		}

		@Override
		void getSamples(String[] compounds, String[] dosesOrTimes, String[] organisms,
				ColorPolicy policy, SampleAcceptor acceptor) {
			loadData(compounds, dosesOrTimes, organisms, policy, acceptor);			
		}
		
		void getSSamples(String[] compounds, String[] dosesOrTimes, String[] organisms,
				ColorPolicy policy, SampleAcceptor acceptor) {
			super.getSamples(compounds, dosesOrTimes, organisms, policy, acceptor);
		}		
	}	
	
	/**
	 * A dynamic source that makes requests based on a list of units.
	 * @author johan
	 *
	 */
	static class DynamicUnitSource extends DynamicExpressionRowSource {
		static OTGSample[] samplesFor(Unit[] units) {
			List<OTGSample> samples = new ArrayList<OTGSample>();
			for (Unit u: units) {
				samples.addAll(Arrays.asList(u.getSamples()));
			}
			
			return samples.toArray(new OTGSample[0]);
		}
		
		private Unit[] units;
		
		DynamicUnitSource(DataSchema schema, String[] probes, 
				ValueType vt, Unit[] units, Screen screen) {
			super(schema, probes, vt, samplesFor(units), screen);
			this.units = units;
		}
		
		@Override
		void loadData(final String[] majors, final String[] medsOrMins, final String[] organisms,
				final ColorPolicy policy, final SampleAcceptor acceptor) {
			logger.info("Dynamic unit source: load for " + majors.length + " majors");
			
			final List<Group> groups = new ArrayList<Group>();
			final List<Unit> useUnits = new ArrayList<Unit>();
			int i = 0;
			for (Unit u: units) {
				if (
						(majors == null || SharedUtils.indexOf(majors, u.get(majorParam)) != -1) &&
						(medsOrMins == null || SharedUtils.indexOf(medsOrMins, u.get(minorParam)) != -1 || 					
						SharedUtils.indexOf(medsOrMins, u.get(medParam)) != -1) // &&
//						!schema.isControlValue(u.get(medParam))
						//TODO generalise the control-check better
					) {
					Group g = new Group(schema, "g" + i, u.getSamples());
					i++;
					groups.add(g);
					useUnits.add(u);
				}				
			}
			
			chartSamples.clear();						
			matrixService.getFullData(groups, 
					probes, true, false, type,  
					new PendingAsyncCallback<FullMatrix>(screen) {
				@Override
				public void handleFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void handleSuccess(final FullMatrix mat) {
					addSamplesFromUnits(useUnits, mat.rows());	
					getSSamples(majors, medsOrMins, organisms, policy, acceptor);
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
