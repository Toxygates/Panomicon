package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.rpc.MatrixService;
import otgviewer.client.rpc.MatrixServiceAsync;
import otgviewer.shared.Group;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Series;
import otgviewer.shared.ValueType;
import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
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
	
	//TODO consider deprecating/simplifying this class
	static class ChartSample {
		final String minor;
		final String medium;
		final String major;
		final double value;
		final char call;
		final @Nullable OTGSample barcode; 
		final String probe;
		String color = "grey";
		
		ChartSample(OTGSample sample, DataSchema schema,
				double value, String probe, char call) {
			this(schema.getMinor(sample), schema.getMedium(sample),
					schema.getMajor(sample),
					value, sample, probe, call);					
		}
		
		ChartSample(String minor, String medium, String major, 
				double value, OTGSample barcode, String probe, char call) {
			this.minor = minor;
			this.medium = medium;
			this.major = major;
			this.value = value;
			this.barcode = barcode;
			this.probe = probe;
			this.call = call;
		}
		
		@Override
		public int hashCode() {
			int r = 0;			
			if (barcode != null) {
				r = barcode.hashCode();
			} else {
				r = r * 41 + minor.hashCode();
				r = r * 41 + medium.hashCode();
				r = r * 41 + major.hashCode();
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
					return (ocs.medium == medium && ocs.minor == minor && 
							ocs.major == major && ocs.value == value);
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
	
	void getSamples(String[] compounds, String[] dosesOrTimes, ColorPolicy policy, SampleAcceptor acceptor) {
		if (compounds == null) {
			applyPolicy(policy, chartSamples);
			acceptor.accept(chartSamples);			
		} else {
			//We store these in a set since we may be getting the same samples several times
			Set<ChartSample> r = new HashSet<ChartSample>();
			for (ChartSample s: chartSamples) {
				if (SharedUtils.indexOf(compounds, s.major) != -1) {
					if (dosesOrTimes == null || SharedUtils.indexOf(dosesOrTimes, s.medium) != -1 || 
							SharedUtils.indexOf(dosesOrTimes, s.minor) != -1) {
						r.add(s);			
						s.color = policy.colorFor(s);
					}
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
			List<String> minorVals = new ArrayList<String>();
			for (ChartDataSource.ChartSample s : chartSamples) {
				if (!minorVals.contains(s.minor)) {
					minorVals.add(s.minor);
				}
			}
			_minorVals = minorVals.toArray(new String[0]);
			// TODO avoid magic constants
			schema.sort(schema.minorParameter(), _minorVals);

			List<String> medVals = new ArrayList<String>();
			for (ChartDataSource.ChartSample s : chartSamples) {
				//TODO generalise control-check better
				if (!schema.isControlValue(s.medium) && !medVals.contains(s.medium)) {
					medVals.add(s.medium);
				}
			}
			_mediumVals = medVals.toArray(new String[0]);
			schema.sort(schema.mediumParameter(), _mediumVals);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to sort chart data", e);
		}
	}
	
	//TODO upgrade to schema
	static class SeriesSource extends ChartDataSource {
		SeriesSource(DataSchema schema, List<Series> series, String[] times) {
			super(schema);
			for (Series s: series) {				
				for (int i = 0; i < s.values().length; ++i) {
					ExpressionValue ev = s.values()[i];					
					ChartSample cs = new ChartSample(times[i], s.timeDose(), s.compound(), 
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
			List<String> times = new ArrayList<String>();
			for (OTGSample b: samples) {
				if (!times.contains(b.get(minorParam))) {
					times.add(b.get(minorParam));
				}
			}
			try {
				//TODO code duplication with above
				_minorVals = times.toArray(new String[0]);
				schema.sort(schema.minorParameter(), _minorVals);

				List<String> doses = new ArrayList<String>();
				for (OTGSample b : samples) {
					logger.info("Inspect " + b + " for " + medParam);
					if (!doses.contains(b.get(medParam))) {
						doses.add(b.get(medParam));						
					}
				}
				_mediumVals = doses.toArray(new String[0]);				
				schema.sort(schema.mediumParameter(), _mediumVals);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Unable to sort chart data", e);
			}
		}
		
		protected void addSamplesFromBarcodes(OTGSample[] samples, List<ExpressionRow> rows) {
			logger.info("Add samples from " + samples.length + " samples and " + rows.size() + " rows");
			for (int i = 0; i < samples.length; ++i) {
				OTGSample b = samples[i];				
//				logger.info("Consider " + b.toString());
				for (ExpressionRow er : rows) {
					ExpressionValue ev = er.getValue(i);
					ChartSample cs = new ChartSample(b, schema,
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
		private static final MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
				.create(MatrixService.class);
		
		private String probe;
		private ValueType type;
		private Screen screen;
		
		DynamicExpressionRowSource(DataSchema schema, String probe, 
				ValueType vt, OTGSample[] barcodes, Screen screen) {
			super(schema, barcodes, new ArrayList<ExpressionRow>());			
			this.probe = probe;
			this.type = vt;		
			this.screen = screen;
		}
		
		void loadData(final String[] majors, final String[] medsOrMins, 
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
			matrixService.getFullData(g, 
					new String[] { probe }, true, false, type,  
					new PendingAsyncCallback<List<ExpressionRow>>(screen) {
				@Override
				public void handleFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void handleSuccess(final List<ExpressionRow> rows) {
					addSamplesFromBarcodes(useSamples.toArray(new OTGSample[0]), rows);	
					getSSamples(majors, medsOrMins, policy, acceptor);
				}					
			});
			
		}

		@Override
		void getSamples(String[] compounds, String[] dosesOrTimes, ColorPolicy policy, SampleAcceptor acceptor) {
			loadData(compounds, dosesOrTimes, policy, acceptor);			
		}
		
		void getSSamples(String[] compounds, String[] dosesOrTimes, ColorPolicy policy, SampleAcceptor acceptor) {
			super.getSamples(compounds, dosesOrTimes, policy, acceptor);
		}		
	}	
}
