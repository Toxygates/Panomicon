package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.rpc.MatrixService;
import otgviewer.client.rpc.MatrixServiceAsync;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Series;
import otgviewer.shared.TimesDoses;
import otgviewer.shared.ValueType;
import bioweb.shared.SharedUtils;
import bioweb.shared.array.ExpressionRow;
import bioweb.shared.array.ExpressionValue;

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
	
	static class ChartSample {
		String time;
		String dose;
		String compound;
		double value;
		char call;
		Barcode barcode; //may be null
		String probe;
		String color = "grey";
		
		ChartSample(String time, String dose, String compound, 
				double value, Barcode barcode, String probe, char call) {
			this.time = time;
			this.dose = dose;
			this.compound = compound;
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
				r = r * 41 + time.hashCode();
				r = r * 41 + dose.hashCode();
				r = r * 41 + compound.hashCode();	
			}
			return r;
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof ChartSample) {
				if (barcode != null) {
					return (barcode == ((ChartSample) other).barcode);
				} else {
					return (((ChartSample) other).dose == dose &&
						    ((ChartSample) other).time == time && 
							((ChartSample) other).compound == compound);
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
			applyPolicy(policy, samples);
			acceptor.accept(samples);			
		} else {
			//We store these in a set since we may be getting the same samples several times
			Set<ChartSample> r = new HashSet<ChartSample>();
			for (ChartSample s: samples) {
				if (SharedUtils.indexOf(compounds, s.compound) != -1) {
					if (dosesOrTimes == null || SharedUtils.indexOf(dosesOrTimes, s.dose) != -1 || 
							SharedUtils.indexOf(dosesOrTimes, s.time) != -1) {
						r.add(s);			
						s.color = policy.colorFor(s);
					}
				}
			}
			acceptor.accept(new ArrayList<ChartSample>(r));			
		}		 
	}
	
	protected List<ChartSample> samples = new ArrayList<ChartSample>();

	protected String[] _times;
	protected String[] _doses;
	
	String[] times() { return _times; }
	String[] doses() { return _doses; }
	
	protected void init() {
		List<String> times = new ArrayList<String>();
		for (ChartDataSource.ChartSample s: samples) {
			if (!times.contains(s.time)) {
				times.add(s.time);
			}
		}
		_times = times.toArray(new String[0]);
		TimesDoses.sortTimes(_times);		
	
		List<String> doses = new ArrayList<String>();
		for (ChartDataSource.ChartSample s: samples) {
			if (!doses.contains(s.dose)) {
				doses.add(s.dose);
			}
		}
		_doses = doses.toArray(new String[0]);
		TimesDoses.sortDoses(_doses);
	}
	
	static class SeriesSource extends ChartDataSource {
		SeriesSource(List<Series> series, String[] times) {
			for (Series s: series) {				
				for (int i = 0; i < s.values().length; ++i) {
					ExpressionValue ev = s.values()[i];					
					ChartSample cs = new ChartSample(times[i], s.timeDose(), s.compound(), 
							ev.getValue(), null, s.probe(), ev.getCall());
					samples.add(cs);
				}
			}
			init();
		}		
	}
	
	/**
	 * An expression row source with a fixed dataset.
	 * @author johan
	 */
	static class ExpressionRowSource extends ChartDataSource {
		protected Barcode[] barcodes;
		
		ExpressionRowSource(Barcode[] barcodes, List<ExpressionRow> rows) {
			this.barcodes = barcodes;
			addSamplesFromBarcodes(barcodes, rows);
			init();
		}
		
		@Override
		protected void init() {
			List<String> times = new ArrayList<String>();
			for (Barcode b: barcodes) {
				if (!times.contains(b.getTime())) {
					times.add(b.getTime());
				}
			}
			_times = times.toArray(new String[0]);
			TimesDoses.sortTimes(_times);		
		
			List<String> doses = new ArrayList<String>();
			for (Barcode b: barcodes) {
				if (!doses.contains(b.getDose())) {
					doses.add(b.getDose());
				}
			}
			_doses = doses.toArray(new String[0]);
			TimesDoses.sortDoses(_doses);
		}
		
		protected void addSamplesFromBarcodes(Barcode[] barcodes, List<ExpressionRow> rows) {
			for (int i = 0; i < barcodes.length; ++i) {
				Barcode b = barcodes[i];			
				for (ExpressionRow er : rows) {
					ExpressionValue ev = er.getValue(i);
					ChartSample cs = new ChartSample(b.getTime(), b.getDose(), b.getCompound(), 
							ev.getValue(), b, er.getProbe(), ev.getCall());
					cs.barcode = b;
					samples.add(cs);
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
		private static final MatrixServiceAsync kcService = (MatrixServiceAsync) GWT
				.create(MatrixService.class);
		
		private DataFilter filter;
		private String probe;
		private ValueType type;
		private Screen screen;
		
		DynamicExpressionRowSource(DataFilter filter, String probe, ValueType vt, Barcode[] barcodes, Screen screen) {
			super(barcodes, new ArrayList<ExpressionRow>());
			this.filter = filter;
			this.probe = probe;
			this.type = vt;		
			this.screen = screen;
		}
		
		void loadData(final String[] compounds, final String[] dosesOrTimes, 
				final ColorPolicy policy, final SampleAcceptor acceptor) {
			final List<String> useBarcodes = new ArrayList<String>();
			final List<Barcode> useBarcodes_ = new ArrayList<Barcode>();
			for (Barcode b: barcodes) {
				if (
						(compounds == null || SharedUtils.indexOf(compounds, b.getCompound()) != -1) &&
						(dosesOrTimes == null || SharedUtils.indexOf(dosesOrTimes, b.getTime()) != -1 || 
							SharedUtils.indexOf(dosesOrTimes, b.getDose()) != -1) &&
						(type == ValueType.Absolute || ! b.getDose().equals("Control"))
					) {
					useBarcodes.add(b.getCode());
					useBarcodes_.add(b);
				}
			}
			
			samples.clear();
			kcService.getFullData(filter, useBarcodes, 
					new String[] { probe }, type, true, false, 
					new PendingAsyncCallback<List<ExpressionRow>>(screen) {
				@Override
				public void handleFailure(Throwable caught) {
					Window.alert("Unable to obtain chart data.");
				}

				@Override
				public void handleSuccess(final List<ExpressionRow> rows) {
					addSamplesFromBarcodes(useBarcodes_.toArray(new Barcode[0]), rows);	
					getSSamples(compounds, dosesOrTimes, policy, acceptor);
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
