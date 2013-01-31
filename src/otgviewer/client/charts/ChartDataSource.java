package otgviewer.client.charts;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ExpressionValue;
import otgviewer.shared.Series;

/**
 * This class brings series and row data into a unified interface for the purposes of
 * chart drawing.
 * @author johan
 *
 */
abstract class ChartDataSource {
	
	static class ChartSample {
		String time;
		String dose;
		String compound;
		double value;
		Barcode barcode; //may be null
		String probe; 
		
		ChartSample(String time, String dose, String compound, 
				double value, Barcode barcode, String probe) {
			this.time = time;
			this.dose = dose;
			this.compound = compound;
			this.value = value;
			this.barcode = barcode;
			this.probe = probe;
		}
	}
	
	List<ChartSample> getSamples() { return samples; }
	protected List<ChartSample> samples = new ArrayList<ChartSample>();
	
	static class SeriesSource extends ChartDataSource {
		SeriesSource(List<Series> series, String[] times) {
			for (Series s: series) {				
				for (int i = 0; i < s.values().length; ++i) {
					ExpressionValue ev = s.values()[i];					
					ChartSample cs = new ChartSample(times[i], s.timeDose(), s.compound(), ev.getValue(), null, s.probe());
					samples.add(cs);
				}
			}
		}
	}
	
	static class ExpressionRowSource extends ChartDataSource {
		ExpressionRowSource(Barcode[] barcodes, List<ExpressionRow> rows) {
			for (int i = 0; i < barcodes.length; ++i) {
				Barcode b = barcodes[i];			
				for (ExpressionRow er : rows) {
					ChartSample cs = new ChartSample(b.getTime(), b.getDose(), b.getCompound(), 
							er.getValue(i).getValue(), b, er.getProbe());
					samples.add(cs);
				}
			}
		}
	}
}
